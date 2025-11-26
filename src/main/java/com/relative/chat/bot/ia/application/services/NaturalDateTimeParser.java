package com.relative.chat.bot.ia.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para parsear fechas y horas en lenguaje natural
 */
@Slf4j
@Service
public class NaturalDateTimeParser {
    
    // Patrón mejorado para capturar horas con "a las", "las", etc.
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "\\b(?:a las|las|a|de las)?\\s*(\\d{1,2})\\s*(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.|a\\.m|p\\.m)?\\b",
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b"
    );
    
    /**
     * Parsea una fecha desde lenguaje natural
     */
    public LocalDate parseDate(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        
        String normalized = message.toLowerCase().trim();
        LocalDate today = LocalDate.now();
        
        // Días de la semana (mejorar para manejar "viernes de esta semana", "este viernes", etc.)
        if (normalized.contains("lunes")) {
            return getNextDayOfWeek(today, DayOfWeek.MONDAY);
        } else if (normalized.contains("martes")) {
            return getNextDayOfWeek(today, DayOfWeek.TUESDAY);
        } else if (normalized.contains("miércoles") || normalized.contains("miercoles")) {
            return getNextDayOfWeek(today, DayOfWeek.WEDNESDAY);
        } else if (normalized.contains("jueves")) {
            return getNextDayOfWeek(today, DayOfWeek.THURSDAY);
        } else if (normalized.contains("viernes")) {
            // Manejar "viernes de esta semana", "este viernes", "el viernes"
            return getNextDayOfWeek(today, DayOfWeek.FRIDAY);
        } else if (normalized.contains("sábado") || normalized.contains("sabado")) {
            return getNextDayOfWeek(today, DayOfWeek.SATURDAY);
        } else if (normalized.contains("domingo")) {
            return getNextDayOfWeek(today, DayOfWeek.SUNDAY);
        }
        
        // Referencias temporales
        if (normalized.contains("hoy")) {
            return today;
        } else if (normalized.contains("mañana") || normalized.contains("manana")) {
            return today.plusDays(1);
        } else if (normalized.contains("pasado mañana") || normalized.contains("pasado manana")) {
            return today.plusDays(2);
        } else if (normalized.contains("próxima semana") || normalized.contains("proxima semana") || 
                   normalized.contains("la proxima semana") || normalized.contains("la próxima semana")) {
            // "la próxima semana" = ajustar al lunes de la próxima semana
            // Si hoy es lunes-viernes, "próxima semana" generalmente significa el lunes de la próxima semana
            // Si hoy es sábado-domingo, "próxima semana" es el lunes siguiente
            DayOfWeek currentDay = today.getDayOfWeek();
            if (currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY) {
                // Si es fin de semana, "próxima semana" es el lunes siguiente
                return getNextDayOfWeek(today, DayOfWeek.MONDAY);
            } else {
                // Si es día laboral, "próxima semana" es el lunes de la semana siguiente
                int daysUntilNextMonday = DayOfWeek.MONDAY.getValue() - currentDay.getValue();
                if (daysUntilNextMonday <= 0) {
                    daysUntilNextMonday += 7; // Siguiente lunes
                }
                return today.plusDays(daysUntilNextMonday);
            }
        }
        
        // Fechas numéricas (DD/MM/YYYY o DD-MM-YYYY)
        Matcher dateMatcher = DATE_PATTERN.matcher(message);
        if (dateMatcher.find()) {
            try {
                int day = Integer.parseInt(dateMatcher.group(1));
                int month = Integer.parseInt(dateMatcher.group(2));
                int year = dateMatcher.group(3) != null 
                        ? Integer.parseInt(dateMatcher.group(3).length() == 2 
                                ? "20" + dateMatcher.group(3) 
                                : dateMatcher.group(3))
                        : today.getYear();
                
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                log.debug("Error al parsear fecha numérica: {}", e.getMessage());
            }
        }
        
        // Intentar parsear como fecha ISO
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            // Ignorar
        }
        
        return null;
    }
    
    /**
     * Parsea una hora desde lenguaje natural
     */
    public LocalTime parseTime(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        
        String normalized = message.toLowerCase().trim();
        
        // Manejar referencias temporales comunes
        if (normalized.contains("mañana") || normalized.contains("manana") || 
            normalized.contains("en la manana") || normalized.contains("en la mañana")) {
            // "en la mañana" generalmente significa entre 8am y 12pm
            // Por defecto, usar 9:00 AM
            return LocalTime.of(9, 0);
        } else if (normalized.contains("tarde") || normalized.contains("en la tarde")) {
            // "en la tarde" generalmente significa entre 12pm y 6pm
            // Por defecto, usar 2:00 PM
            return LocalTime.of(14, 0);
        } else if (normalized.contains("noche") || normalized.contains("en la noche")) {
            // "en la noche" generalmente significa después de las 6pm
            // Por defecto, usar 7:00 PM
            return LocalTime.of(19, 0);
        }
        
        // Buscar patrón de hora (ej: "8 am", "14:30", "2pm", "a las 8am", "a las 8:00")
        Matcher timeMatcher = TIME_PATTERN.matcher(message);
        if (timeMatcher.find()) {
            try {
                int hour = Integer.parseInt(timeMatcher.group(1));
                int minute = timeMatcher.group(2) != null ? Integer.parseInt(timeMatcher.group(2)) : 0;
                String amPm = timeMatcher.group(3);
                
                if (amPm != null && (amPm.toLowerCase().startsWith("p") || amPm.toLowerCase().contains("p"))) {
                    // PM
                    if (hour != 12) {
                        hour += 12;
                    }
                } else if (amPm != null && (amPm.toLowerCase().startsWith("a") || amPm.toLowerCase().contains("a"))) {
                    // AM
                    if (hour == 12) {
                        hour = 0;
                    }
                } else {
                    // Sin AM/PM, asumir formato 24h si es > 12, sino AM
                    if (hour < 12 && hour > 0 && !normalized.contains(":")) {
                        // Si no tiene ":" y es < 12, probablemente es AM
                        // Pero si tiene ":" podría ser formato 24h
                    }
                }
                
                return LocalTime.of(hour % 24, minute);
            } catch (Exception e) {
                log.debug("Error al parsear hora: {}", e.getMessage());
            }
        }
        
        // Intentar parsear como hora ISO
        try {
            return LocalTime.parse(normalized, DateTimeFormatter.ISO_TIME);
        } catch (DateTimeParseException e) {
            // Ignorar
        }
        
        return null;
    }
    
    private LocalDate getNextDayOfWeek(LocalDate from, DayOfWeek dayOfWeek) {
        int daysUntil = dayOfWeek.getValue() - from.getDayOfWeek().getValue();
        if (daysUntil <= 0) {
            daysUntil += 7; // Siguiente semana
        }
        LocalDate result = from.plusDays(daysUntil);
        log.debug("getNextDayOfWeek: desde {} hasta {} = {}", from, dayOfWeek, result);
        return result;
    }
}

