package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Implementación Mock de WhatsAppService para desarrollo y testing
 */
@Slf4j
@Service
public class MockWhatsAppAdapter implements WhatsAppService {
    
    @Override
    public String sendMessage(String from, String to, String message) {
        String mockMessageId = "mock_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("📱 [MOCK] Mensaje enviado:");
        log.info("   De: {}", from);
        log.info("   A: {}", to);
        log.info("   Mensaje: {}", message);
        log.info("   ID: {}", mockMessageId);
        
        return mockMessageId;
    }
    
    @Override
    public String sendTemplate(String from, String to, String templateId, String language, Map<String, String> parameters) {
        String mockMessageId = "mock_template_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("📱 [MOCK] Plantilla enviada:");
        log.info("   De: {}", from);
        log.info("   A: {}", to);
        log.info("   Template ID: {}", templateId);
        log.info("   Idioma: {}", language);
        log.info("   Parámetros: {}", parameters);
        log.info("   ID: {}", mockMessageId);
        
        return mockMessageId;
    }
}

