package com.relative.chat.bot.ia.interfaces.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * API de salud y monitoreo del sistema
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {
    
    private final DataSource dataSource;
    
    @Value("${spring.application.name:chatbot-ia}")
    private String applicationName;
    
    @Value("${app.whatsapp.provider:mock}")
    private String whatsappProvider;
    
    @Value("${app.ai.provider:openai}")
    private String aiProvider;
    
    /**
     * Health check básico
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("application", applicationName);
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Health check detallado con dependencias
     * GET /api/health/full
     */
    @GetMapping("/health/full")
    public ResponseEntity<Map<String, Object>> healthFull() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("application", applicationName);
        
        // Check base de datos
        Map<String, Object> database = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            database.put("status", conn.isValid(2) ? "UP" : "DOWN");
            database.put("driver", conn.getMetaData().getDatabaseProductName());
            database.put("version", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            log.error("Error al verificar base de datos: {}", e.getMessage());
            database.put("status", "DOWN");
            database.put("error", e.getMessage());
        }
        health.put("database", database);
        
        // Configuración de servicios
        Map<String, Object> services = new HashMap<>();
        services.put("whatsapp", Map.of(
                "provider", whatsappProvider,
                "status", "CONFIGURED"
        ));
        services.put("ai", Map.of(
                "provider", aiProvider,
                "status", "CONFIGURED"
        ));
        health.put("services", services);
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Información de la aplicación
     * GET /api/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", applicationName);
        info.put("version", "1.0.0");
        info.put("description", "Chatbot con IA para WhatsApp");
        info.put("whatsappProvider", whatsappProvider);
        info.put("aiProvider", aiProvider);
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("webhook", "/webhook/whatsapp");
        endpoints.put("webhookMeta", "/webhooks/whatsapp/meta");
        endpoints.put("test", "/webhook/whatsapp/test");
        endpoints.put("conversations", "/api/conversations");
        endpoints.put("knowledgeBase", "/api/knowledge-base");
        endpoints.put("health", "/api/health");
        
        info.put("endpoints", endpoints);
        
        return ResponseEntity.ok(info);
    }
}

