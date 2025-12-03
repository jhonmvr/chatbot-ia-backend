package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.WhatsAppTemplateRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.meta.MetaWhatsAppTemplateApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Servicio para gestión de plantillas de WhatsApp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppTemplateService {
    
    private final WhatsAppTemplateRepository templateRepository;
    private final ClientPhoneRepository clientPhoneRepository;
    private final MetaWhatsAppTemplateApiClient metaApiClient;
    private final WhatsAppProviderConfigServiceV2 configServiceV2;
    
    /**
     * Crea una nueva plantilla
     */
    @Transactional
    public WhatsAppTemplate createTemplate(
        UuidId<ClientPhone> clientPhoneId,
        String name,
        TemplateCategory category,
        String language,
        ParameterFormat parameterFormat,
        List<TemplateComponent> components
    ) {
        log.info("Creando plantilla: {} para cliente: {}", name, clientPhoneId.value());
        
        // Validar que el cliente existe
        if (clientPhoneRepository.findById(clientPhoneId).isEmpty()) {
            throw new IllegalArgumentException("ClientPhone no encontrado: " + clientPhoneId.value());
        }
        
        // Validar unicidad del nombre por cliente y categoría
        if (templateRepository.findByClientPhoneIdAndNameAndCategory(clientPhoneId, name, category).isPresent()) {
            throw new IllegalArgumentException("Ya existe una plantilla con el nombre '" + name + "' para la categoría " + category + " en este cliente");
        }
        
        // Validar componentes
        validateTemplateComponents(components, category);
        
        // Crear plantilla
        WhatsAppTemplate template = WhatsAppTemplate.create(
            clientPhoneId, name, category, language, parameterFormat, components
        );
        
        // Guardar
        templateRepository.save(template);
        
        log.info("Plantilla creada exitosamente: {}", template.id().value());
        return template;
    }

    /**
     * Busca una plantilla por su ID
     */
    @Transactional(readOnly = true)
    public WhatsAppTemplate findById(UuidId<WhatsAppTemplate> templateId) {
        log.info("Buscando plantilla por ID: {}", templateId.value());

        return templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + templateId.value()));
    }

    /**
     * Actualiza una plantilla existente
     */
    @Transactional
    public WhatsAppTemplate updateTemplate(
        UuidId<WhatsAppTemplate> templateId,
        String name,
        String language,
        ParameterFormat parameterFormat,
        List<TemplateComponent> components
    ) {
        log.info("Actualizando plantilla: {}", templateId.value());
        
        WhatsAppTemplate existingTemplate = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + templateId.value()));
        
        // Solo permitir actualización si está en estado DRAFT
        if (existingTemplate.status() != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden actualizar plantillas en estado DRAFT. Estado actual: " + existingTemplate.status());
        }
        
        // Validar componentes
        validateTemplateComponents(components, existingTemplate.category());
        
        // Crear plantilla actualizada
        WhatsAppTemplate updatedTemplate = WhatsAppTemplate.existing(
            existingTemplate.id(),
            existingTemplate.clientPhoneId(),
            name != null ? name : existingTemplate.name(),
            existingTemplate.category(),
            language != null ? language : existingTemplate.language(),
            existingTemplate.status(),
            parameterFormat != null ? parameterFormat : existingTemplate.parameterFormat(),
            components != null ? components : existingTemplate.components(),
            existingTemplate.metaTemplateId(),
            existingTemplate.qualityRating(),
            existingTemplate.rejectionReason(),
            existingTemplate.createdAt(),
            Instant.now()
        );
        
        // Guardar
        templateRepository.save(updatedTemplate);
        
        log.info("Plantilla actualizada exitosamente: {}", templateId.value());
        return updatedTemplate;
    }
    
    /**
     * Sincroniza una plantilla con Meta API
     */
    @Transactional
    public WhatsAppTemplate syncTemplateToMeta(UuidId<WhatsAppTemplate> templateId) {
        log.info("Sincronizando plantilla con Meta API: {}", templateId.value());
        
        WhatsAppTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + templateId.value()));
        
        // Solo sincronizar si está en estado DRAFT
        if (template.status() != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden sincronizar plantillas en estado DRAFT. Estado actual: " + template.status());
        }
        
        try {
            // Obtener configuración de Meta para el cliente
            String accessToken = getMetaAccessToken(template.clientPhoneId());
            String businessAccountId = getMetaBusinessAccountId(template.clientPhoneId());
            String apiVersion = getMetaApiVersion(template.clientPhoneId());
            
            // Crear plantilla en Meta
            MetaWhatsAppTemplateApiClient.MetaTemplateResponse response = metaApiClient
                .createTemplate(accessToken, businessAccountId, apiVersion, template)
                .block(); // En producción usar async
            
            if (response != null) {
                // Actualizar plantilla con ID de Meta y estado PENDING
                WhatsAppTemplate syncedTemplate = template
                    .withMetaTemplateId(response.id())
                    .withStatus(TemplateStatus.PENDING);
                
                templateRepository.save(syncedTemplate);
                
                log.info("Plantilla sincronizada exitosamente con Meta. ID Meta: {}", response.id());
                return syncedTemplate;
            } else {
                throw new RuntimeException("No se recibió respuesta de Meta API");
            }
            
        } catch (Exception e) {
            log.error("Error al sincronizar plantilla con Meta: {}", e.getMessage(), e);
            throw new RuntimeException("Error al sincronizar con Meta: " + e.getMessage(), e);
        }
    }
    
    /**
     * Actualiza el estado de una plantilla desde Meta
     */
    @Transactional
    public WhatsAppTemplate updateTemplateStatusFromMeta(UuidId<WhatsAppTemplate> templateId) {
        log.debug("Actualizando estado de plantilla desde Meta: {}", templateId.value());
        
        WhatsAppTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + templateId.value()));
        
        if (!template.isSyncedWithMeta()) {
            throw new IllegalStateException("La plantilla no está sincronizada con Meta");
        }
        
        try {
            // Obtener configuración de Meta
            String accessToken = getMetaAccessToken(template.clientPhoneId());
            String apiVersion = getMetaApiVersion(template.clientPhoneId());
            
            // Obtener estado desde Meta
            MetaWhatsAppTemplateApiClient.MetaTemplateStatusResponse response = metaApiClient
                .getTemplateStatus(accessToken, apiVersion, template.metaTemplateId())
                .block();
            
            if (response != null) {
                // Mapear estado de Meta a nuestro enum
                TemplateStatus newStatus = mapMetaStatusToTemplateStatus(response.status());
                QualityRating newQualityRating = mapMetaQualityToQualityRating(response.quality_score());
                
                // Actualizar plantilla
                WhatsAppTemplate updatedTemplate = template
                    .withStatus(newStatus)
                    .withQualityRating(newQualityRating);
                
                // Limpiar razón de rechazo si está aprobada
                if (newStatus == TemplateStatus.APPROVED) {
                    updatedTemplate = updatedTemplate.withRejectionReason(null);
                }
                
                templateRepository.save(updatedTemplate);
                
                log.info("Estado de plantilla actualizado desde Meta: {} -> {}", templateId.value(), newStatus);
                return updatedTemplate;
            }
            
        } catch (Exception e) {
            log.error("Error al actualizar estado desde Meta: {}", e.getMessage(), e);
        }
        
        return template;
    }
    
    /**
     * Elimina una plantilla
     */
    @Transactional
    public void deleteTemplate(UuidId<WhatsAppTemplate> templateId) {
        log.info("Eliminando plantilla: {}", templateId.value());
        
        WhatsAppTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + templateId.value()));
        
        // Si está sincronizada con Meta, eliminarla también de Meta
        if (template.isSyncedWithMeta()) {
            try {
                String accessToken = getMetaAccessToken(template.clientPhoneId());
                String apiVersion = getMetaApiVersion(template.clientPhoneId());
                metaApiClient.deleteTemplate(accessToken, apiVersion, template.metaTemplateId()).block();
                log.info("Plantilla eliminada de Meta API: {}", template.metaTemplateId());
            } catch (Exception e) {
                log.warn("No se pudo eliminar plantilla de Meta API: {}", e.getMessage());
            }
        }
        
        // Eliminar de base de datos local
        templateRepository.delete(templateId);
        
        log.info("Plantilla eliminada exitosamente: {}", templateId.value());
    }
    
    /**
     * Obtiene plantillas por cliente
     */
    public List<WhatsAppTemplate> getTemplatesByClient(UuidId<ClientPhone> clientPhoneId) {
        return templateRepository.findByClientPhoneId(clientPhoneId);
    }
    
    /**
     * Obtiene plantillas por estado
     */
    public List<WhatsAppTemplate> getTemplatesByStatus(TemplateStatus status) {
        return templateRepository.findByStatus(status);
    }
    
    /**
     * Obtiene plantillas por categoría
     */
    public List<WhatsAppTemplate> getTemplatesByCategory(TemplateCategory category) {
        return templateRepository.findByCategory(category);
    }
    
    /**
     * Busca plantillas con filtros múltiples a nivel de base de datos
     * Todos los parámetros son opcionales
     */
    @Transactional(readOnly = true)
    public List<WhatsAppTemplate> filterTemplates(
        UuidId<ClientPhone> clientPhoneId,
        com.relative.chat.bot.ia.domain.identity.Client clientId,
        TemplateStatus status,
        TemplateCategory category,
        String search
    ) {
        log.info("Filtrando plantillas - clientPhoneId: {}, clientId: {}, status: {}, category: {}, search: {}", 
            clientPhoneId != null ? clientPhoneId.value() : null,
            clientId != null ? clientId.id().value() : null,
            status,
            category,
            search);
        
        return templateRepository.findByFilters(clientPhoneId, clientId, status, category, search);
    }
    
    /**
     * Obtiene plantillas listas para usar
     */
    public List<WhatsAppTemplate> getReadyTemplates() {
        return templateRepository.findReadyTemplates();
    }
    
    /**
     * Sincroniza todas las plantillas pendientes
     */
    @Transactional
    public void syncAllPendingTemplates() {
        log.info("Iniciando sincronización de todas las plantillas pendientes");
        
        List<WhatsAppTemplate> pendingTemplates = templateRepository.findPendingSyncTemplates();
        
        for (WhatsAppTemplate template : pendingTemplates) {
            try {
                syncTemplateToMeta(template.id());
            } catch (Exception e) {
                log.error("Error al sincronizar plantilla {}: {}", template.id().value(), e.getMessage());
            }
        }
        
        log.info("Sincronización completada. Procesadas {} plantillas", pendingTemplates.size());
    }
    
    /**
     * Actualiza estados de todas las plantillas sincronizadas
     */
    @Transactional
    public void updateAllTemplateStatuses() {
        log.info("Actualizando estados de todas las plantillas sincronizadas");
        
        List<WhatsAppTemplate> syncedTemplates = templateRepository.findTemplatesNeedingStatusUpdate();
        
        for (WhatsAppTemplate template : syncedTemplates) {
            try {
                updateTemplateStatusFromMeta(template.id());
            } catch (Exception e) {
                log.error("Error al actualizar estado de plantilla {}: {}", template.id().value(), e.getMessage());
            }
        }
        
        log.info("Actualización de estados completada. Procesadas {} plantillas", syncedTemplates.size());
    }
    
    /**
     * Valida los componentes de una plantilla
     */
    private void validateTemplateComponents(List<TemplateComponent> components, TemplateCategory category) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("La plantilla debe tener al menos un componente");
        }
        
        // Validaciones específicas por categoría
        switch (category) {
            case AUTHENTICATION -> validateAuthenticationTemplate(components);
            case MARKETING -> validateMarketingTemplate(components);
            case UTILITY -> validateUtilityTemplate(components);
        }
    }
    
    /**
     * Valida plantilla de autenticación
     */
    private void validateAuthenticationTemplate(List<TemplateComponent> components) {
        // Las plantillas de autenticación deben tener un BODY con código OTP
        boolean hasBodyWithOtp = components.stream()
            .anyMatch(comp -> comp.type() == ComponentType.BODY && 
                comp.text() != null && comp.text().contains("{{"));
        
        if (!hasBodyWithOtp) {
            throw new IllegalArgumentException("Las plantillas de autenticación deben tener un componente BODY con parámetros");
        }
    }
    
    /**
     * Valida plantilla de marketing
     */
    private void validateMarketingTemplate(List<TemplateComponent> components) {
        // Las plantillas de marketing pueden tener HEADER multimedia, BODY, FOOTER y BUTTONS
        // Validaciones básicas
        validateBasicTemplateStructure(components);
    }
    
    /**
     * Valida plantilla de utilidad
     */
    private void validateUtilityTemplate(List<TemplateComponent> components) {
        // Las plantillas de utilidad deben tener al menos un BODY
        boolean hasBody = components.stream()
            .anyMatch(comp -> comp.type() == ComponentType.BODY);
        
        if (!hasBody) {
            throw new IllegalArgumentException("Las plantillas de utilidad deben tener un componente BODY");
        }
    }
    
    /**
     * Valida estructura básica de plantilla
     */
    private void validateBasicTemplateStructure(List<TemplateComponent> components) {
        // Validar que no hay componentes duplicados del mismo tipo
        long bodyCount = components.stream().filter(c -> c.type() == ComponentType.BODY).count();
        long headerCount = components.stream().filter(c -> c.type() == ComponentType.HEADER).count();
        long footerCount = components.stream().filter(c -> c.type() == ComponentType.FOOTER).count();
        long buttonsCount = components.stream().filter(c -> c.type() == ComponentType.BUTTONS).count();
        
        if (bodyCount > 1) {
            throw new IllegalArgumentException("Solo puede haber un componente BODY");
        }
        if (headerCount > 1) {
            throw new IllegalArgumentException("Solo puede haber un componente HEADER");
        }
        if (footerCount > 1) {
            throw new IllegalArgumentException("Solo puede haber un componente FOOTER");
        }
        if (buttonsCount > 1) {
            throw new IllegalArgumentException("Solo puede haber un componente BUTTONS");
        }
    }
    
    /**
     * Obtiene el access token de Meta para un cliente desde la configuración de la base de datos
     */
    private String getMetaAccessToken(UuidId<ClientPhone> clientPhoneId) {
        log.debug("Obteniendo access token de Meta para clientPhone: {}", clientPhoneId.value());
        
        // Obtener configuración del proveedor META desde la base de datos
        WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configServiceV2
                .getProviderConfiguration(clientPhoneId, "META")
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró configuración de Meta WhatsApp para clientPhone: " + clientPhoneId.value()
                ));
        
        // Extraer el access_token del map de configValues
        String accessToken = config.getConfigValueOrDefault("access_token", "");
        
        if (accessToken.isEmpty()) {
            throw new IllegalStateException(
                    "access_token no está configurado para clientPhone: " + clientPhoneId.value()
            );
        }
        
        log.debug("Access token de Meta obtenido exitosamente para clientPhone: {}", clientPhoneId.value());
        return accessToken;
    }
    
    /**
     * Obtiene el business account ID de Meta para un cliente desde la configuración de la base de datos
     */
    private String getMetaBusinessAccountId(UuidId<ClientPhone> clientPhoneId) {
        log.debug("Obteniendo business account ID de Meta para clientPhone: {}", clientPhoneId.value());
        
        // Obtener configuración del proveedor META desde la base de datos
        WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configServiceV2
                .getProviderConfiguration(clientPhoneId, "META")
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró configuración de Meta WhatsApp para clientPhone: " + clientPhoneId.value()
                ));
        
        // Intentar diferentes variantes del campo business_account_id
        String businessAccountId = config.getConfigValueOrDefault("business_account_id", "");
        
        // Si no se encuentra, intentar con otras variantes comunes
        if (businessAccountId.isEmpty()) {
            businessAccountId = config.getConfigValueOrDefault("waba_id", "");
        }
        if (businessAccountId.isEmpty()) {
            businessAccountId = config.getConfigValueOrDefault("whatsapp_business_account_id", "");
        }
        
        if (businessAccountId.isEmpty()) {
            throw new IllegalStateException(
                    "business_account_id no está configurado para clientPhone: " + clientPhoneId.value()
            );
        }
        
        log.debug("Business account ID de Meta obtenido exitosamente: {} para clientPhone: {}", 
                businessAccountId, clientPhoneId.value());
        return businessAccountId;
    }
    
    /**
     * Obtiene la versión de la API de Meta para un cliente desde la configuración de la base de datos
     */
    private String getMetaApiVersion(UuidId<ClientPhone> clientPhoneId) {
        log.debug("Obteniendo versión de API de Meta para clientPhone: {}", clientPhoneId.value());
        
        // Obtener configuración del proveedor META desde la base de datos
        WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configServiceV2
                .getProviderConfiguration(clientPhoneId, "META")
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró configuración de Meta WhatsApp para clientPhone: " + clientPhoneId.value()
                ));
        
        // Intentar obtener versión desde configValues primero
        String apiVersion = config.getConfigValueOrDefault("api_version", "");
        
        // Si no está en configValues, intentar obtener del ProviderConfig
        if (apiVersion.isEmpty()) {
            apiVersion = config.getApiVersion();
        }
        
        // Si aún no hay configuración, usar versión por defecto
        if (apiVersion.isEmpty()) {
            apiVersion = "v21.0";
            log.debug("No se encontró versión de API configurada, usando versión por defecto: {}", apiVersion);
        }
        
        log.debug("Versión de API de Meta obtenida: {} para clientPhone: {}", apiVersion, clientPhoneId.value());
        return apiVersion;
    }
    
    /**
     * Mapea estado de Meta a nuestro enum
     */
    private TemplateStatus mapMetaStatusToTemplateStatus(String metaStatus) {
        return switch (metaStatus.toUpperCase()) {
            case "APPROVED" -> TemplateStatus.APPROVED;
            case "PENDING" -> TemplateStatus.PENDING;
            case "REJECTED" -> TemplateStatus.REJECTED;
            case "PAUSED" -> TemplateStatus.PAUSED;
            case "DISABLED" -> TemplateStatus.DISABLED;
            default -> TemplateStatus.PENDING;
        };
    }
    
    /**
     * Mapea calidad de Meta a nuestro enum
     */
    private QualityRating mapMetaQualityToQualityRating(String metaQuality) {
        if (metaQuality == null) {
            return QualityRating.PENDING;
        }
        
        return switch (metaQuality.toUpperCase()) {
            case "HIGH" -> QualityRating.HIGH;
            case "MEDIUM" -> QualityRating.MEDIUM;
            case "LOW" -> QualityRating.LOW;
            default -> QualityRating.PENDING;
        };
    }
}
