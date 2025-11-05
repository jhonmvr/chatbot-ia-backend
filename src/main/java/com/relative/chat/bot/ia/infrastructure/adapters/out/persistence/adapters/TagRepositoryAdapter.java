package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Tag;
import com.relative.chat.bot.ia.domain.ports.messaging.TagRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.TagMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.TagJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter del repositorio de Etiquetas
 */
@Repository
@RequiredArgsConstructor
public class TagRepositoryAdapter implements TagRepository {

    private final TagJpa tagJpa;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Tag> findById(UuidId<Tag> id) {
        return tagJpa.findById(id.value())
                .map(TagMapper::toDomain);
    }

    @Override
    public Optional<Tag> findByName(String name) {
        return tagJpa.findByNameIgnoreCase(name)
                .map(TagMapper::toDomain);
    }

    @Override
    public void save(Tag tag) {
        tagJpa.save(TagMapper.toEntity(tag, em));
    }

    @Override
    public void delete(UuidId<Tag> id) {
        tagJpa.deleteById(id.value());
    }

    @Override
    public List<Tag> findActiveTags() {
        return tagJpa.findByIsActiveTrueOrderByUsageCountDescNameAsc().stream()
                .map(TagMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tag> findByNameContaining(String name) {
        return tagJpa.findByNameContainingIgnoreCaseAndIsActiveTrue(name).stream()
                .map(TagMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tag> findMostUsedTags() {
        return tagJpa.findMostUsedTags().stream()
                .map(TagMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tag> findPopularTags(Integer minUsageCount) {
        return tagJpa.findByIsActiveTrueAndUsageCountGreaterThanOrderByUsageCountDesc(minUsageCount).stream()
                .map(TagMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tag> findSuggestedTags(String searchText) {
        return tagJpa.findSuggestedTags(searchText).stream()
                .map(TagMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Long countContactsByTagId(UuidId<Tag> tagId) {
        return tagJpa.countContactsByTagId(tagId.value());
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UuidId<Tag> excludeId) {
        return tagJpa.existsByNameAndIdNot(name, excludeId.value());
    }

    @Override
    public List<Tag> findAll() {
        return tagJpa.findAll().stream()
                .map(TagMapper::toDomain)
                .collect(Collectors.toList());
    }
}
