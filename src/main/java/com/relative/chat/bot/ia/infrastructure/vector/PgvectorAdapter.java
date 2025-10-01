
package com.relative.chat.bot.ia.infrastructure.vector;
import com.relative.chat.bot.ia.application.ports.out.VectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
@RequiredArgsConstructor
@Repository
public class PgvectorAdapter implements VectorStore {
    private final JdbcTemplate jdbc;
    private final String table = "embeddings";
    private final String vectorCol = "vector";
    @Override public void ensureNamespace(String ns, int dim) {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " (id TEXT PRIMARY KEY, " + vectorCol + " vector(" + dim + ") NOT NULL, payload JSONB)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS " + table + "_hnsw ON " + table + " USING hnsw (" + vectorCol + ")");
    }
    @Override public void upsert(String ns, List<VectorRecord> records) {
        if (records.isEmpty()) return;
        String sql = "INSERT INTO " + table + "(id," + vectorCol + ",payload) VALUES (?,?,?::jsonb) " +
                "ON CONFLICT(id) DO UPDATE SET " + vectorCol + "=EXCLUDED." + vectorCol + ", payload=EXCLUDED.payload";
        jdbc.batchUpdate(sql, records, 500, (ps, r) -> {
            ps.setString(1, r.id());
            ps.setString(2, toPgVector(r.vector()));
            ps.setString(3, JsonUtil.toJson(mergeNs(r.payload(), ns)));
        });
    }
    @Override public List<QueryResult> query(String ns, float[] vector, int topK, Map<String, Object> filter) {
        String sql = "SELECT id, 1 - ("+vectorCol+" <=> ?::vector) AS score, payload " +
                     "FROM " + table + " WHERE payload->>'namespace' = ? " +
                     "ORDER BY " + vectorCol + " <-> ?::vector LIMIT ?";
        return jdbc.query(sql, rs -> {
            List<QueryResult> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new QueryResult(rs.getString("id"), rs.getDouble("score"),
                        JsonUtil.fromJson(rs.getString("payload"))));
            }
            return out;
        }, toPgVector(vector), ns, toPgVector(vector), topK);
    }
    @Override public void delete(String ns, List<String> ids) {
        if (ids.isEmpty()) return;
        String sql = "DELETE FROM " + table + " WHERE id = ANY(?) AND payload->>'namespace' = ?";
        jdbc.update(sql, ids.toArray(new String[0]), ns);
    }
    @Override public Stream<List<VectorRecord>> streamAll(String ns, int batchSize) {
        throw new UnsupportedOperationException("streamAll not implemented yet");
    }
    private static String toPgVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<v.length;i++) { if (i>0) sb.append(","); sb.append(Float.toString(v[i])); }
        sb.append("]"); return sb.toString();
    }
    private static Map<String,Object> mergeNs(Map<String,Object> payload, String ns) {
        var out = new java.util.LinkedHashMap<String,Object>();
        if (payload != null) out.putAll(payload);
        out.put("namespace", ns); return out;
    }
    static class JsonUtil {
        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();
        static String toJson(Object o) { try { return MAPPER.writeValueAsString(o); } catch (Exception e) { throw new RuntimeException(e); } }
        @SuppressWarnings("unchecked")
        static Map<String,Object> fromJson(String s) { if (s == null) return null; try { return MAPPER.readValue(s, java.util.Map.class); } catch (Exception e) { throw new RuntimeException(e); } }
    }
}
