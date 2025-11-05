package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.WhatsAppTemplateEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper para WhatsAppTemplate
 */
public class WhatsAppTemplateMapper {
    
    /**
     * Convierte de entidad JPA a dominio
     */
    public static WhatsAppTemplate toDomain(WhatsAppTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return WhatsAppTemplate.existing(
            UuidId.of(entity.getId()),
            UuidId.of(entity.getClientPhoneEntity().getId()),
            entity.getName(),
            TemplateCategory.valueOf(entity.getCategory()),
            entity.getLanguage(),
            TemplateStatus.valueOf(entity.getStatus()),
            entity.getParameterFormat() != null ? ParameterFormat.valueOf(entity.getParameterFormat()) : null,
            mapComponentsFromJson(entity.getComponents()),
            entity.getMetaTemplateId(),
            entity.getQualityRating() != null ? QualityRating.valueOf(entity.getQualityRating()) : QualityRating.PENDING,
            entity.getRejectionReason(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null,
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant() : null
        );
    }
    
    /**
     * Convierte de dominio a entidad JPA
     */
    public static WhatsAppTemplateEntity toEntity(WhatsAppTemplate domain, ClientPhoneEntity clientPhoneEntity) {
        WhatsAppTemplateEntity entity = new WhatsAppTemplateEntity();
        entity.setId(domain.id().value());
        entity.setClientPhoneEntity(clientPhoneEntity);
        entity.setName(domain.name());
        entity.setCategory(domain.category().name());
        entity.setLanguage(domain.language());
        entity.setStatus(domain.status().name());
        entity.setParameterFormat(domain.parameterFormat() != null ? domain.parameterFormat().name() : null);
        entity.setMetaTemplateId(domain.metaTemplateId());
        entity.setQualityRating(domain.qualityRating().name());
        entity.setRejectionReason(domain.rejectionReason());
        entity.setComponents(mapComponentsToJson(domain.components()));
        entity.setCreatedAt(domain.createdAt() != null ? OffsetDateTime.ofInstant(domain.createdAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        entity.setUpdatedAt(domain.updatedAt() != null ? OffsetDateTime.ofInstant(domain.updatedAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        
        return entity;
    }
    
    /**
     * Mapea componentes desde JSON a objetos de dominio
     */
    @SuppressWarnings("unchecked")
    private static List<TemplateComponent> mapComponentsFromJson(List<Map<String, Object>> componentsJson) {
        if (componentsJson == null || componentsJson.isEmpty()) {
            return new ArrayList<>();
        }
        
        return componentsJson.stream()
            .map(componentMap -> {
                ComponentType type = ComponentType.valueOf((String) componentMap.get("type"));
                String text = (String) componentMap.get("text");
                
                // Mapear parámetros
                List<ComponentParameter> parameters = null;
                if (componentMap.containsKey("parameters")) {
                    List<Map<String, Object>> paramsJson = (List<Map<String, Object>>) componentMap.get("parameters");
                    parameters = paramsJson.stream()
                        .map(paramMap -> new ComponentParameter(
                            (String) paramMap.get("type"),
                            (String) paramMap.get("text"),
                            (String) paramMap.get("parameterName"),
                            (String) paramMap.get("example")
                        ))
                        .collect(Collectors.toList());
                }
                
                // Mapear botones
                List<TemplateButton> buttons = null;
                if (componentMap.containsKey("buttons")) {
                    List<Map<String, Object>> buttonsJson = (List<Map<String, Object>>) componentMap.get("buttons");
                    buttons = buttonsJson.stream()
                        .map(buttonMap -> new TemplateButton(
                            (String) buttonMap.get("type"),
                            (String) buttonMap.get("text"),
                            (String) buttonMap.get("url"),
                            (String) buttonMap.get("phoneNumber"),
                            (String) buttonMap.get("otpType"),
                            (String) buttonMap.get("autofillText"),
                            (String) buttonMap.get("packageName"),
                            (String) buttonMap.get("signatureHash"),
                            (String) buttonMap.get("example")
                        ))
                        .collect(Collectors.toList());
                }
                
                // Mapear media
                MediaComponent media = null;
                if (componentMap.containsKey("media")) {
                    Map<String, Object> mediaMap = (Map<String, Object>) componentMap.get("media");
                    media = new MediaComponent(
                        (String) mediaMap.get("type"),
                        (String) mediaMap.get("url"),
                        (String) mediaMap.get("mediaId"),
                        (String) mediaMap.get("filename"),
                        (String) mediaMap.get("altText")
                    );
                }
                
                // Nuevos campos de TemplateComponent
                String format = (String) componentMap.get("format");
                Boolean addSecurityRecommendation = componentMap.get("addSecurityRecommendation") != null ? 
                    (Boolean) componentMap.get("addSecurityRecommendation") : null;
                Integer codeExpirationMinutes = componentMap.get("codeExpirationMinutes") != null ? 
                    (Integer) componentMap.get("codeExpirationMinutes") : null;
                
                return new TemplateComponent(type, text, parameters, buttons, media, format, 
                    addSecurityRecommendation, codeExpirationMinutes);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Mapea componentes de dominio a JSON
     */
    private static List<Map<String, Object>> mapComponentsToJson(List<TemplateComponent> components) {
        if (components == null || components.isEmpty()) {
            return new ArrayList<>();
        }
        
        return components.stream()
            .map(component -> {
                Map<String, Object> componentMap = new java.util.HashMap<>();
                componentMap.put("type", component.type().name());
                
                if (component.text() != null) {
                    componentMap.put("text", component.text());
                }
                
                if (component.parameters() != null && !component.parameters().isEmpty()) {
                    List<Map<String, Object>> paramsJson = component.parameters().stream()
                        .map(param -> {
                            Map<String, Object> paramMap = new java.util.HashMap<>();
                            paramMap.put("type", param.type());
                            paramMap.put("text", param.text());
                            if (param.parameterName() != null) {
                                paramMap.put("parameterName", param.parameterName());
                            }
                            if (param.example() != null) {
                                paramMap.put("example", param.example());
                            }
                            return paramMap;
                        })
                        .collect(Collectors.toList());
                    componentMap.put("parameters", paramsJson);
                }
                
                if (component.buttons() != null && !component.buttons().isEmpty()) {
                    List<Map<String, Object>> buttonsJson = component.buttons().stream()
                        .map(button -> {
                            Map<String, Object> buttonMap = new java.util.HashMap<>();
                            buttonMap.put("type", button.type());
                            buttonMap.put("text", button.text());
                            if (button.url() != null) {
                                buttonMap.put("url", button.url());
                            }
                            if (button.phoneNumber() != null) {
                                buttonMap.put("phoneNumber", button.phoneNumber());
                            }
                            if (button.otpType() != null) {
                                buttonMap.put("otpType", button.otpType());
                            }
                            if (button.autofillText() != null) {
                                buttonMap.put("autofillText", button.autofillText());
                            }
                            if (button.packageName() != null) {
                                buttonMap.put("packageName", button.packageName());
                            }
                            if (button.signatureHash() != null) {
                                buttonMap.put("signatureHash", button.signatureHash());
                            }
                            if (button.example() != null) {
                                buttonMap.put("example", button.example());
                            }
                            return buttonMap;
                        })
                        .collect(Collectors.toList());
                    componentMap.put("buttons", buttonsJson);
                }
                
                if (component.media() != null) {
                    Map<String, Object> mediaMap = new java.util.HashMap<>();
                    mediaMap.put("type", component.media().type());
                    if (component.media().url() != null) {
                        mediaMap.put("url", component.media().url());
                    }
                    if (component.media().mediaId() != null) {
                        mediaMap.put("mediaId", component.media().mediaId());
                    }
                    if (component.media().filename() != null) {
                        mediaMap.put("filename", component.media().filename());
                    }
                    if (component.media().altText() != null) {
                        mediaMap.put("altText", component.media().altText());
                    }
                    componentMap.put("media", mediaMap);
                }
                
                // Nuevos campos de TemplateComponent
                if (component.format() != null) {
                    componentMap.put("format", component.format());
                }
                if (component.addSecurityRecommendation() != null) {
                    componentMap.put("addSecurityRecommendation", component.addSecurityRecommendation());
                }
                if (component.codeExpirationMinutes() != null) {
                    componentMap.put("codeExpirationMinutes", component.codeExpirationMinutes());
                }
                
                return componentMap;
            })
            .collect(Collectors.toList());
    }
}
