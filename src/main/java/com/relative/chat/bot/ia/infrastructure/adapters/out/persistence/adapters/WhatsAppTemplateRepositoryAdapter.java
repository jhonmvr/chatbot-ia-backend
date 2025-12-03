package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.messaging.WhatsAppTemplateRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.WhatsAppTemplateEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.WhatsAppTemplateMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ClientPhoneJpa;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.WhatsAppTemplateJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de repositorio para WhatsAppTemplate
 */
@Component
@RequiredArgsConstructor
public class WhatsAppTemplateRepositoryAdapter implements WhatsAppTemplateRepository {
    
    private final WhatsAppTemplateJpa whatsAppTemplateJpa;
    private final ClientPhoneJpa clientPhoneJpa;
    
    @Override
    public Optional<WhatsAppTemplate> findById(UuidId<WhatsAppTemplate> id) {
        return whatsAppTemplateJpa.findById(id.value())
                .map(WhatsAppTemplateMapper::toDomain);
    }
    
    @Override
    public List<WhatsAppTemplate> findByClientPhoneId(UuidId<ClientPhone> clientPhoneId) {
        return whatsAppTemplateJpa.findByClientPhoneEntityId(clientPhoneId.value())
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WhatsAppTemplate> findByStatus(TemplateStatus status) {
        return whatsAppTemplateJpa.findByStatus(status.name())
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WhatsAppTemplate> findByCategory(TemplateCategory category) {
        return whatsAppTemplateJpa.findByCategory(category.name())
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WhatsAppTemplate> findByClientPhoneIdAndStatus(UuidId<ClientPhone> clientPhoneId, TemplateStatus status) {
        return whatsAppTemplateJpa.findByClientPhoneEntityIdAndStatus(clientPhoneId.value(), status.name())
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WhatsAppTemplate> findByClientPhoneIdAndCategory(UuidId<ClientPhone> clientPhoneId, TemplateCategory category) {
        return whatsAppTemplateJpa.findByClientPhoneEntityIdAndCategory(clientPhoneId.value(), category.name())
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<WhatsAppTemplate> findByClientPhoneIdAndNameAndCategory(
        UuidId<ClientPhone> clientPhoneId, 
        String name, 
        TemplateCategory category
    ) {
        // Si category es null, usar el método sin categoría
        if (category == null) {
            return findByClientPhoneIdAndName(clientPhoneId, name);
        }
        return whatsAppTemplateJpa.findByClientPhoneEntityIdAndNameAndCategory(clientPhoneId.value(), name, category.name())
                .map(WhatsAppTemplateMapper::toDomain);
    }
    
    @Override
    public Optional<WhatsAppTemplate> findByClientPhoneIdAndName(
        UuidId<ClientPhone> clientPhoneId,
        String name
    ) {
        return whatsAppTemplateJpa.findByClientPhoneEntityIdAndName(clientPhoneId.value(), name)
                .map(WhatsAppTemplateMapper::toDomain);
    }

    @Override
    public Optional<WhatsAppTemplate> findByName(String name) {
        return whatsAppTemplateJpa.findByName(name).map(WhatsAppTemplateMapper::toDomain);
    }

    @Override
    public Optional<WhatsAppTemplate> findByMetaTemplateId(String metaTemplateId) {
        return whatsAppTemplateJpa.findByMetaTemplateId(metaTemplateId)
                .map(WhatsAppTemplateMapper::toDomain);
    }
    
    @Override
    public List<WhatsAppTemplate> findReadyTemplates() {
        return whatsAppTemplateJpa.findReadyTemplates()
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WhatsAppTemplate> findPendingSyncTemplates() {
        return whatsAppTemplateJpa.findPendingSyncTemplates()
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByClientPhoneIdAndStatus(UuidId<ClientPhone> clientPhoneId, TemplateStatus status) {
        return whatsAppTemplateJpa.countByClientPhoneIdAndStatus(clientPhoneId.value(), status.name());
    }
    
    @Override
    public long countByClientPhoneIdAndCategory(UuidId<ClientPhone> clientPhoneId, TemplateCategory category) {
        return whatsAppTemplateJpa.countByClientPhoneIdAndCategory(clientPhoneId.value(), category.name());
    }
    
    @Override
    public List<WhatsAppTemplate> findTemplatesNeedingStatusUpdate() {
        return whatsAppTemplateJpa.findTemplatesNeedingStatusUpdate()
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void save(WhatsAppTemplate template) {
        ClientPhoneEntity clientPhoneEntity = clientPhoneJpa.findById(template.clientPhoneId().value())
                .orElseThrow(() -> new IllegalArgumentException("ClientPhone no encontrado: " + template.clientPhoneId().value()));
        
        WhatsAppTemplateEntity entity = WhatsAppTemplateMapper.toEntity(template, clientPhoneEntity);
        whatsAppTemplateJpa.save(entity);
    }
    
    @Override
    public void delete(UuidId<WhatsAppTemplate> id) {
        whatsAppTemplateJpa.deleteById(id.value());
    }
    
    @Override
    public List<WhatsAppTemplate> findAll() {
        return whatsAppTemplateJpa.findAll()
                .stream()
                .map(WhatsAppTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WhatsAppTemplate> findByFilters(
        com.relative.chat.bot.ia.domain.common.UuidId<ClientPhone> clientPhoneId,
        com.relative.chat.bot.ia.domain.identity.Client clientId,
        com.relative.chat.bot.ia.domain.messaging.TemplateStatus status,
        com.relative.chat.bot.ia.domain.messaging.TemplateCategory category,
        String search
    ) {
        java.util.UUID clientPhoneIdValue = clientPhoneId != null ? clientPhoneId.value() : null;
        java.util.UUID clientIdValue = clientId != null ? clientId.id().value() : null;
        String statusValue = status != null ? status.name() : null;
        String categoryValue = category != null ? category.name() : null;
        String searchValue = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        return whatsAppTemplateJpa.findByFilters(
            clientPhoneIdValue,
            clientIdValue,
            statusValue,
            categoryValue,
            searchValue
        )
        .stream()
        .map(WhatsAppTemplateMapper::toDomain)
        .collect(Collectors.toList());
    }
}
