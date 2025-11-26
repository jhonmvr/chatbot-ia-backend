package com.relative.chat.bot.ia.infrastructure.adapters.out.vector;

import com.relative.chat.bot.ia.application.ports.out.VectorStore;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.*;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

/**
 * Implementación de VectorStore usando pgvector de PostgreSQL
 * Persiste embeddings y metadatos en la base de datos
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.vector-store.provider", havingValue = "pgvector", matchIfMissing = false)
public class PgVectorStoreAdapter implements VectorStore {
    
    private final KbEmbeddingPgvectorJpa embeddingRepository;
    private final KbChunkJpa chunkRepository;
    private final KbVectorRefJpa vectorRefRepository;
    
    @Override
    public void ensureNamespace(String ns, int dim) {
        log.info("Namespace '{}' configurado para dimensión {} con pgvector", ns, dim);
        // En pgvector, no necesitamos crear namespaces explícitamente
        // Los datos se organizan por kb_id
    }
    
    @Override
    @Transactional
    public void upsert(String ns, List<VectorRecord> records) {
        log.info("Insertando {} vectores en namespace '{}' usando pgvector", records.size(), ns);
        
        for (VectorRecord record : records) {
            try {
                UUID chunkId = UUID.fromString(record.id());
                
                // 1. Verificar que el chunk existe
                if (!chunkRepository.existsById(chunkId)) {
                    log.warn("Chunk {} no encontrado, saltando embedding", chunkId);
                    continue;
                }
                
                // 2. Convertir float[] a formato pgvector string: "[0.1, 0.2, 0.3]"
                String embeddingStr = vectorToString(record.vector());
                
                // 3. Upsert del embedding usando query nativa (INSERT ... ON CONFLICT)
                UUID clientId = extractClientId(record.payload());
                UUID kbId = extractKbId(record.payload());
                embeddingRepository.upsertEmbedding(chunkId, embeddingStr, clientId, kbId);
                
                // 4. Upsert de la referencia vectorial
                vectorRefRepository.upsertVectorRef(
                    chunkId, 
                    "pgvector", 
                    "kb_embedding_pgvector", 
                    chunkId.toString()
                );
                
                log.debug("Embedding persistido para chunk {}", chunkId);
                
            } catch (Exception e) {
                log.error("Error al persistir vector {}: {}", record.id(), e.getMessage(), e);
                throw new RuntimeException("Error al guardar embedding: " + e.getMessage(), e);
            }
        }
        
        log.info("✅ {} embeddings guardados exitosamente en pgvector", records.size());
    }
    
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, noRollbackFor = {SQLGrammarException.class})
    public List<QueryResult> query(String ns, float[] vector, int topK, Map<String, Object> filter) {
        log.info("Búsqueda en namespace '{}' con topK={}", ns, topK);
        
        try {
            // Convertir vector a string format pgvector
            String embeddingStr = vectorToString(vector);
            
            // Extraer kb_id del namespace (formato: "kb_<uuid>")
            UUID kbIdFromNamespace = extractKbIdFromNamespace(ns);
            
            // Ejecutar búsqueda por similitud
            List<Object[]> results;
            
            // Prioridad: 1) namespace, 2) filtro kb_id, 3) filtro client_id, 4) sin filtro
            if (kbIdFromNamespace != null) {
                log.info("Filtrando por KB ID del namespace: {}", kbIdFromNamespace);
                results = embeddingRepository.findNearestNeighborsByKb(embeddingStr, kbIdFromNamespace, topK);
            } else if (filter != null && filter.containsKey("kb_id")) {
                UUID kbId = UUID.fromString(filter.get("kb_id").toString());
                log.info("Filtrando por KB ID del filtro: {}", kbId);
                results = embeddingRepository.findNearestNeighborsByKb(embeddingStr, kbId, topK);
            } else if (filter != null && filter.containsKey("client_id")) {
                UUID clientId = UUID.fromString(filter.get("client_id").toString());
                log.info("Filtrando por Client ID: {}", clientId);
                results = embeddingRepository.findNearestNeighborsByClient(embeddingStr, clientId, topK);
            } else {
                log.warn("Búsqueda sin filtros - buscando en todos los embeddings");
                results = embeddingRepository.findNearestNeighbors(embeddingStr, topK);
            }
            
            // Convertir resultados a QueryResult
            List<QueryResult> queryResults = new ArrayList<>();
            
            for (Object[] row : results) {
                UUID chunkId = (UUID) row[0];
                double distance = ((Number) row[1]).doubleValue();
                double score = 1.0 - distance; // Convertir distancia a score
                
                // Cargar chunk para obtener contenido y metadata
                Optional<KbChunkEntity> chunkOpt = chunkRepository.findById(chunkId);
                if (chunkOpt.isEmpty()) {
                    continue;
                }
                
                KbChunkEntity chunk = chunkOpt.get();
                
                Map<String, Object> payload = new HashMap<>(chunk.getMetadata() != null ? chunk.getMetadata() : Map.of());
                payload.put("text", chunk.getContent());
                payload.put("chunk_id", chunkId.toString());
                payload.put("chunk_index", chunk.getChunkIndex());
                payload.put("document_id", chunk.getDocument().getId().toString());
                
                queryResults.add(new QueryResult(chunkId.toString(), score, payload));
            }
            
            log.info("Encontrados {} resultados", queryResults.size());
            return queryResults;
            
        } catch (DataAccessException e) {
            // Capturar errores específicos de pgvector
            Throwable rootCause = e.getRootCause();
            String errorMessage = rootCause != null ? rootCause.getMessage() : e.getMessage();
            
            // Verificar si es un error relacionado con el tipo vector
            if (errorMessage != null && errorMessage.contains("type \"vector\" does not exist")) {
                log.error("❌ Error crítico: La extensión pgvector no está disponible en esta conexión. " +
                        "Esto puede ocurrir cuando se procesan múltiples mensajes en paralelo. " +
                        "Mensaje: {}", errorMessage);
                log.error("Stack trace completo:", e);
                // Retornar lista vacía en lugar de lanzar excepción para no marcar la transacción como rollback-only
                return List.of();
            }
            
            // Verificar si es un error de transacción abortada
            if (errorMessage != null && errorMessage.contains("current transaction is aborted")) {
                log.error("❌ Error de transacción abortada. Esto puede ocurrir cuando una consulta anterior falló. " +
                        "Mensaje: {}", errorMessage);
                log.error("Stack trace completo:", e);
                return List.of();
            }
            
            log.error("Error en búsqueda de vectores: {}", e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Error inesperado en búsqueda: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    @Transactional
    public void delete(String ns, List<String> ids) {
        log.info("Eliminando {} documentos del namespace '{}'", ids.size(), ns);
        
        for (String id : ids) {
            try {
                UUID chunkId = UUID.fromString(id);
                
                // Las eliminaciones en cascada se encargarán de los embeddings y referencias
                chunkRepository.deleteById(chunkId);
                
            } catch (Exception e) {
                log.error("Error al eliminar chunk {}: {}", id, e.getMessage());
            }
        }
    }
    
    @Override
    public Stream<List<VectorRecord>> streamAll(String ns, int batchSize) {
        log.warn("streamAll aún no implementado para pgvector");
        return Stream.empty();
    }
    
    /**
     * Convierte un array de floats a formato string de pgvector
     * Ejemplo: [0.1, 0.2, 0.3] -> "[0.1,0.2,0.3]"
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Extrae el clientId del payload de metadata
     */
    private UUID extractClientId(Map<String, Object> payload) {
        if (payload != null && payload.containsKey("client_id")) {
            Object clientId = payload.get("client_id");
            if (clientId instanceof UUID) {
                return (UUID) clientId;
            } else if (clientId instanceof String) {
                return UUID.fromString((String) clientId);
            }
        }
        return null;
    }
    
    /**
     * Extrae el kbId del payload de metadata
     */
    private UUID extractKbId(Map<String, Object> payload) {
        if (payload != null && payload.containsKey("kb_id")) {
            Object kbId = payload.get("kb_id");
            if (kbId instanceof UUID) {
                return (UUID) kbId;
            } else if (kbId instanceof String) {
                return UUID.fromString((String) kbId);
            }
        }
        return null;
    }
    
    /**
     * Extrae el UUID del KB desde el namespace
     * Ejemplo: "kb_123e4567-e89b-12d3-a456-426614174000" -> UUID
     */
    private UUID extractKbIdFromNamespace(String namespace) {
        if (namespace == null || !namespace.startsWith("kb_")) {
            log.warn("Namespace no tiene formato válido 'kb_<uuid>': {}", namespace);
            return null;
        }
        
        try {
            String uuidPart = namespace.substring(3); // Remover "kb_"
            UUID kbId = UUID.fromString(uuidPart);
            log.debug("KB ID extraído del namespace: {}", kbId);
            return kbId;
        } catch (IllegalArgumentException e) {
            log.error("Error al extraer UUID del namespace '{}': {}", namespace, e.getMessage());
            return null;
        }
    }
}

