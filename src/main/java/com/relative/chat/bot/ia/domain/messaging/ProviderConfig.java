package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import java.util.Map;

/**
 * Dominio para configuración parametrizable de proveedores de WhatsApp
 */
public record ProviderConfig(
        UuidId<ProviderConfig> id,
        String providerName,
        String providerType, // META, TWILIO, WWEBJS, etc.
        String displayName,
        String description,
        String apiBaseUrl,
        String apiVersion,
        String webhookUrlTemplate,
        Boolean isActive,
        Boolean isDefault,
        Map<String, Object> configSchema
) {
    
    /**
     * Obtiene un campo específico del esquema de configuración
     */
    public Object getSchemaField(String fieldName) {
        if (configSchema == null) {
            return null;
        }
        return configSchema.get(fieldName);
    }
    
    /**
     * Verifica si un campo es requerido según el esquema
     */
    public boolean isFieldRequired(String fieldName) {
        if (configSchema == null) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<String> requiredFields = (java.util.List<String>) configSchema.get("required_fields");
        return requiredFields != null && requiredFields.contains(fieldName);
    }
    
    /**
     * Verifica si un campo es opcional según el esquema
     */
    public boolean isFieldOptional(String fieldName) {
        if (configSchema == null) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<String> optionalFields = (java.util.List<String>) configSchema.get("optional_fields");
        return optionalFields != null && optionalFields.contains(fieldName);
    }
    
    /**
     * Obtiene la configuración de un campo específico
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFieldConfig(String fieldName) {
        if (configSchema == null) {
            return null;
        }
        
        Map<String, Object> fieldConfigs = (Map<String, Object>) configSchema.get("field_configs");
        if (fieldConfigs == null) {
            return null;
        }
        
        return (Map<String, Object>) fieldConfigs.get(fieldName);
    }
    
    /**
     * Verifica si un campo es sensible (requiere encriptación)
     */
    public boolean isFieldSensitive(String fieldName) {
        Map<String, Object> fieldConfig = getFieldConfig(fieldName);
        if (fieldConfig == null) {
            return false;
        }
        
        Boolean sensitive = (Boolean) fieldConfig.get("sensitive");
        return sensitive != null && sensitive;
    }
}
