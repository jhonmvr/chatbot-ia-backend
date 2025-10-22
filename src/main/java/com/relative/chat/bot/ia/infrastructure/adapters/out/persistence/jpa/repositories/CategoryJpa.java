package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryJpa extends JpaRepository<CategoryEntity, UUID> {
    
    /**
     * Busca categorías por nombre (case insensitive)
     */
    Optional<CategoryEntity> findByNameIgnoreCase(String name);
    
    /**
     * Busca categorías activas ordenadas por sortOrder
     */
    List<CategoryEntity> findByIsActiveTrueOrderBySortOrderAsc();
    
    /**
     * Busca categorías por nombre que contenga el texto (case insensitive)
     */
    List<CategoryEntity> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    
    /**
     * Cuenta el número de contactos asociados a una categoría
     */
    @Query("SELECT COUNT(c) FROM ContactEntity c JOIN c.categories cat WHERE cat.id = :categoryId")
    Long countContactsByCategoryId(@Param("categoryId") UUID categoryId);
    
    /**
     * Busca categorías más utilizadas
     */
    @Query("""
        SELECT cat FROM CategoryEntity cat 
        LEFT JOIN cat.contacts c 
        WHERE cat.isActive = true 
        GROUP BY cat.id 
        ORDER BY COUNT(c) DESC, cat.sortOrder ASC
        """)
    List<CategoryEntity> findMostUsedCategories();
    
    /**
     * Verifica si existe una categoría con el mismo nombre (excluyendo la actual)
     */
    @Query("SELECT COUNT(cat) > 0 FROM CategoryEntity cat WHERE cat.name = :name AND cat.id != :excludeId")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("excludeId") UUID excludeId);
}
