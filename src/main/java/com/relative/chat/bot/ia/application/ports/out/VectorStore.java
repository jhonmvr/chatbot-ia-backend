
package com.relative.chat.bot.ia.application.ports.out;

import java.util.List;

import java.util.Map;

import java.util.stream.Stream;

public interface VectorStore {
    void ensureNamespace(String ns, int dim);

    void upsert(String ns, List<VectorRecord> records);

    List<QueryResult> query(String ns, float[] vector, int topK, Map<String, Object> filter);

    void delete(String ns, List<String> ids);

    Stream<List<VectorRecord>> streamAll(String ns, int batchSize);

    record VectorRecord(String id, float[] vector, Map<String,Object> payload) {}
    record QueryResult(String id, double score, Map<String,Object> payload) {}
}
