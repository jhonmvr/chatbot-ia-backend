package com.relative.chat.bot.ia.interfaces.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Sistema", description = "Endpoints de monitoreo y estado del sistema")
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
    @Operation(
        summary = "Health check básico",
        description = "Retorna el estado básico de la aplicación"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Aplicación funcionando correctamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "UP",
                      "timestamp": "2025-10-03T10:30:00Z",
                      "application": "chatbot-ia"
                    }
                    """)
            )
        )
    })
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
    @Operation(
        summary = "Health check detallado",
        description = "Retorna el estado detallado de la aplicación incluyendo base de datos y servicios externos"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Estado detallado del sistema",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "UP",
                      "timestamp": "2025-10-03T10:30:00Z",
                      "application": "chatbot-ia",
                      "database": {
                        "status": "UP",
                        "driver": "PostgreSQL",
                        "version": "16.0"
                      },
                      "services": {
                        "whatsapp": {
                          "provider": "meta",
                          "status": "CONFIGURED"
                        },
                        "ai": {
                          "provider": "openai",
                          "status": "CONFIGURED"
                        }
                      }
                    }
                    """)
            )
        )
    })
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
    @Operation(
        summary = "Información de la aplicación",
        description = "Retorna información general de la aplicación, versión y endpoints disponibles"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Información de la aplicación",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "name": "chatbot-ia",
                      "version": "1.0.0",
                      "description": "Chatbot con IA para WhatsApp",
                      "whatsappProvider": "meta",
                      "aiProvider": "openai",
                      "endpoints": {
                        "webhook": "/webhook/whatsapp",
                        "webhookMeta": "/webhooks/whatsapp/meta",
                        "conversations": "/api/conversations",
                        "knowledgeBase": "/api/knowledge-base",
                        "health": "/api/health"
                      }
                    }
                    """)
            )
        )
    })
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

