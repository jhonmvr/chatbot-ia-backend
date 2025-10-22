package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.messaging.ClientPhoneProviderConfig;
import com.relative.chat.bot.ia.domain.messaging.ProviderConfig;
import com.relative.chat.bot.ia.domain.ports.messaging.ClientPhoneProviderConfigRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ProviderConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Servicio para obtener configuración de proveedores usando la nueva arquitectura parametrizable
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppProviderConfigServiceV2 {
    
    private final ProviderConfigRepository providerConfigRepository;
    private final ClientPhoneProviderConfigRepository clientPhoneProviderConfigRepository;
    
    /**
     * Obtiene la configuración de un proveedor específico para un número de WhatsApp
     * 
     * @param clientPhoneId ID del número de WhatsApp
     * @param providerType Tipo de proveedor (META, TWILIO, WWEBJS)
     * @return Configuración del proveedor o Optional.empty() si no está configurado
     */
    public Optional<ProviderConfiguration> getProviderConfiguration(UuidId<ClientPhone> clientPhoneId, String providerType) {
        // Buscar configuración activa del proveedor para este número
        Optional<ClientPhoneProviderConfig> configOpt = clientPhoneProviderConfigRepository
                .findActiveByClientPhoneIdAndProviderType(clientPhoneId, providerType);
        
        if (configOpt.isEmpty()) {
            log.debug("No se encontró configuración activa para provider {} en clientPhone {}", 
                    providerType, clientPhoneId.value());
            return Optional.empty();
        }
        
        ClientPhoneProviderConfig config = configOpt.get();
        
        // Obtener el esquema del proveedor
        Optional<ProviderConfig> providerConfigOpt = providerConfigRepository.findById(config.providerConfigId());
        if (providerConfigOpt.isEmpty()) {
            log.error("ProviderConfig no encontrado para ID: {}", config.providerConfigId().value());
            return Optional.empty();
        }
        
        ProviderConfig providerConfig = providerConfigOpt.get();
        
        return Optional.of(new ProviderConfiguration(
                providerConfig,
                config.configValues()
        ));
    }
    
    /**
     * Obtiene un valor específico de configuración
     * 
     * @param clientPhoneId ID del número de WhatsApp
     * @param providerType Tipo de proveedor
     * @param fieldName Nombre del campo
     * @return Valor del campo o Optional.empty() si no existe
     */
    public Optional<String> getConfigValue(UuidId<ClientPhone> clientPhoneId, String providerType, String fieldName) {
        return getProviderConfiguration(clientPhoneId, providerType)
                .map(config -> config.getConfigValue(fieldName))
                .orElse(Optional.empty());
    }
    
    /**
     * Obtiene un valor específico de configuración con valor por defecto
     * 
     * @param clientPhoneId ID del número de WhatsApp
     * @param providerType Tipo de proveedor
     * @param fieldName Nombre del campo
     * @param defaultValue Valor por defecto
     * @return Valor del campo o valor por defecto
     */
    public String getConfigValueOrDefault(UuidId<ClientPhone> clientPhoneId, String providerType, String fieldName, String defaultValue) {
        return getConfigValue(clientPhoneId, providerType, fieldName).orElse(defaultValue);
    }
    
    /**
     * Verifica si un campo de configuración existe y tiene valor
     * 
     * @param clientPhoneId ID del número de WhatsApp
     * @param providerType Tipo de proveedor
     * @param fieldName Nombre del campo
     * @return true si el campo existe y tiene valor
     */
    public boolean hasConfigValue(UuidId<ClientPhone> clientPhoneId, String providerType, String fieldName) {
        return getConfigValue(clientPhoneId, providerType, fieldName).isPresent();
    }
    
    /**
     * Configuración completa de un proveedor
     */
    public record ProviderConfiguration(
            ProviderConfig providerConfig,
            Map<String, Object> configValues
    ) {
        
        /**
         * Obtiene un valor de configuración como String
         */
        public Optional<String> getConfigValue(String fieldName) {
            Object value = configValues.get(fieldName);
            return value != null ? Optional.of(value.toString()) : Optional.empty();
        }
        
        /**
         * Obtiene un valor de configuración con valor por defecto
         */
        public String getConfigValueOrDefault(String fieldName, String defaultValue) {
            return getConfigValue(fieldName).orElse(defaultValue);
        }
        
        /**
         * Obtiene la URL base de la API del proveedor
         */
        public String getApiBaseUrl() {
            return providerConfig.apiBaseUrl() != null ? providerConfig.apiBaseUrl() : "";
        }
        
        /**
         * Obtiene la versión de la API del proveedor
         */
        public String getApiVersion() {
            return providerConfig.apiVersion() != null ? providerConfig.apiVersion() : "";
        }
        
        /**
         * Obtiene la URL completa de la API
         */
        public String getFullApiUrl() {
            String baseUrl = getApiBaseUrl();
            String version = getApiVersion();
            
            if (baseUrl.isEmpty()) {
                return "";
            }
            
            if (version.isEmpty()) {
                return baseUrl;
            }
            
            return baseUrl + "/" + version;
        }
        
        /**
         * Verifica si un campo es requerido según el esquema
         */
        public boolean isFieldRequired(String fieldName) {
            return providerConfig.isFieldRequired(fieldName);
        }
        
        /**
         * Verifica si un campo es sensible (requiere encriptación)
         */
        public boolean isFieldSensitive(String fieldName) {
            return providerConfig.isFieldSensitive(fieldName);
        }
    }
}
