package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.dto.AvailabilityConfig;
import com.relative.chat.bot.ia.application.services.AvailabilityConfigService;
import com.relative.chat.bot.ia.domain.common.UuidId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller para gestionar configuración de horarios de disponibilidad y jornadas laborales
 */
@RestController
@RequestMapping("/api/calendar-provider-accounts/{accountId}/availability-config")
@RequiredArgsConstructor
@Tag(name = "Availability Configuration", description = "API para gestionar configuración de horarios de disponibilidad y jornadas laborales")
public class AvailabilityConfigController {
    
    private final AvailabilityConfigService configService;
    
    @GetMapping
    @Operation(summary = "Obtener configuración de disponibilidad")
    public ResponseEntity<Map<String, Object>> getConfig(
            @PathVariable UUID accountId
    ) {
        try {
            AvailabilityConfig config = configService.getAvailabilityConfig(
                    UuidId.of(accountId)
            );
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", config
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    @PutMapping
    @Operation(summary = "Actualizar configuración de disponibilidad")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable UUID accountId,
            @RequestBody AvailabilityConfig config
    ) {
        try {
            configService.updateAvailabilityConfig(
                    UuidId.of(accountId),
                    config
            );
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Configuración actualizada exitosamente"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint específico para configurar jornadas laborales
     * POST /api/calendar-provider-accounts/{accountId}/availability-config/working-hours
     */
    @PostMapping("/working-hours")
    @Operation(summary = "Configurar jornadas laborales", 
               description = "Configura los horarios de trabajo por día de la semana. " +
                           "Los eventos solo se podrán agendar dentro de estos horarios.")
    public ResponseEntity<Map<String, Object>> configureWorkingHours(
            @PathVariable UUID accountId,
            @RequestBody WorkingHoursRequest request
    ) {
        try {
            // Obtener configuración actual
            AvailabilityConfig currentConfig = configService.getAvailabilityConfig(
                    UuidId.of(accountId)
            );
            
            // Actualizar solo los working hours
            AvailabilityConfig updatedConfig = new AvailabilityConfig(
                    currentConfig.enabled(),
                    currentConfig.slotDurationMinutes(),
                    currentConfig.advanceBookingDays(),
                    request.workingHours(), // Nuevos horarios
                    currentConfig.holidays(),
                    currentConfig.blockedSlots()
            );
            
            configService.updateAvailabilityConfig(UuidId.of(accountId), updatedConfig);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Jornadas laborales configuradas exitosamente",
                    "data", updatedConfig.workingHours()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint para obtener solo las jornadas laborales
     * GET /api/calendar-provider-accounts/{accountId}/availability-config/working-hours
     */
    @GetMapping("/working-hours")
    @Operation(summary = "Obtener jornadas laborales configuradas")
    public ResponseEntity<Map<String, Object>> getWorkingHours(
            @PathVariable UUID accountId
    ) {
        try {
            AvailabilityConfig config = configService.getAvailabilityConfig(
                    UuidId.of(accountId)
            );
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", config.workingHours()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * DTO para configurar jornadas laborales
     */
    public record WorkingHoursRequest(
            Map<String, AvailabilityConfig.DaySchedule> workingHours
    ) {}
}

