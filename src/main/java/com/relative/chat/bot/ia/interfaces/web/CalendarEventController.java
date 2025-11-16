package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.dto.*;
import com.relative.chat.bot.ia.application.ports.out.CalendarService;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.domain.scheduling.exceptions.CalendarApiException;
import com.relative.chat.bot.ia.domain.scheduling.exceptions.CalendarAuthenticationException;
import com.relative.chat.bot.ia.domain.ports.scheduling.CalendarProviderAccountRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.calendar.CalendarServiceRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * API REST para gestión de eventos de calendario
 * Permite crear, actualizar, eliminar y consultar eventos en Google Calendar y Outlook
 */
@Slf4j
@RestController
@RequestMapping("/api/calendar-events")
@RequiredArgsConstructor
@Tag(name = "Calendar Events", description = "API para gestionar eventos de calendario (Google Calendar y Outlook)")
public class CalendarEventController {
    
    private final CalendarServiceRouter calendarServiceRouter;
    private final CalendarProviderAccountRepository accountRepository;
    
    /**
     * Crea un nuevo evento de calendario
     * POST /api/calendar-events?accountId={accountId}
     */
    @Operation(
        summary = "Crear evento de calendario",
        description = "Crea un nuevo evento en el calendario configurado. Requiere el ID de la cuenta de calendario."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Evento creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "data": {
                        "eventId": "abc123xyz",
                        "summary": "Reunión con cliente",
                        "description": "Discutir proyecto",
                        "startTime": "2025-10-15T10:00:00Z",
                        "endTime": "2025-10-15T11:00:00Z",
                        "timeZone": "America/Guayaquil",
                        "location": "Oficina principal",
                        "organizerEmail": "usuario@gmail.com",
                        "attendeeEmails": ["cliente@example.com"],
                        "htmlLink": "https://calendar.google.com/calendar/event?eid=...",
                        "createdAt": "2025-10-03T10:30:00Z",
                        "updatedAt": "2025-10-03T10:30:00Z"
                      },
                      "message": "Evento creado exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o error al crear evento",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "summary es requerido"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cuenta de calendario no encontrada"),
        @ApiResponse(responseCode = "401", description = "Error de autenticación con el proveedor de calendario")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createEvent(
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @RequestParam String accountId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del evento a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "summary": "Reunión con cliente",
                      "description": "Discutir proyecto",
                      "startTime": "2025-10-15T10:00:00Z",
                      "endTime": "2025-10-15T11:00:00Z",
                      "timeZone": "America/Guayaquil",
                      "location": "Oficina principal",
                      "attendeeEmails": ["cliente@example.com"],
                      "isOnlineMeeting": false
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            CalendarEvent event = parseCalendarEvent(request);
            CalendarProviderAccount account = getAccount(accountId);
            CalendarService calendarService = calendarServiceRouter.getCalendarService(account.provider());
            
            CalendarEventResponse response = calendarService.createEvent(account, event);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toMap(response),
                "message", "Evento creado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarAuthenticationException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarApiException e) {
            log.error("Error de API: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", "error",
                "message", e.getMessage(),
                "errorCode", e.getErrorCode() != null ? e.getErrorCode() : ""
            ));
        } catch (Exception e) {
            log.error("Error al crear evento: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error al crear evento: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Actualiza un evento existente
     * PUT /api/calendar-events/{eventId}?accountId={accountId}
     */
    @Operation(
        summary = "Actualizar evento de calendario",
        description = "Actualiza un evento existente en el calendario. Requiere el ID del evento y el ID de la cuenta de calendario."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Evento actualizado exitosamente"
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Evento o cuenta no encontrada"),
        @ApiResponse(responseCode = "401", description = "Error de autenticación")
    })
    @PutMapping(value = "/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateEvent(
        @Parameter(description = "ID del evento en el calendario externo", required = true)
        @PathVariable String eventId,
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @RequestParam String accountId,
        @RequestBody Map<String, Object> request
    ) {
        try {
            CalendarEvent event = parseCalendarEvent(request);
            CalendarProviderAccount account = getAccount(accountId);
            CalendarService calendarService = calendarServiceRouter.getCalendarService(account.provider());
            
            CalendarEventResponse response = calendarService.updateEvent(account, eventId, event);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toMap(response),
                "message", "Evento actualizado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarAuthenticationException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarApiException e) {
            log.error("Error de API: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al actualizar evento: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error al actualizar evento: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Elimina un evento
     * DELETE /api/calendar-events/{eventId}?accountId={accountId}
     */
    @Operation(
        summary = "Eliminar evento de calendario",
        description = "Elimina un evento del calendario. Requiere el ID del evento y el ID de la cuenta de calendario."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Evento eliminado exitosamente"
        ),
        @ApiResponse(responseCode = "404", description = "Evento o cuenta no encontrada"),
        @ApiResponse(responseCode = "401", description = "Error de autenticación")
    })
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Map<String, Object>> deleteEvent(
        @Parameter(description = "ID del evento en el calendario externo", required = true)
        @PathVariable String eventId,
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @RequestParam String accountId
    ) {
        try {
            CalendarProviderAccount account = getAccount(accountId);
            CalendarService calendarService = calendarServiceRouter.getCalendarService(account.provider());
            
            calendarService.deleteEvent(account, eventId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Evento eliminado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (CalendarAuthenticationException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarApiException e) {
            log.error("Error de API: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al eliminar evento: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error al eliminar evento: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtiene un evento por ID
     * GET /api/calendar-events/{eventId}?accountId={accountId}
     */
    @Operation(
        summary = "Obtener evento de calendario",
        description = "Obtiene los detalles de un evento específico del calendario."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Evento encontrado"
        ),
        @ApiResponse(responseCode = "404", description = "Evento o cuenta no encontrada"),
        @ApiResponse(responseCode = "401", description = "Error de autenticación")
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<Map<String, Object>> getEvent(
        @Parameter(description = "ID del evento en el calendario externo", required = true)
        @PathVariable String eventId,
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @RequestParam String accountId
    ) {
        try {
            CalendarProviderAccount account = getAccount(accountId);
            CalendarService calendarService = calendarServiceRouter.getCalendarService(account.provider());
            
            CalendarEventResponse response = calendarService.getEvent(account, eventId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toMap(response)
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (CalendarAuthenticationException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarApiException e) {
            log.error("Error de API: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener evento: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error al obtener evento: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Lista eventos en un rango de tiempo
     * GET /api/calendar-events?accountId={accountId}&startTime={startTime}&endTime={endTime}
     */
    @Operation(
        summary = "Listar eventos de calendario",
        description = "Obtiene una lista de eventos en un rango de tiempo específico."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de eventos obtenida correctamente"
        ),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
        @ApiResponse(responseCode = "401", description = "Error de autenticación")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> listEvents(
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @RequestParam String accountId,
        @Parameter(description = "Fecha y hora de inicio (ISO 8601)", required = true)
        @RequestParam String startTime,
        @Parameter(description = "Fecha y hora de fin (ISO 8601)", required = true)
        @RequestParam String endTime
    ) {
        try {
            Instant start = Instant.parse(startTime);
            Instant end = Instant.parse(endTime);
            
            CalendarProviderAccount account = getAccount(accountId);
            CalendarService calendarService = calendarServiceRouter.getCalendarService(account.provider());
            
            List<CalendarEventResponse> events = calendarService.listEvents(account, start, end);
            
            List<Map<String, Object>> eventsList = events.stream()
                    .map(this::toMap)
                    .toList();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", eventsList,
                "count", eventsList.size()
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarAuthenticationException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarApiException e) {
            log.error("Error de API: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al listar eventos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error al listar eventos: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Consulta disponibilidad (Free/Busy)
     * POST /api/calendar-events/free-busy?accountId={accountId}
     */
    @Operation(
        summary = "Consultar disponibilidad de calendario",
        description = "Consulta los slots libres y ocupados en un rango de tiempo específico."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Disponibilidad consultada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "data": {
                        "calendarId": "primary",
                        "startTime": "2025-10-15T00:00:00Z",
                        "endTime": "2025-10-15T23:59:59Z",
                        "busySlots": [
                          {
                            "start": "2025-10-15T10:00:00Z",
                            "end": "2025-10-15T11:00:00Z"
                          }
                        ],
                        "freeSlots": [
                          {
                            "start": "2025-10-15T00:00:00Z",
                            "end": "2025-10-15T10:00:00Z"
                          },
                          {
                            "start": "2025-10-15T11:00:00Z",
                            "end": "2025-10-15T23:59:59Z"
                          }
                        ]
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
        @ApiResponse(responseCode = "401", description = "Error de autenticación")
    })
    @PostMapping(value = "/free-busy", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getFreeBusy(
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @RequestParam String accountId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Consulta de disponibilidad",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "startTime": "2025-10-15T00:00:00Z",
                      "endTime": "2025-10-15T23:59:59Z",
                      "timeZone": "America/Guayaquil"
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            FreeBusyQuery query = parseFreeBusyQuery(request);
            CalendarProviderAccount account = getAccount(accountId);
            CalendarService calendarService = calendarServiceRouter.getCalendarService(account.provider());
            
            FreeBusyResponse response = calendarService.getFreeBusy(account, query);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toMap(response)
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarAuthenticationException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (CalendarApiException e) {
            log.error("Error de API: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al consultar disponibilidad: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error al consultar disponibilidad: " + e.getMessage()
            ));
        }
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Obtiene la cuenta de calendario por ID
     */
    private CalendarProviderAccount getAccount(String accountId) {
        UuidId<CalendarProviderAccount> id = UuidId.of(java.util.UUID.fromString(accountId));
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de calendario no encontrada: " + accountId));
    }
    
    /**
     * Parsea un Map a CalendarEvent
     */
    private CalendarEvent parseCalendarEvent(Map<String, Object> request) {
        String summary = (String) request.get("summary");
        String description = (String) request.get("description");
        String location = (String) request.get("location");
        String timeZone = (String) request.getOrDefault("timeZone", "America/Guayaquil");
        Boolean isOnlineMeeting = request.get("isOnlineMeeting") != null 
                ? Boolean.valueOf(request.get("isOnlineMeeting").toString()) 
                : false;
        String onlineMeetingProvider = (String) request.get("onlineMeetingProvider");
        
        Instant startTime = null;
        if (request.get("startTime") != null) {
            String startTimeStr = request.get("startTime").toString();
            startTime = Instant.parse(startTimeStr);
        }
        
        Instant endTime = null;
        if (request.get("endTime") != null) {
            String endTimeStr = request.get("endTime").toString();
            endTime = Instant.parse(endTimeStr);
        }
        
        @SuppressWarnings("unchecked")
        List<String> attendeeEmails = request.get("attendeeEmails") != null
                ? (List<String>) request.get("attendeeEmails")
                : List.of();
        
        return new CalendarEvent(
                summary,
                description,
                startTime,
                endTime,
                timeZone,
                location,
                attendeeEmails,
                isOnlineMeeting,
                onlineMeetingProvider
        );
    }
    
    /**
     * Parsea un Map a FreeBusyQuery
     */
    private FreeBusyQuery parseFreeBusyQuery(Map<String, Object> request) {
        Instant startTime = null;
        if (request.get("startTime") != null) {
            String startTimeStr = request.get("startTime").toString();
            startTime = Instant.parse(startTimeStr);
        }
        
        Instant endTime = null;
        if (request.get("endTime") != null) {
            String endTimeStr = request.get("endTime").toString();
            endTime = Instant.parse(endTimeStr);
        }
        
        String timeZone = (String) request.getOrDefault("timeZone", "America/Guayaquil");
        
        return new FreeBusyQuery(startTime, endTime, timeZone);
    }
    
    /**
     * Convierte CalendarEventResponse a Map
     */
    private Map<String, Object> toMap(CalendarEventResponse response) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("eventId", response.eventId());
        map.put("summary", response.summary() != null ? response.summary() : "");
        map.put("description", response.description() != null ? response.description() : "");
        map.put("startTime", response.startTime().toString());
        map.put("endTime", response.endTime().toString());
        map.put("timeZone", response.timeZone() != null ? response.timeZone() : "");
        map.put("location", response.location() != null ? response.location() : "");
        map.put("organizerEmail", response.organizerEmail() != null ? response.organizerEmail() : "");
        map.put("attendeeEmails", response.attendeeEmails() != null ? response.attendeeEmails() : List.of());
        map.put("htmlLink", response.htmlLink() != null ? response.htmlLink() : "");
        map.put("createdAt", response.createdAt() != null ? response.createdAt().toString() : "");
        map.put("updatedAt", response.updatedAt() != null ? response.updatedAt().toString() : "");
        return map;
    }
    
    /**
     * Convierte FreeBusyResponse a Map
     */
    private Map<String, Object> toMap(FreeBusyResponse response) {
        List<Map<String, String>> busySlots = response.busySlots().stream()
                .map(slot -> Map.of(
                        "start", slot.start().toString(),
                        "end", slot.end().toString()
                ))
                .toList();
        
        List<Map<String, String>> freeSlots = response.freeSlots().stream()
                .map(slot -> Map.of(
                        "start", slot.start().toString(),
                        "end", slot.end().toString()
                ))
                .toList();
        
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("calendarId", response.calendarId() != null ? response.calendarId() : "");
        map.put("startTime", response.startTime().toString());
        map.put("endTime", response.endTime().toString());
        map.put("busySlots", busySlots);
        map.put("freeSlots", freeSlots);
        return map;
    }
}

