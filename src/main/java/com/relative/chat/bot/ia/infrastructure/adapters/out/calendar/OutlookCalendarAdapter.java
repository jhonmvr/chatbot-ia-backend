package com.relative.chat.bot.ia.infrastructure.adapters.out.calendar;

import com.relative.chat.bot.ia.application.dto.*;
import com.relative.chat.bot.ia.application.ports.out.CalendarService;
import com.relative.chat.bot.ia.application.services.TokenRefreshService;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.domain.scheduling.exceptions.CalendarApiException;
import com.relative.chat.bot.ia.domain.scheduling.exceptions.CalendarAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador para Microsoft Graph Calendar API
 * Documentación: https://learn.microsoft.com/en-us/graph/api/resources/calendar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutlookCalendarAdapter implements CalendarService {
    
    private final WebClient.Builder webClientBuilder;
    private final TokenRefreshService tokenRefreshService;
    
    private static final String MICROSOFT_GRAPH_API_BASE = "https://graph.microsoft.com/v1.0";
    private static final int MAX_RETRIES = 2;
    
    @Override
    public CalendarEventResponse createEvent(CalendarProviderAccount account, CalendarEvent event) {
        log.info("Creando evento en Outlook Calendar: {}", event.summary());
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            Map<String, Object> requestBody = buildOutlookEventRequest(event);
            
            String endpoint = calendarId != null && !calendarId.equals("primary")
                    ? "/me/calendars/" + calendarId + "/events"
                    : "/me/events";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(endpoint)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "crear evento"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("id")) {
                throw new CalendarApiException("Error al crear evento en Outlook Calendar: respuesta inválida", 500);
            }
            
            return mapOutlookEventToResponse(response);
        });
    }
    
    @Override
    public CalendarEventResponse updateEvent(CalendarProviderAccount account, String eventId, CalendarEvent event) {
        log.info("Actualizando evento en Outlook Calendar: {}", eventId);
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            Map<String, Object> requestBody = buildOutlookEventRequest(event);
            
            String endpoint = calendarId != null && !calendarId.equals("primary")
                    ? "/me/calendars/" + calendarId + "/events/" + eventId
                    : "/me/events/" + eventId;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.patch()
                    .uri(endpoint)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "actualizar evento"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("id")) {
                throw new CalendarApiException("Error al actualizar evento en Outlook Calendar: respuesta inválida", 500);
            }
            
            return mapOutlookEventToResponse(response);
        });
    }
    
    @Override
    public void deleteEvent(CalendarProviderAccount account, String eventId) {
        log.info("Eliminando evento en Outlook Calendar: {}", eventId);
        
        executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            
            String endpoint = calendarId != null && !calendarId.equals("primary")
                    ? "/me/calendars/" + calendarId + "/events/" + eventId
                    : "/me/events/" + eventId;
            
            webClient.delete()
                    .uri(endpoint)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "eliminar evento"))
                    .toBodilessEntity()
                    .block();
            
            log.info("Evento eliminado exitosamente: {}", eventId);
            return null;
        });
    }
    
    @Override
    public CalendarEventResponse getEvent(CalendarProviderAccount account, String eventId) {
        log.info("Obteniendo evento de Outlook Calendar: {}", eventId);
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            
            String endpoint = calendarId != null && !calendarId.equals("primary")
                    ? "/me/calendars/" + calendarId + "/events/" + eventId
                    : "/me/events/" + eventId;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(endpoint)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "obtener evento"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null) {
                throw new CalendarApiException("Evento no encontrado en Outlook Calendar", 404);
            }
            
            return mapOutlookEventToResponse(response);
        });
    }
    
    @Override
    public List<CalendarEventResponse> listEvents(CalendarProviderAccount account, Instant startTime, Instant endTime) {
        log.info("Listando eventos de Outlook Calendar desde {} hasta {}", startTime, endTime);
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            
            String endpoint = calendarId != null && !calendarId.equals("primary")
                    ? "/me/calendars/" + calendarId + "/events"
                    : "/me/events";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("$filter", 
                                    String.format("start/dateTime ge '%s' and end/dateTime le '%s'", 
                                            formatOutlookDateTime(startTime), formatOutlookDateTime(endTime)))
                            .queryParam("$orderby", "start/dateTime")
                            .build())
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "listar eventos"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("value")) {
                return List.of();
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
            
            return items.stream()
                    .map(this::mapOutlookEventToResponse)
                    .toList();
        });
    }
    
    @Override
    public FreeBusyResponse getFreeBusy(CalendarProviderAccount account, FreeBusyQuery query) {
        log.info("Consultando disponibilidad en Outlook Calendar desde {} hasta {}", query.startTime(), query.endTime());
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("schedules", List.of(acc.accountEmail()));
            requestBody.put("startTime", Map.of(
                    "dateTime", formatOutlookDateTime(query.startTime()),
                    "timeZone", query.timeZone()
            ));
            requestBody.put("endTime", Map.of(
                    "dateTime", formatOutlookDateTime(query.endTime()),
                    "timeZone", query.timeZone()
            ));
            requestBody.put("availabilityViewInterval", 30); // 30 minutos
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/me/calendar/getSchedule")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "consultar disponibilidad"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("value")) {
                throw new CalendarApiException("Error al consultar disponibilidad en Outlook Calendar: respuesta inválida", 500);
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> schedules = (List<Map<String, Object>>) response.get("value");
            
            if (schedules.isEmpty()) {
                return new FreeBusyResponse(calendarId, query.startTime(), query.endTime(), List.of(), 
                        List.of(new FreeBusyResponse.TimeSlot(query.startTime(), query.endTime())));
            }
            
            Map<String, Object> schedule = schedules.get(0);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scheduleItems = (List<Map<String, Object>>) schedule.getOrDefault("scheduleItems", List.of());
            
            List<FreeBusyResponse.TimeSlot> busySlots = scheduleItems.stream()
                    .filter(item -> "busy".equals(item.get("status")))
                    .map(item -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> start = (Map<String, String>) item.get("start");
                        @SuppressWarnings("unchecked")
                        Map<String, String> end = (Map<String, String>) item.get("end");
                        return new FreeBusyResponse.TimeSlot(
                                parseOutlookDateTime(start.get("dateTime")),
                                parseOutlookDateTime(end.get("dateTime"))
                        );
                    })
                    .toList();
            
            List<FreeBusyResponse.TimeSlot> freeSlots = calculateFreeSlots(query.startTime(), query.endTime(), busySlots);
            
            return new FreeBusyResponse(
                    calendarId,
                    query.startTime(),
                    query.endTime(),
                    busySlots,
                    freeSlots
            );
        });
    }
    
    /**
     * Construye el request body para crear/actualizar evento en Outlook Calendar
     */
    private Map<String, Object> buildOutlookEventRequest(CalendarEvent event) {
        Map<String, Object> request = new HashMap<>();
        request.put("subject", event.summary());
        
        if (event.description() != null && !event.description().isBlank()) {
            Map<String, String> body = new HashMap<>();
            body.put("contentType", "HTML");
            body.put("content", event.description());
            request.put("body", body);
        }
        
        if (event.location() != null && !event.location().isBlank()) {
            Map<String, String> location = new HashMap<>();
            location.put("displayName", event.location());
            request.put("location", location);
        }
        
        // Start time
        Map<String, String> start = new HashMap<>();
        start.put("dateTime", formatOutlookDateTime(event.startTime()));
        start.put("timeZone", event.timeZone());
        request.put("start", start);
        
        // End time
        Map<String, String> end = new HashMap<>();
        end.put("dateTime", formatOutlookDateTime(event.endTime()));
        end.put("timeZone", event.timeZone());
        request.put("end", end);
        
        // Attendees
        if (event.attendeeEmails() != null && !event.attendeeEmails().isEmpty()) {
            List<Map<String, Object>> attendees = event.attendeeEmails().stream()
                    .map(email -> {
                        Map<String, Object> attendee = new HashMap<>();
                        Map<String, String> emailAddress = new HashMap<>();
                        emailAddress.put("address", email);
                        attendee.put("emailAddress", emailAddress);
                        attendee.put("type", "required");
                        return attendee;
                    })
                    .toList();
            request.put("attendees", attendees);
        }
        
        // Online meeting
        if (event.isOnlineMeeting() != null && event.isOnlineMeeting()) {
            request.put("isOnlineMeeting", true);
            if (event.onlineMeetingProvider() != null && !event.onlineMeetingProvider().isBlank()) {
                request.put("onlineMeetingProvider", event.onlineMeetingProvider());
            } else {
                request.put("onlineMeetingProvider", "teamsForBusiness");
            }
        }
        
        return request;
    }
    
    /**
     * Mapea la respuesta de Outlook Calendar a CalendarEventResponse
     */
    private CalendarEventResponse mapOutlookEventToResponse(Map<String, Object> outlookEvent) {
        String eventId = (String) outlookEvent.get("id");
        String subject = (String) outlookEvent.get("subject");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) outlookEvent.getOrDefault("body", Map.of());
        String description = (String) body.getOrDefault("content", "");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> location = (Map<String, Object>) outlookEvent.getOrDefault("location", Map.of());
        String locationName = (String) location.getOrDefault("displayName", "");
        
        @SuppressWarnings("unchecked")
        Map<String, String> start = (Map<String, String>) outlookEvent.get("start");
        @SuppressWarnings("unchecked")
        Map<String, String> end = (Map<String, String>) outlookEvent.get("end");
        
        Instant startTime = parseOutlookDateTime(start.get("dateTime"));
        Instant endTime = parseOutlookDateTime(end.get("dateTime"));
        String timeZone = start.getOrDefault("timeZone", "UTC");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> organizer = (Map<String, Object>) outlookEvent.getOrDefault("organizer", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, String> organizerEmailAddress = (Map<String, String>) organizer.getOrDefault("emailAddress", Map.of());
        String organizerEmail = organizerEmailAddress.getOrDefault("address", "");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attendees = (List<Map<String, Object>>) outlookEvent.getOrDefault("attendees", List.of());
        List<String> attendeeEmails = attendees.stream()
                .map(attendee -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> emailAddress = (Map<String, String>) attendee.get("emailAddress");
                    return emailAddress.get("address");
                })
                .filter(email -> email != null)
                .toList();
        
        String status = (String) outlookEvent.getOrDefault("showAs", "busy");
        String webLink = (String) outlookEvent.getOrDefault("webLink", "");
        Boolean isOnlineMeeting = (Boolean) outlookEvent.getOrDefault("isOnlineMeeting", false);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> onlineMeeting = (Map<String, Object>) outlookEvent.getOrDefault("onlineMeeting", Map.of());
        String onlineMeetingUrl = (String) onlineMeeting.getOrDefault("joinUrl", null);
        
        String createdDateTime = (String) outlookEvent.getOrDefault("createdDateTime", null);
        String lastModifiedDateTime = (String) outlookEvent.getOrDefault("lastModifiedDateTime", null);
        
        Instant createdAt = createdDateTime != null ? parseOutlookDateTime(createdDateTime) : null;
        Instant updatedAt = lastModifiedDateTime != null ? parseOutlookDateTime(lastModifiedDateTime) : null;
        
        return new CalendarEventResponse(
                eventId,
                subject,
                description,
                startTime,
                endTime,
                timeZone,
                locationName,
                attendeeEmails,
                organizerEmail,
                status,
                webLink,
                isOnlineMeeting,
                onlineMeetingUrl,
                createdAt,
                updatedAt
        );
    }
    
    /**
     * Formatea fecha/hora para Microsoft Graph API
     */
    private String formatOutlookDateTime(Instant instant) {
        return instant.atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    
    /**
     * Parsea fecha/hora de Microsoft Graph API
     */
    private Instant parseOutlookDateTime(String dateTime) {
        return Instant.parse(dateTime);
    }
    
    /**
     * Calcula slots libres entre los slots ocupados
     */
    private List<FreeBusyResponse.TimeSlot> calculateFreeSlots(
            Instant start, Instant end, List<FreeBusyResponse.TimeSlot> busySlots) {
        List<FreeBusyResponse.TimeSlot> freeSlots = new ArrayList<>();
        
        if (busySlots.isEmpty()) {
            freeSlots.add(new FreeBusyResponse.TimeSlot(start, end));
            return freeSlots;
        }
        
        // Ordenar slots ocupados por inicio
        List<FreeBusyResponse.TimeSlot> sortedBusy = new ArrayList<>(busySlots);
        sortedBusy.sort((a, b) -> a.start().compareTo(b.start()));
        
        // Primer slot libre (antes del primer ocupado)
        if (start.isBefore(sortedBusy.get(0).start())) {
            freeSlots.add(new FreeBusyResponse.TimeSlot(start, sortedBusy.get(0).start()));
        }
        
        // Slots libres entre ocupados
        for (int i = 0; i < sortedBusy.size() - 1; i++) {
            Instant freeStart = sortedBusy.get(i).end();
            Instant freeEnd = sortedBusy.get(i + 1).start();
            if (freeStart.isBefore(freeEnd)) {
                freeSlots.add(new FreeBusyResponse.TimeSlot(freeStart, freeEnd));
            }
        }
        
        // Último slot libre (después del último ocupado)
        FreeBusyResponse.TimeSlot lastBusy = sortedBusy.get(sortedBusy.size() - 1);
        if (lastBusy.end().isBefore(end)) {
            freeSlots.add(new FreeBusyResponse.TimeSlot(lastBusy.end(), end));
        }
        
        return freeSlots;
    }
    
    /**
     * Obtiene el calendar ID de la configuración o usa null para calendario principal
     */
    private String getCalendarId(CalendarProviderAccount account) {
        if (account.config() != null && account.config().containsKey("calendar_id")) {
            return (String) account.config().get("calendar_id");
        }
        return null; // null significa calendario principal en Outlook
    }
    
    /**
     * Asegura que el token sea válido, renovándolo si es necesario
     */
    private String ensureValidToken(CalendarProviderAccount account) {
        CalendarProviderAccount refreshedAccount = tokenRefreshService.refreshTokenIfNeeded(account);
        return refreshedAccount.accessToken();
    }
    
    /**
     * Ejecuta una operación con reintentos automáticos en caso de errores de autenticación
     */
    private <T> T executeWithRetry(CalendarProviderAccount account, CalendarOperation<T> operation) {
        int attempts = 0;
        CalendarProviderAccount currentAccount = account;
        
        while (attempts <= MAX_RETRIES) {
            try {
                return operation.execute(currentAccount);
            } catch (CalendarAuthenticationException e) {
                attempts++;
                if (attempts > MAX_RETRIES) {
                    log.error("Error de autenticación después de {} intentos: {}", attempts, e.getMessage());
                    throw e;
                }
                
                log.warn("Error de autenticación (intento {}/{}), renovando token...", attempts, MAX_RETRIES);
                try {
                    currentAccount = tokenRefreshService.refreshTokenIfNeeded(currentAccount);
                } catch (Exception refreshError) {
                    log.error("Error al renovar token: {}", refreshError.getMessage());
                    throw new CalendarAuthenticationException(
                            "No se pudo renovar el token después de error de autenticación", refreshError
                    );
                }
            } catch (CalendarApiException e) {
                // Errores de API no se reintentan (excepto 401/403 que se manejan arriba)
                throw e;
            } catch (Exception e) {
                // Otros errores se convierten en CalendarApiException
                log.error("Error inesperado en operación de calendario: {}", e.getMessage(), e);
                throw new CalendarApiException("Error en operación de calendario: " + e.getMessage(), 500, e);
            }
        }
        
        throw new CalendarApiException("Error después de múltiples intentos", 500);
    }
    
    /**
     * Maneja respuestas de error HTTP
     */
    private reactor.core.publisher.Mono<? extends Throwable> handleErrorResponse(
            org.springframework.web.reactive.function.client.ClientResponse response, String operation) {
        return response.bodyToMono(Map.class)
                .map(errorBody -> {
                    int statusCode = response.statusCode().value();
                    String errorMessage = extractErrorMessage(errorBody, operation);
                    String errorCode = extractErrorCode(errorBody);
                    
                    if (statusCode == 401 || statusCode == 403) {
                        return new CalendarAuthenticationException(
                                String.format("Error de autenticación al %s: %s", operation, errorMessage)
                        );
                    }
                    
                    return new CalendarApiException(errorMessage, statusCode, errorCode);
                })
                .defaultIfEmpty(new CalendarApiException(
                        String.format("Error HTTP %d al %s", response.statusCode().value(), operation),
                        response.statusCode().value()
                ));
    }
    
    /**
     * Extrae el mensaje de error del cuerpo de respuesta
     */
    @SuppressWarnings("unchecked")
    private String extractErrorMessage(Map<String, Object> errorBody, String operation) {
        if (errorBody == null) {
            return "Error desconocido al " + operation;
        }
        
        // Microsoft Graph API estructura de error
        if (errorBody.containsKey("error")) {
            Object error = errorBody.get("error");
            if (error instanceof Map) {
                Map<String, Object> errorMap = (Map<String, Object>) error;
                String message = (String) errorMap.getOrDefault("message", "Error desconocido");
                return String.format("Error al %s: %s", operation, message);
            }
        }
        
        return (String) errorBody.getOrDefault("message", "Error desconocido al " + operation);
    }
    
    /**
     * Extrae el código de error del cuerpo de respuesta
     */
    @SuppressWarnings("unchecked")
    private String extractErrorCode(Map<String, Object> errorBody) {
        if (errorBody == null) {
            return null;
        }
        
        if (errorBody.containsKey("error")) {
            Object error = errorBody.get("error");
            if (error instanceof Map) {
                Map<String, Object> errorMap = (Map<String, Object>) error;
                return (String) errorMap.get("code");
            }
        }
        
        return null;
    }
    
    /**
     * Interfaz funcional para operaciones de calendario
     */
    @FunctionalInterface
    private interface CalendarOperation<T> {
        T execute(CalendarProviderAccount account) throws Exception;
    }
    
    /**
     * Crea un WebClient configurado para Microsoft Graph API
     */
    private WebClient createWebClient(String accessToken) {
        return webClientBuilder
                .baseUrl(MICROSOFT_GRAPH_API_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

