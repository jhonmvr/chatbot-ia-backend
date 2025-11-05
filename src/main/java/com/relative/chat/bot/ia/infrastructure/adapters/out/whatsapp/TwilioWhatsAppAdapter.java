package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Implementación de WhatsAppService usando Twilio API
 */
@Slf4j
@Service
public class TwilioWhatsAppAdapter implements WhatsAppService {
    
    private final WebClient twilioClient;
    private final String accountSid;
    
    public TwilioWhatsAppAdapter(
            @Value("${app.whatsapp.twilio.account-sid:#{null}}") String accountSid,
            @Value("${app.whatsapp.twilio.auth-token:#{null}}") String authToken
    ) {
        this.accountSid = accountSid;
        
        if (accountSid != null && authToken != null) {
            String credentials = accountSid + ":" + authToken;
            String encodedCredentials = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            
            this.twilioClient = WebClient.builder()
                    .baseUrl("https://api.twilio.com/2010-04-01")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                    .build();
            
            log.info("TwilioWhatsAppAdapter inicializado con Account SID: {}", accountSid);
        } else {
            log.warn("Credenciales de Twilio no configuradas. El servicio no funcionará.");
            this.twilioClient = null;
        }
    }
    
    @Override
    public String sendMessage(String from, String to, String message) {
        if (twilioClient == null) {
            log.error("No se puede enviar mensaje: Twilio no está configurado");
            throw new IllegalStateException("Twilio no está configurado");
        }
        
        try {
            // Formatear números para WhatsApp
            String whatsappFrom = formatWhatsAppNumber(from);
            String whatsappTo = formatWhatsAppNumber(to);
            
            log.info("Enviando mensaje de {} a {}: {}", whatsappFrom, whatsappTo, 
                    message.substring(0, Math.min(50, message.length())));
            
            // Preparar datos del formulario
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("From", whatsappFrom);
            formData.add("To", whatsappTo);
            formData.add("Body", message);
            
            // Enviar solicitud a Twilio
            @SuppressWarnings("unchecked")
            Map<String, Object> response = twilioClient.post()
                    .uri("/Accounts/{AccountSid}/Messages.json", accountSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            String messageSid = (String) response.get("sid");
            log.info("Mensaje enviado exitosamente. SID: {}", messageSid);
            
            return messageSid;
            
        } catch (Exception e) {
            log.error("Error al enviar mensaje de WhatsApp: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar mensaje: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String sendTemplate(String from, String to, String templateId, String language, Map<String, String> parameters) {
        // Twilio usa Content SID para plantillas aprobadas
        if (twilioClient == null) {
            log.error("No se puede enviar plantilla: Twilio no está configurado");
            throw new IllegalStateException("Twilio no está configurado");
        }
        
        try {
            String whatsappFrom = formatWhatsAppNumber(from);
            String whatsappTo = formatWhatsAppNumber(to);
            
            log.info("Enviando plantilla {} de {} a {}", templateId, whatsappFrom, whatsappTo);
            
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("From", whatsappFrom);
            formData.add("To", whatsappTo);
            formData.add("ContentSid", templateId);
            
            // Agregar variables de plantilla si hay
            if (parameters != null && !parameters.isEmpty()) {
                parameters.forEach((key, value) -> 
                    formData.add("ContentVariables", String.format("{\"%s\":\"%s\"}", key, value))
                );
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = twilioClient.post()
                    .uri("/Accounts/{AccountSid}/Messages.json", accountSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            String messageSid = (String) response.get("sid");
            log.info("Plantilla enviada exitosamente. SID: {}", messageSid);
            
            return messageSid;
            
        } catch (Exception e) {
            log.error("Error al enviar plantilla de WhatsApp: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar plantilla: " + e.getMessage(), e);
        }
    }
    
    /**
     * Formatea el número de teléfono para WhatsApp en formato Twilio
     * Ejemplo: +593999999999 -> whatsapp:+593999999999
     */
    private String formatWhatsAppNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Número de teléfono no puede estar vacío");
        }
        
        // Si ya tiene el prefijo whatsapp:, devolverlo tal cual
        if (phoneNumber.startsWith("whatsapp:")) {
            return phoneNumber;
        }
        
        // Asegurar que tenga el prefijo +
        String formatted = phoneNumber.startsWith("+") ? phoneNumber : "+" + phoneNumber;
        
        return "whatsapp:" + formatted;
    }
}

