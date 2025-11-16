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
 * Adaptador para Google Calendar API v3
 * Documentación: https://developers.google.com/workspace/calendar/api/v3/reference
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements CalendarService {
    
    private final WebClient.Builder webClientBuilder;
    private final TokenRefreshService tokenRefreshService;
    
    private static final String GOOGLE_CALENDAR_API_BASE = "https://www.googleapis.com/calendar/v3";
    private static final int MAX_RETRIES = 2;
    
    @Override
    public CalendarEventResponse createEvent(CalendarProviderAccount account, CalendarEvent event) {
        log.info("Creando evento en Google Calendar: {}", event.summary());
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            Map<String, Object> requestBody = buildGoogleEventRequest(event);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/calendars/{calendarId}/events", calendarId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                            clientResponse -> handleErrorResponse(clientResponse, "crear evento"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("id")) {
                throw new CalendarApiException("Error al crear evento en Google Calendar: respuesta inválida", 500);
            }
            
            return mapGoogleEventToResponse(response);
        });
    }
    
    @Override
    public CalendarEventResponse updateEvent(CalendarProviderAccount account, String eventId, CalendarEvent event) {
        log.info("Actualizando evento en Google Calendar: {}", eventId);
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            Map<String, Object> requestBody = buildGoogleEventRequest(event);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.put()
                    .uri("/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "actualizar evento"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("id")) {
                throw new CalendarApiException("Error al actualizar evento en Google Calendar: respuesta inválida", 500);
            }
            
            return mapGoogleEventToResponse(response);
        });
    }
    
    @Override
    public void deleteEvent(CalendarProviderAccount account, String eventId) {
        log.info("Eliminando evento en Google Calendar: {}", eventId);
        
        executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            
            webClient.delete()
                    .uri("/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
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
        log.info("Obteniendo evento de Google Calendar: {}", eventId);
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri("/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "obtener evento"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null) {
                throw new CalendarApiException("Evento no encontrado en Google Calendar", 404);
            }
            
            return mapGoogleEventToResponse(response);
        });
    }
    
    @Override
    public List<CalendarEventResponse> listEvents(CalendarProviderAccount account, Instant startTime, Instant endTime) {
        log.info("Listando eventos de Google Calendar desde {} hasta {}", startTime, endTime);
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/calendars/{calendarId}/events")
                            .queryParam("timeMin", startTime.toString())
                            .queryParam("timeMax", endTime.toString())
                            .queryParam("singleEvents", "true")
                            .queryParam("orderBy", "startTime")
                            .build(calendarId))
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "listar eventos"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("items")) {
                return List.of();
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            
            return items.stream()
                    .map(this::mapGoogleEventToResponse)
                    .toList();
        });
    }
    
    @Override
    public FreeBusyResponse getFreeBusy(CalendarProviderAccount account, FreeBusyQuery query) {
        log.info("Consultando disponibilidad en Google Calendar desde {} hasta {}", query.startTime(), query.endTime());
        
        return executeWithRetry(account, acc -> {
            String accessToken = ensureValidToken(acc);
            String calendarId = getCalendarId(acc);
            WebClient webClient = createWebClient(accessToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("timeMin", query.startTime().toString());
            requestBody.put("timeMax", query.endTime().toString());
            requestBody.put("items", List.of(Map.of("id", calendarId)));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/freeBusy")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> handleErrorResponse(clientResponse, "consultar disponibilidad"))
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("calendars")) {
                throw new CalendarApiException("Error al consultar disponibilidad en Google Calendar: respuesta inválida", 500);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> calendars = (Map<String, Object>) response.get("calendars");
            @SuppressWarnings("unchecked")
            Map<String, Object> calendarData = (Map<String, Object>) calendars.get(calendarId);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> busyList = (List<Map<String, Object>>) calendarData.getOrDefault("busy", List.of());
            
            List<FreeBusyResponse.TimeSlot> busySlots = busyList.stream()
                    .map(busy -> new FreeBusyResponse.TimeSlot(
                            Instant.parse((String) busy.get("start")),
                            Instant.parse((String) busy.get("end"))
                    ))
                    .toList();
            
            // Calcular slots libres (simplificado)
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
     * Construye el request body para crear/actualizar evento en Google Calendar
     */
    private Map<String, Object> buildGoogleEventRequest(CalendarEvent event) {
        Map<String, Object> request = new HashMap<>();
        request.put("summary", event.summary());
        
        if (event.description() != null && !event.description().isBlank()) {
            request.put("description", event.description());
        }
        
        if (event.location() != null && !event.location().isBlank()) {
            request.put("location", event.location());
        }
        
        // Start time
        Map<String, String> start = new HashMap<>();
        start.put("dateTime", formatGoogleDateTime(event.startTime(), event.timeZone()));
        start.put("timeZone", event.timeZone());
        request.put("start", start);
        
        // End time
        Map<String, String> end = new HashMap<>();
        end.put("dateTime", formatGoogleDateTime(event.endTime(), event.timeZone()));
        end.put("timeZone", event.timeZone());
        request.put("end", end);
        
        // Attendees
        if (event.attendeeEmails() != null && !event.attendeeEmails().isEmpty()) {
            List<Map<String, String>> attendees = event.attendeeEmails().stream()
                    .map(email -> Map.<String, String>of("email", email))
                    .toList();
            request.put("attendees", attendees);
        }
        
        return request;
    }
    
    /**
     * Mapea la respuesta de Google Calendar a CalendarEventResponse
     */
    private CalendarEventResponse mapGoogleEventToResponse(Map<String, Object> googleEvent) {
        String eventId = (String) googleEvent.get("id");
        String summary = (String) googleEvent.get("summary");
        String description = (String) googleEvent.getOrDefault("description", "");
        String location = (String) googleEvent.getOrDefault("location", "");
        String htmlLink = (String) googleEvent.getOrDefault("htmlLink", "");
        String status = (String) googleEvent.getOrDefault("status", "confirmed");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> start = (Map<String, Object>) googleEvent.get("start");
        @SuppressWarnings("unchecked")
        Map<String, Object> end = (Map<String, Object>) googleEvent.get("end");
        
        Instant startTime = parseGoogleDateTime((String) start.get("dateTime"));
        Instant endTime = parseGoogleDateTime((String) end.get("dateTime"));
        String timeZone = (String) start.getOrDefault("timeZone", "UTC");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> organizer = (Map<String, Object>) googleEvent.getOrDefault("organizer", Map.of());
        String organizerEmail = (String) organizer.getOrDefault("email", "");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attendees = (List<Map<String, Object>>) googleEvent.getOrDefault("attendees", List.of());
        List<String> attendeeEmails = attendees.stream()
                .map(attendee -> (String) attendee.get("email"))
                .toList();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> created = (Map<String, Object>) googleEvent.getOrDefault("created", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> updated = (Map<String, Object>) googleEvent.getOrDefault("updated", Map.of());
        
        Instant createdAt = created.containsKey("dateTime") 
                ? parseGoogleDateTime((String) created.get("dateTime"))
                : null;
        Instant updatedAt = updated.containsKey("dateTime")
                ? parseGoogleDateTime((String) updated.get("dateTime"))
                : null;
        
        return new CalendarEventResponse(
                eventId,
                summary,
                description,
                startTime,
                endTime,
                timeZone,
                location,
                attendeeEmails,
                organizerEmail,
                status,
                htmlLink,
                false, // Google Calendar no tiene isOnlineMeeting directo
                null, // onlineMeetingUrl
                createdAt,
                updatedAt
        );
    }
    
    /**
     * Formatea fecha/hora para Google Calendar API
     */
    private String formatGoogleDateTime(Instant instant, String timeZone) {
        return instant.atZone(ZoneId.of(timeZone))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    
    /**
     * Parsea fecha/hora de Google Calendar API
     */
    private Instant parseGoogleDateTime(String dateTime) {
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
     * Obtiene el calendar ID de la configuración o usa "primary" por defecto
     */
    private String getCalendarId(CalendarProviderAccount account) {
        if (account.config() != null && account.config().containsKey("calendar_id")) {
            return (String) account.config().get("calendar_id");
        }
        return "primary";
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
        
        // Google Calendar API estructura de error
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
     * Crea un WebClient configurado para Google Calendar API
     */
    private WebClient createWebClient(String accessToken) {
        return webClientBuilder
                .baseUrl(GOOGLE_CALENDAR_API_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

