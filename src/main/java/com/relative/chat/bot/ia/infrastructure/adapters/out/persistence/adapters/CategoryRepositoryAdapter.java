package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Category;
import com.relative.chat.bot.ia.domain.ports.messaging.CategoryRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.CategoryMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.CategoryJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter del repositorio de Categorías
 */
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpa categoryJpa;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Category> findById(UuidId<Category> id) {
        return categoryJpa.findById(id.value())
                .map(CategoryMapper::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return categoryJpa.findByNameIgnoreCase(name)
                .map(CategoryMapper::toDomain);
    }

    @Override
    public void save(Category category) {
        categoryJpa.save(CategoryMapper.toEntity(category, em));
    }

    @Override
    public void delete(UuidId<Category> id) {
        categoryJpa.deleteById(id.value());
    }

    @Override
    public List<Category> findActiveCategories() {
        return categoryJpa.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(CategoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Category> findByNameContaining(String name) {
        return categoryJpa.findByNameContainingIgnoreCaseAndIsActiveTrue(name).stream()
                .map(CategoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Category> findMostUsedCategories() {
        return categoryJpa.findMostUsedCategories().stream()
                .map(CategoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Long countContactsByCategoryId(UuidId<Category> categoryId) {
        return categoryJpa.countContactsByCategoryId(categoryId.value());
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UuidId<Category> excludeId) {
        return categoryJpa.existsByNameAndIdNot(name, excludeId.value());
    }

    @Override
    public List<Category> findAll() {
        return categoryJpa.findAll().stream()
                .map(CategoryMapper::toDomain)
                .collect(Collectors.toList());
    }
}
