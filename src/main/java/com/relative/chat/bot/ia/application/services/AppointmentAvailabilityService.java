package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.application.dto.AvailabilityConfig;
import com.relative.chat.bot.ia.application.dto.FreeBusyQuery;
import com.relative.chat.bot.ia.application.dto.FreeBusyResponse;
import com.relative.chat.bot.ia.application.ports.out.CalendarService;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.infrastructure.adapters.out.calendar.CalendarServiceRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentAvailabilityService {
    
    private final AvailabilityConfigService configService;
    private final CalendarServiceRouter calendarServiceRouter;
    
    /**
     * Obtiene slots disponibles para una fecha específica
     */
    public List<TimeSlot> getAvailableSlots(
            CalendarProviderAccount account,
            LocalDate date
    ) {
        AvailabilityConfig config = configService.getAvailabilityConfig(account.id());
        
        if (!config.enabled()) {
            log.warn("Disponibilidad deshabilitada para cuenta: {}", account.id().value());
            return List.of();
        }
        
        // 1. Verificar si el día está habilitado
        String dayOfWeek = date.getDayOfWeek().name().toLowerCase();
        AvailabilityConfig.DaySchedule daySchedule = config.workingHours().get(dayOfWeek);
        
        if (daySchedule == null || !daySchedule.enabled()) {
            return List.of(); // Día no disponible
        }
        
        // 2. Verificar si es feriado
        if (isHoliday(date, config.holidays())) {
            return List.of();
        }
        
        // 3. Generar slots teóricos basados en horarios de trabajo
        String timezone = account.config() != null && account.config().containsKey("timezone")
                ? account.config().get("timezone").toString()
                : "America/Guayaquil";
        
        List<TimeSlot> theoreticalSlots = generateTheoreticalSlots(
                date,
                daySchedule,
                config.slotDurationMinutes(),
                timezone
        );
        
        // 4. Consultar calendario externo para obtener slots ocupados
        List<TimeSlot> busySlots = getBusySlotsFromCalendar(account, date, timezone);
        
        // 5. Filtrar slots ocupados y bloqueados
        List<TimeSlot> availableSlots = filterAvailableSlots(
                theoreticalSlots,
                busySlots,
                config.blockedSlots(),
                date
        );
        
        return availableSlots;
    }
    
    /**
     * Verifica si un slot específico está disponible
     */
    public boolean isSlotAvailable(
            CalendarProviderAccount account,
            LocalDateTime dateTime
    ) {
        LocalDate date = dateTime.toLocalDate();
        List<TimeSlot> availableSlots = getAvailableSlots(account, date);
        
        return availableSlots.stream()
                .anyMatch(slot -> slot.startTime().equals(dateTime));
    }
    
    /**
     * Formatea slots para mostrar en WhatsApp
     */
    public String formatSlotsForWhatsApp(List<TimeSlot> slots) {
        if (slots.isEmpty()) {
            return "❌ No hay horarios disponibles para esta fecha. " +
                   "Por favor, elige otro día.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("✅ Horarios disponibles:\n\n");
        
        // Agrupar por hora
        slots.stream()
                .map(slot -> slot.startTime().toLocalTime())
                .distinct()
                .sorted()
                .forEach(time -> {
                    String timeStr = time.format(DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH));
                    sb.append("• ").append(timeStr).append("\n");
                });
        
        sb.append("\nResponde con la hora que prefieras.");
        
        return sb.toString();
    }
    
    /**
     * Genera slots teóricos basados en horarios de trabajo
     */
    private List<TimeSlot> generateTheoreticalSlots(
            LocalDate date,
            AvailabilityConfig.DaySchedule daySchedule,
            int slotDurationMinutes,
            String timezone
    ) {
        List<TimeSlot> slots = new ArrayList<>();
        
        if (daySchedule.startTime() == null || daySchedule.endTime() == null) {
            return List.of();
        }
        
        LocalTime start = LocalTime.parse(daySchedule.startTime());
        LocalTime end = LocalTime.parse(daySchedule.endTime());
        
        LocalDateTime current = date.atTime(start);
        LocalDateTime endDateTime = date.atTime(end);
        
        while (current.isBefore(endDateTime)) {
            LocalDateTime slotEnd = current.plusMinutes(slotDurationMinutes);
            
            // Verificar si el slot está en un break
            if (!isInBreak(current.toLocalTime(), daySchedule.breaks())) {
                slots.add(new TimeSlot(current, slotEnd));
            }
            
            current = slotEnd;
        }
        
        return slots;
    }
    
    /**
     * Obtiene slots ocupados del calendario externo
     */
    private List<TimeSlot> getBusySlotsFromCalendar(
            CalendarProviderAccount account,
            LocalDate date,
            String timezone
    ) {
        try {
            CalendarService calendarService = calendarServiceRouter.getCalendarService(account.provider());
            
            ZoneId zoneId = ZoneId.of(timezone);
            
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            Instant startInstant = startOfDay.atZone(zoneId).toInstant();
            Instant endInstant = endOfDay.atZone(zoneId).toInstant();
            
            FreeBusyQuery query = new FreeBusyQuery(startInstant, endInstant, timezone);
            
            FreeBusyResponse response = calendarService.getFreeBusy(account, query);
            
            return response.busySlots().stream()
                    .map(busy -> {
                        LocalDateTime start = busy.start().atZone(ZoneId.of("UTC"))
                                .withZoneSameInstant(zoneId).toLocalDateTime();
                        LocalDateTime end = busy.end().atZone(ZoneId.of("UTC"))
                                .withZoneSameInstant(zoneId).toLocalDateTime();
                        return new TimeSlot(start, end);
                    })
                    .toList();
            
        } catch (Exception e) {
            log.error("Error al consultar calendario externo: {}", e.getMessage(), e);
            return List.of(); // Si falla, asumimos que no hay slots ocupados
        }
    }
    
    /**
     * Filtra slots disponibles eliminando ocupados y bloqueados
     */
    private List<TimeSlot> filterAvailableSlots(
            List<TimeSlot> theoreticalSlots,
            List<TimeSlot> busySlots,
            List<AvailabilityConfig.BlockedSlot> blockedSlots,
            LocalDate date
    ) {
        return theoreticalSlots.stream()
                .filter(slot -> !isSlotBusy(slot, busySlots))
                .filter(slot -> !isSlotBlocked(slot, blockedSlots, date))
                .toList();
    }
    
    private boolean isSlotBusy(TimeSlot slot, List<TimeSlot> busySlots) {
        return busySlots.stream()
                .anyMatch(busy -> 
                        slot.startTime().isBefore(busy.endTime()) && 
                        slot.endTime().isAfter(busy.startTime())
                );
    }
    
    private boolean isSlotBlocked(
            TimeSlot slot,
            List<AvailabilityConfig.BlockedSlot> blockedSlots,
            LocalDate date
    ) {
        String dateStr = date.toString();
        return blockedSlots.stream()
                .anyMatch(blocked -> 
                        blocked.date().equals(dateStr) &&
                        slot.startTime().toLocalTime().isBefore(LocalTime.parse(blocked.endTime())) &&
                        slot.endTime().toLocalTime().isAfter(LocalTime.parse(blocked.startTime()))
                );
    }
    
    private boolean isInBreak(LocalTime time, List<AvailabilityConfig.Break> breaks) {
        return breaks.stream()
                .anyMatch(break_ -> {
                    LocalTime breakStart = LocalTime.parse(break_.startTime());
                    LocalTime breakEnd = LocalTime.parse(break_.endTime());
                    return !time.isBefore(breakStart) && time.isBefore(breakEnd);
                });
    }
    
    private boolean isHoliday(LocalDate date, List<AvailabilityConfig.Holiday> holidays) {
        String dateStr = date.toString();
        return holidays.stream()
                .anyMatch(holiday -> holiday.date().equals(dateStr));
    }
    
    public record TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {}
}

