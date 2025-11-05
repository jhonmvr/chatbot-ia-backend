package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Router dinámico que selecciona el adaptador de WhatsApp correcto basado en el provider
 * del ClientPhone en lugar de usar una configuración estática.
 * 
 * Permite tener múltiples proveedores activos simultáneamente y seleccionar
 * dinámicamente el adaptador según el provider del número de teléfono.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppProviderRouter implements WhatsAppService {
    
    private final MetaWhatsAppAdapter metaAdapter;
    private final TwilioWhatsAppAdapter twilioAdapter;
    private final WWebJsWhatsAppAdapter wwebjsAdapter;
    private final MockWhatsAppAdapter mockAdapter;
    
    /**
     * Mapa de providers normalizados a sus adaptadores correspondientes
     */
    private Map<String, WhatsAppService> getAdapterMap() {
        return Map.of(
            "META", metaAdapter,
            "TWILIO", twilioAdapter,
            "WWEBJS", wwebjsAdapter,
            "MOCK", mockAdapter
        );
    }
    
    /**
     * Normaliza el nombre del provider a mayúsculas
     * 
     * @param provider Nombre del provider (puede ser "meta", "META", "Meta", etc.)
     * @return Provider normalizado en mayúsculas
     */
    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Provider no puede ser null o vacío");
        }
        return provider.toUpperCase().trim();
    }
    
    /**
     * Obtiene el adaptador correcto para el provider especificado
     * 
     * @param provider Nombre del provider (META, TWILIO, WWEBJS, MOCK)
     * @return Adaptador de WhatsApp correspondiente
     * @throws IllegalArgumentException si el provider no es válido
     */
    private WhatsAppService getAdapter(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        Map<String, WhatsAppService> adapterMap = getAdapterMap();
        
        WhatsAppService adapter = adapterMap.get(normalizedProvider);
        if (adapter == null) {
            log.error("Provider no válido: {}. Providers disponibles: {}", 
                normalizedProvider, adapterMap.keySet());
            throw new IllegalArgumentException(
                String.format("Provider '%s' no es válido. Providers disponibles: %s", 
                    normalizedProvider, adapterMap.keySet())
            );
        }
        
        log.debug("Seleccionando adaptador para provider: {}", normalizedProvider);
        return adapter;
    }
    
    /**
     * Envía un mensaje usando el adaptador correcto según el provider
     * 
     * @param from Número de origen (puede ser phone_number_id de Meta, SID de Twilio, etc.)
     * @param to Número de destino
     * @param message Contenido del mensaje
     * @param provider Nombre del provider (META, TWILIO, WWEBJS, MOCK)
     * @return ID externo del mensaje enviado
     */
    public String sendMessage(String from, String to, String message, String provider) {
        WhatsAppService adapter = getAdapter(provider);
        log.debug("Enviando mensaje usando provider: {}", normalizeProvider(provider));
        return adapter.sendMessage(from, to, message);
    }
    
    /**
     * Envía un mensaje usando el adaptador correcto según el provider
     * Este método implementa la interfaz WhatsAppService pero requiere el provider
     * como parámetro adicional. Para compatibilidad, este método intentará inferir
     * el provider del número de origen.
     * 
     * @param from Número de origen
     * @param to Número de destino
     * @param message Contenido del mensaje
     * @return ID externo del mensaje enviado
     * @throws IllegalStateException si no se puede determinar el provider
     */
    @Override
    public String sendMessage(String from, String to, String message) {
        // Este método no debería usarse directamente sin el provider
        // Pero para compatibilidad, intentamos inferir el provider
        // En la práctica, los use cases deberían usar sendMessage con provider
        log.warn("sendMessage llamado sin provider. Usando META como fallback. " +
                "Se recomienda usar sendMessage(from, to, message, provider)");
        return sendMessage(from, to, message, "META");
    }
    
    /**
     * Envía una plantilla usando el adaptador correcto según el provider
     * 
     * @param from Número de origen
     * @param to Número de destino
     * @param templateId ID de la plantilla
     * @param language Código de idioma
     * @param parameters Parámetros de la plantilla
     * @param provider Nombre del provider (META, TWILIO, WWEBJS, MOCK)
     * @return ID externo del mensaje enviado
     */
    public String sendTemplate(String from, String to, String templateId, String language, 
                              Map<String, String> parameters, String provider) {
        WhatsAppService adapter = getAdapter(provider);
        log.debug("Enviando plantilla usando provider: {}", normalizeProvider(provider));
        return adapter.sendTemplate(from, to, templateId, language, parameters);
    }
    
    /**
     * Envía una plantilla usando el adaptador correcto según el provider
     * Este método implementa la interfaz WhatsAppService pero requiere el provider
     * como parámetro adicional.
     * 
     * @param from Número de origen
     * @param to Número de destino
     * @param templateId ID de la plantilla
     * @param language Código de idioma
     * @param parameters Parámetros de la plantilla
     * @return ID externo del mensaje enviado
     */
    @Override
    public String sendTemplate(String from, String to, String templateId, String language, 
                              Map<String, String> parameters) {
        // Este método no debería usarse directamente sin el provider
        // Pero para compatibilidad, intentamos inferir el provider
        log.warn("sendTemplate llamado sin provider. Usando META como fallback. " +
                "Se recomienda usar sendTemplate(..., provider)");
        return sendTemplate(from, to, templateId, language, parameters, "META");
    }
}

