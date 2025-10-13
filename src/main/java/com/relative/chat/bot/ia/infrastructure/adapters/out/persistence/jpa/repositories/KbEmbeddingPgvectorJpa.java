package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.KbEmbeddingPgvectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para embeddings almacenados en pgvector
 */
public interface KbEmbeddingPgvectorJpa extends JpaRepository<KbEmbeddingPgvectorEntity, UUID> {
    
    /**
     * Inserta o actualiza un embedding usando UPSERT de PostgreSQL
     * Esto evita problemas de StaleObjectStateException
     */
    @Modifying
    @Query(value = """
        INSERT INTO chatbotia.kb_embedding_pgvector (chunk_id, embedding, client_id, kb_id, created_at)
        VALUES (:chunkId, CAST(:embedding AS vector), :clientId, :kbId, NOW())
        ON CONFLICT (chunk_id) 
        DO UPDATE SET 
            embedding = CAST(:embedding AS vector),
            client_id = :clientId,
            kb_id = :kbId
        """, nativeQuery = true)
    void upsertEmbedding(
        @Param("chunkId") UUID chunkId,
        @Param("embedding") String embedding,
        @Param("clientId") UUID clientId,
        @Param("kbId") UUID kbId
    );
    
    /**
     * Búsqueda por similitud de vectores usando cosine distance
     * @param embedding Vector de consulta
     * @param limit Número máximo de resultados
     * @return Lista de chunk IDs ordenados por similitud
     */
    @Query(value = """
        SELECT chunk_id, embedding <=> CAST(:embedding AS vector) as distance
        FROM chatbotia.kb_embedding_pgvector
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findNearestNeighbors(@Param("embedding") String embedding, @Param("limit") int limit);
    
    /**
     * Búsqueda por similitud con filtro de KB
     */
    @Query(value = """
        SELECT chunk_id, embedding <=> CAST(:embedding AS vector) as distance
        FROM chatbotia.kb_embedding_pgvector
        WHERE kb_id = :kbId
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findNearestNeighborsByKb(
        @Param("embedding") String embedding, 
        @Param("kbId") UUID kbId,
        @Param("limit") int limit
    );
    
    /**
     * Búsqueda por similitud con filtro de cliente
     */
    @Query(value = """
        SELECT chunk_id, embedding <=> CAST(:embedding AS vector) as distance
        FROM chatbotia.kb_embedding_pgvector
        WHERE client_id = :clientId
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findNearestNeighborsByClient(
        @Param("embedding") String embedding, 
        @Param("clientId") UUID clientId,
        @Param("limit") int limit
    );
    
    /**
     * Eliminar embeddings por KB
     */
    void deleteByKbId(UUID kbId);
    
    /**
     * Contar embeddings por KB
     */
    long countByKbId(UUID kbId);
}

