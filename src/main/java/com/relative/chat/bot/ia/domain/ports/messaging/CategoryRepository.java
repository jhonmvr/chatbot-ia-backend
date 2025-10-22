package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Category;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para Categorías
 */
public interface CategoryRepository {

    /**
     * Busca una categoría por ID
     */
    Optional<Category> findById(UuidId<Category> id);

    /**
     * Busca una categoría por nombre (case insensitive)
     */
    Optional<Category> findByName(String name);

    /**
     * Guarda una categoría
     */
    void save(Category category);

    /**
     * Elimina una categoría
     */
    void delete(UuidId<Category> id);

    /**
     * Obtiene todas las categorías activas ordenadas por sortOrder
     */
    List<Category> findActiveCategories();

    /**
     * Busca categorías por nombre que contenga el texto
     */
    List<Category> findByNameContaining(String name);

    /**
     * Obtiene las categorías más utilizadas
     */
    List<Category> findMostUsedCategories();

    /**
     * Cuenta el número de contactos asociados a una categoría
     */
    Long countContactsByCategoryId(UuidId<Category> categoryId);

    /**
     * Verifica si existe una categoría con el mismo nombre (excluyendo la actual)
     */
    boolean existsByNameAndIdNot(String name, UuidId<Category> excludeId);

    /**
     * Obtiene todas las categorías
     */
    List<Category> findAll();
}
