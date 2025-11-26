package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.application.dto.AvailabilityConfig;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.domain.ports.scheduling.CalendarProviderAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityConfigService {
    
    private final CalendarProviderAccountRepository accountRepository;
    
    /**
     * Obtiene la configuración de disponibilidad de una cuenta de calendario
     */
    public AvailabilityConfig getAvailabilityConfig(UuidId<CalendarProviderAccount> accountId) {
        CalendarProviderAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de calendario no encontrada"));
        
        Map<String, Object> config = account.config();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> availability = config != null && config.containsKey("availability")
                ? (Map<String, Object>) config.get("availability")
                : null;
        
        if (availability == null) {
            return getDefaultAvailabilityConfig();
        }
        
        return parseAvailabilityConfig(availability);
    }
    
    /**
     * Actualiza la configuración de disponibilidad
     */
    public void updateAvailabilityConfig(
            UuidId<CalendarProviderAccount> accountId,
            AvailabilityConfig availabilityConfig
    ) {
        CalendarProviderAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de calendario no encontrada"));
        
        Map<String, Object> config = new HashMap<>(account.config() != null ? account.config() : new HashMap<>());
        config.put("availability", toMap(availabilityConfig));
        
        // Actualizar cuenta con nueva configuración
        CalendarProviderAccount updatedAccount = new CalendarProviderAccount(
                account.id(),
                account.clientId(),
                account.provider(),
                account.accountEmail(),
                account.accessToken(),
                account.refreshToken(),
                account.tokenExpiresAt(),
                config,
                account.isActive(),
                account.createdAt(),
                java.time.Instant.now()
        );
        
        accountRepository.save(updatedAccount);
        
        log.info("Configuración de disponibilidad actualizada para cuenta: {}", accountId.value());
    }
    
    private AvailabilityConfig getDefaultAvailabilityConfig() {
        // Configuración por defecto: Lunes a Viernes 8:00-18:00, slots de 30 min
        Map<String, AvailabilityConfig.DaySchedule> workingHours = new HashMap<>();
        workingHours.put("monday", new AvailabilityConfig.DaySchedule(true, "08:00", "18:00", List.of()));
        workingHours.put("tuesday", new AvailabilityConfig.DaySchedule(true, "08:00", "18:00", List.of()));
        workingHours.put("wednesday", new AvailabilityConfig.DaySchedule(true, "08:00", "18:00", List.of()));
        workingHours.put("thursday", new AvailabilityConfig.DaySchedule(true, "08:00", "18:00", List.of()));
        workingHours.put("friday", new AvailabilityConfig.DaySchedule(true, "08:00", "18:00", List.of()));
        workingHours.put("saturday", new AvailabilityConfig.DaySchedule(false, null, null, List.of()));
        workingHours.put("sunday", new AvailabilityConfig.DaySchedule(false, null, null, List.of()));
        
        return new AvailabilityConfig(
                true,
                30,
                30,
                workingHours,
                List.of(),
                List.of()
        );
    }
    
    @SuppressWarnings("unchecked")
    private AvailabilityConfig parseAvailabilityConfig(Map<String, Object> map) {
        boolean enabled = map.getOrDefault("enabled", true) instanceof Boolean 
                ? (Boolean) map.get("enabled") 
                : true;
        int slotDuration = map.getOrDefault("slotDurationMinutes", 30) instanceof Number
                ? ((Number) map.get("slotDurationMinutes")).intValue()
                : 30;
        int advanceDays = map.getOrDefault("advanceBookingDays", 30) instanceof Number
                ? ((Number) map.get("advanceBookingDays")).intValue()
                : 30;
        
        Map<String, Object> workingHoursMap = map.getOrDefault("workingHours", Map.of()) instanceof Map
                ? (Map<String, Object>) map.get("workingHours")
                : Map.of();
        
        Map<String, AvailabilityConfig.DaySchedule> workingHours = new HashMap<>();
        for (String day : List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")) {
            Object dayObj = workingHoursMap.get(day);
            if (dayObj instanceof Map) {
                Map<String, Object> dayMap = (Map<String, Object>) dayObj;
                boolean dayEnabled = dayMap.getOrDefault("enabled", false) instanceof Boolean
                        ? (Boolean) dayMap.get("enabled")
                        : false;
                String startTime = dayMap.getOrDefault("startTime", null) instanceof String
                        ? (String) dayMap.get("startTime")
                        : null;
                String endTime = dayMap.getOrDefault("endTime", null) instanceof String
                        ? (String) dayMap.get("endTime")
                        : null;
                
                List<AvailabilityConfig.Break> breaks = List.of(); // Simplificado por ahora
                
                workingHours.put(day, new AvailabilityConfig.DaySchedule(dayEnabled, startTime, endTime, breaks));
            }
        }
        
        List<AvailabilityConfig.Holiday> holidays = List.of(); // Simplificado
        List<AvailabilityConfig.BlockedSlot> blockedSlots = List.of(); // Simplificado
        
        return new AvailabilityConfig(enabled, slotDuration, advanceDays, workingHours, holidays, blockedSlots);
    }
    
    private Map<String, Object> toMap(AvailabilityConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", config.enabled());
        map.put("slotDurationMinutes", config.slotDurationMinutes());
        map.put("advanceBookingDays", config.advanceBookingDays());
        
        Map<String, Object> workingHoursMap = new HashMap<>();
        config.workingHours().forEach((day, schedule) -> {
            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("enabled", schedule.enabled());
            dayMap.put("startTime", schedule.startTime());
            dayMap.put("endTime", schedule.endTime());
            dayMap.put("breaks", schedule.breaks().stream()
                    .map(b -> Map.of(
                            "startTime", b.startTime(),
                            "endTime", b.endTime(),
                            "description", b.description() != null ? b.description() : ""
                    ))
                    .toList());
            workingHoursMap.put(day, dayMap);
        });
        map.put("workingHours", workingHoursMap);
        
        map.put("holidays", config.holidays().stream()
                .map(h -> Map.of("date", h.date(), "description", h.description() != null ? h.description() : ""))
                .toList());
        
        map.put("blockedSlots", config.blockedSlots().stream()
                .map(b -> Map.of(
                        "date", b.date(),
                        "startTime", b.startTime(),
                        "endTime", b.endTime(),
                        "description", b.description() != null ? b.description() : ""
                ))
                .toList());
        
        return map;
    }
}

