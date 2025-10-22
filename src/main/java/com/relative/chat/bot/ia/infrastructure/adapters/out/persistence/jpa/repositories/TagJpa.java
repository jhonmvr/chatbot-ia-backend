package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagJpa extends JpaRepository<TagEntity, UUID> {
    
    /**
     * Busca etiquetas por nombre (case insensitive)
     */
    Optional<TagEntity> findByNameIgnoreCase(String name);
    
    /**
     * Busca etiquetas activas ordenadas por uso descendente
     */
    List<TagEntity> findByIsActiveTrueOrderByUsageCountDescNameAsc();
    
    /**
     * Busca etiquetas por nombre que contenga el texto (case insensitive)
     */
    List<TagEntity> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    
    /**
     * Cuenta el número de contactos asociados a una etiqueta
     */
    @Query("SELECT COUNT(c) FROM ContactEntity c JOIN c.tags t WHERE t.id = :tagId")
    Long countContactsByTagId(@Param("tagId") UUID tagId);
    
    /**
     * Busca etiquetas más utilizadas
     */
    @Query("""
        SELECT t FROM TagEntity t 
        LEFT JOIN t.contacts c 
        WHERE t.isActive = true 
        GROUP BY t.id 
        ORDER BY COUNT(c) DESC, t.usageCount DESC, t.name ASC
        """)
    List<TagEntity> findMostUsedTags();
    
    /**
     * Busca etiquetas populares (con más de X usos)
     */
    List<TagEntity> findByIsActiveTrueAndUsageCountGreaterThanOrderByUsageCountDesc(Integer minUsageCount);
    
    /**
     * Verifica si existe una etiqueta con el mismo nombre (excluyendo la actual)
     */
    @Query("SELECT COUNT(t) > 0 FROM TagEntity t WHERE t.name = :name AND t.id != :excludeId")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("excludeId") UUID excludeId);
    
    /**
     * Busca etiquetas sugeridas basadas en texto de búsqueda
     */
    @Query("""
        SELECT t FROM TagEntity t 
        WHERE t.isActive = true 
        AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :searchText, '%')) 
             OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchText, '%')))
        ORDER BY t.usageCount DESC, t.name ASC
        """)
    List<TagEntity> findSuggestedTags(@Param("searchText") String searchText);
}
