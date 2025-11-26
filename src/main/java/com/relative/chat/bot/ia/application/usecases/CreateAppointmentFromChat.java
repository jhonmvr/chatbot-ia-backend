package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.dto.CalendarEvent;
import com.relative.chat.bot.ia.application.dto.CalendarEventResponse;
import com.relative.chat.bot.ia.application.ports.out.CalendarService;
import com.relative.chat.bot.ia.application.services.AppointmentAvailabilityService;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.domain.ports.scheduling.CalendarProviderAccountRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.calendar.CalendarServiceRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Caso de uso: Crear appointment desde el chat
 * Crea un evento en el calendario y retorna información amigable
 * Valida que el horario esté dentro de las jornadas laborales configuradas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAppointmentFromChat {
    
    private final CalendarServiceRouter calendarServiceRouter;
    private final CalendarProviderAccountRepository accountRepository;
    private final AppointmentAvailabilityService availabilityService;
    
    /**
     * Crea un appointment desde el chat
     * 
     * @param clientId ID del cliente
     * @param contactId ID del contacto
     * @param dateTime Fecha y hora del appointment
     * @param description Descripción opcional
     * @return Respuesta con información del appointment creado
     */
    public AppointmentResponse handle(
            UuidId<Client> clientId,
            UuidId<Contact> contactId,
            LocalDateTime dateTime,
            String description
    ) {
        try {
            // 1. Obtener cuenta de calendario activa del cliente
            Optional<CalendarProviderAccount> accountOpt = accountRepository
                    .findActiveByClientIdAndProvider(clientId, com.relative.chat.bot.ia.domain.scheduling.CalendarProvider.GOOGLE);
            
            if (accountOpt.isEmpty()) {
                // Intentar con Outlook
                accountOpt = accountRepository.findActiveByClientIdAndProvider(
                        clientId, 
                        com.relative.chat.bot.ia.domain.scheduling.CalendarProvider.OUTLOOK
                );
            }
            
            if (accountOpt.isEmpty()) {
                throw new IllegalArgumentException("No hay cuenta de calendario configurada para este cliente");
            }
            
            CalendarProviderAccount account = accountOpt.get();
            
            // 2. Validar que el horario esté disponible y dentro de jornadas laborales
            if (!availabilityService.isSlotAvailable(account, dateTime)) {
                throw new IllegalArgumentException(
                        "El horario seleccionado no está disponible o está fuera de las jornadas laborales configuradas"
                );
            }
            
            // 3. Obtener servicio de calendario
            CalendarService calendarService = calendarServiceRouter.getCalendarService(account.provider());
            
            // 4. Obtener timezone
            String timezone = account.config() != null && account.config().containsKey("timezone")
                    ? account.config().get("timezone").toString()
                    : "America/Guayaquil";
            
            ZoneId zoneId = ZoneId.of(timezone);
            Instant startInstant = dateTime.atZone(zoneId).toInstant();
            Instant endInstant = dateTime.plusMinutes(30).atZone(zoneId).toInstant(); // Duración por defecto: 30 min
            
            // 5. Crear evento en el calendario
            CalendarEvent event = new CalendarEvent(
                    description != null ? description : "Cita agendada desde WhatsApp",
                    description,
                    startInstant,
                    endInstant,
                    timezone,
                    null, // location
                    List.of(), // attendeeEmails
                    false, // isOnlineMeeting
                    null // onlineMeetingProvider
            );
            
            CalendarEventResponse response = calendarService.createEvent(account, event);
            
            log.info("Appointment creado exitosamente: eventId={}, fecha={}", 
                    response.eventId(), dateTime);
            
            return new AppointmentResponse(
                    response.eventId(),
                    dateTime,
                    response.htmlLink(),
                    response.summary()
            );
            
        } catch (Exception e) {
            log.error("Error al crear appointment: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear appointment: " + e.getMessage(), e);
        }
    }
    
    public record AppointmentResponse(
            String eventId,
            LocalDateTime dateTime,
            String calendarLink,
            String summary
    ) {}
}

