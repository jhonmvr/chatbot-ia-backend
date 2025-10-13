package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.KbVectorRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface KbVectorRefJpa extends JpaRepository<KbVectorRefEntity, UUID> {
    
    /**
     * Inserta o actualiza una referencia vectorial usando UPSERT de PostgreSQL
     */
    @Modifying
    @Query(value = """
        INSERT INTO chatbotia.kb_vector_ref (chunk_id, backend, index_name, vector_id, created_at)
        VALUES (:chunkId, :backend, :indexName, :vectorId, NOW())
        ON CONFLICT (chunk_id) 
        DO UPDATE SET 
            backend = :backend,
            index_name = :indexName,
            vector_id = :vectorId
        """, nativeQuery = true)
    void upsertVectorRef(
        @Param("chunkId") UUID chunkId,
        @Param("backend") String backend,
        @Param("indexName") String indexName,
        @Param("vectorId") String vectorId
    );
}
