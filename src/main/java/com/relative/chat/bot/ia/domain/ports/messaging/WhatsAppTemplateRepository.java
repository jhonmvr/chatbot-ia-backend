package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.TemplateCategory;
import com.relative.chat.bot.ia.domain.messaging.TemplateStatus;
import com.relative.chat.bot.ia.domain.messaging.WhatsAppTemplate;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para WhatsAppTemplate
 */
public interface WhatsAppTemplateRepository {
    
    /**
     * Busca una plantilla por ID
     */
    Optional<WhatsAppTemplate> findById(UuidId<WhatsAppTemplate> id);

    /**
     * Busca plantillas por ID del número de teléfono cliente
     */
    List<WhatsAppTemplate> findByClientPhoneId(UuidId<ClientPhone> clientPhoneId);
    
    /**
     * Busca plantillas por estado
     */
    List<WhatsAppTemplate> findByStatus(TemplateStatus status);
    
    /**
     * Busca plantillas por categoría
     */
    List<WhatsAppTemplate> findByCategory(TemplateCategory category);
    
    /**
     * Busca plantillas por cliente y estado
     */
    List<WhatsAppTemplate> findByClientPhoneIdAndStatus(UuidId<ClientPhone> clientPhoneId, TemplateStatus status);
    
    /**
     * Busca plantillas por cliente y categoría
     */
    List<WhatsAppTemplate> findByClientPhoneIdAndCategory(UuidId<ClientPhone> clientPhoneId, TemplateCategory category);
    
    /**
     * Busca plantilla por nombre, cliente y categoría
     */
    Optional<WhatsAppTemplate> findByClientPhoneIdAndNameAndCategory(
        UuidId<ClientPhone> clientPhoneId, 
        String name, 
        TemplateCategory category
    );
    
    /**
     * Busca plantilla por nombre y cliente (sin categoría)
     */
    Optional<WhatsAppTemplate> findByClientPhoneIdAndName(
        UuidId<ClientPhone> clientPhoneId,
        String name
    );

    Optional<WhatsAppTemplate> findByName(String name);
    
    /**
     * Busca plantilla por ID de Meta
     */
    Optional<WhatsAppTemplate> findByMetaTemplateId(String metaTemplateId);
    
    /**
     * Busca plantillas aprobadas y listas para usar
     */
    List<WhatsAppTemplate> findReadyTemplates();
    
    /**
     * Busca plantillas pendientes de sincronización
     */
    List<WhatsAppTemplate> findPendingSyncTemplates();
    
    /**
     * Cuenta plantillas por cliente y estado
     */
    long countByClientPhoneIdAndStatus(UuidId<ClientPhone> clientPhoneId, TemplateStatus status);
    
    /**
     * Cuenta plantillas por cliente y categoría
     */
    long countByClientPhoneIdAndCategory(UuidId<ClientPhone> clientPhoneId, TemplateCategory category);
    
    /**
     * Busca plantillas que necesitan actualización de estado desde Meta
     */
    List<WhatsAppTemplate> findTemplatesNeedingStatusUpdate();
    
    /**
     * Guarda una plantilla
     */
    void save(WhatsAppTemplate template);
    
    /**
     * Elimina una plantilla
     */
    void delete(UuidId<WhatsAppTemplate> id);
    
    /**
     * Obtiene todas las plantillas
     */
    List<WhatsAppTemplate> findAll();
    
    /**
     * Busca plantillas con filtros múltiples
     * Todos los parámetros son opcionales (pueden ser null)
     * 
     * @param clientPhoneId ID del teléfono del cliente (opcional)
     * @param clientId ID del cliente (opcional, busca en todos los teléfonos del cliente)
     * @param status Estado de la plantilla (opcional)
     * @param category Categoría de la plantilla (opcional)
     * @param search Texto para buscar en el nombre (opcional, búsqueda parcial)
     * @return Lista de plantillas que coinciden con los filtros
     */
    List<WhatsAppTemplate> findByFilters(
        UuidId<ClientPhone> clientPhoneId,
        com.relative.chat.bot.ia.domain.identity.Client clientId,
        TemplateStatus status,
        TemplateCategory category,
        String search
    );
}
