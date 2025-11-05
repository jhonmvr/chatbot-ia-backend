package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import java.util.Map;

/**
 * Dominio para configuración específica de proveedores por número de WhatsApp
 */
public record ClientPhoneProviderConfig(
        UuidId<ClientPhoneProviderConfig> id,
        UuidId<ClientPhone> clientPhoneId,
        UuidId<ProviderConfig> providerConfigId,
        Map<String, Object> configValues,
        Boolean isActive
) {
    
    /**
     * Obtiene un valor de configuración específico
     */
    public Object getConfigValue(String fieldName) {
        if (configValues == null) {
            return null;
        }
        return configValues.get(fieldName);
    }
    
    /**
     * Obtiene un valor de configuración como String
     */
    public String getConfigValueAsString(String fieldName) {
        Object value = getConfigValue(fieldName);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Verifica si existe un valor de configuración
     */
    public boolean hasConfigValue(String fieldName) {
        return getConfigValue(fieldName) != null;
    }
    
    /**
     * Obtiene todos los valores de configuración como Map inmutable
     */
    public Map<String, Object> getConfigValues() {
        return configValues != null ? Map.copyOf(configValues) : Map.of();
    }
}
