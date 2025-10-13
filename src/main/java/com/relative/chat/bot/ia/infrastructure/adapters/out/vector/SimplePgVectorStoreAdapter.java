package com.relative.chat.bot.ia.infrastructure.adapters.out.vector;

import com.relative.chat.bot.ia.application.ports.out.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Implementación MOCK de VectorStore
 * Para desarrollo y testing sin persistencia real
 * 
 * Se activa cuando: app.vector-store.provider = mock
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.vector-store.provider", havingValue = "mock", matchIfMissing = false)
public class SimplePgVectorStoreAdapter implements VectorStore {
    
    @Override
    public void ensureNamespace(String ns, int dim) {
        log.info("Namespace '{}' con dimensión {} está listo", ns, dim);
    }
    
    @Override
    public void upsert(String ns, List<VectorRecord> records) {
        log.info("Mock: Insertando {} documentos en namespace '{}'", records.size(), ns);
        // TODO: Implementar inserción real en pgvector
    }
    
    @Override
    public List<QueryResult> query(String ns, float[] vector, int topK, Map<String, Object> filter) {
        log.info("Mock: Búsqueda en namespace '{}' con topK={}", ns, topK);
        // TODO: Implementar búsqueda real en pgvector
        return List.of();
    }
    
    @Override
    public void delete(String ns, List<String> ids) {
        log.info("Mock: Eliminando {} documentos del namespace '{}'", ids.size(), ns);
        // TODO: Implementar eliminación real
    }
    
    @Override
    public Stream<List<VectorRecord>> streamAll(String ns, int batchSize) {
        log.warn("streamAll no implementado");
        return Stream.empty();
    }
}

