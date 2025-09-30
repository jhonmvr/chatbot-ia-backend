
package com.relative.chat.bot.ia.application.usecases;
import com.relative.chat.bot.ia.application.ports.out.VectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service @RequiredArgsConstructor
public class MigrateVectors {
    private final VectorStore source;
    private final VectorStore dest;
    public long handle(String nsSource, String nsDest, int dim, int batchSize) {
        dest.ensureNamespace(nsDest, dim);
        long total = 0;
        for (var batch : (Iterable<List<VectorStore.VectorRecord>>) () -> source.streamAll(nsSource, batchSize).iterator()) {
            dest.upsert(nsDest, batch);
            total += batch.size();
        }
        return total;
    }
}
