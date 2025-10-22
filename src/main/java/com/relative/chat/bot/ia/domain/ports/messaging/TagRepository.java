package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Tag;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para Etiquetas
 */
public interface TagRepository {

    /**
     * Busca una etiqueta por ID
     */
    Optional<Tag> findById(UuidId<Tag> id);

    /**
     * Busca una etiqueta por nombre (case insensitive)
     */
    Optional<Tag> findByName(String name);

    /**
     * Guarda una etiqueta
     */
    void save(Tag tag);

    /**
     * Elimina una etiqueta
     */
    void delete(UuidId<Tag> id);

    /**
     * Obtiene todas las etiquetas activas ordenadas por uso
     */
    List<Tag> findActiveTags();

    /**
     * Busca etiquetas por nombre que contenga el texto
     */
    List<Tag> findByNameContaining(String name);

    /**
     * Obtiene las etiquetas más utilizadas
     */
    List<Tag> findMostUsedTags();

    /**
     * Obtiene etiquetas populares (con más de X usos)
     */
    List<Tag> findPopularTags(Integer minUsageCount);

    /**
     * Obtiene etiquetas sugeridas basadas en texto de búsqueda
     */
    List<Tag> findSuggestedTags(String searchText);

    /**
     * Cuenta el número de contactos asociados a una etiqueta
     */
    Long countContactsByTagId(UuidId<Tag> tagId);

    /**
     * Verifica si existe una etiqueta con el mismo nombre (excluyendo la actual)
     */
    boolean existsByNameAndIdNot(String name, UuidId<Tag> excludeId);

    /**
     * Obtiene todas las etiquetas
     */
    List<Tag> findAll();
}
