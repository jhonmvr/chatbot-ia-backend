package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.application.dto.AppointmentIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Servicio para detectar intención de agendamiento en mensajes
 */
@Slf4j
@Service
public class AppointmentIntentDetector {
    
    private static final Pattern APPOINTMENT_PATTERNS = Pattern.compile(
            "\\b(agendar|agendamiento|agenda|reservar|reserva|cita|turno|appointment|schedule|" +
            "quiero agendar|necesito una cita|quiero reservar|hacer una cita|pedir turno|" +
            "solicitar cita|programar|programar cita|agendar cita|reservar cita)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    
    /**
     * Detecta si un mensaje contiene intención de agendamiento
     */
    public AppointmentIntent detect(String message) {
        if (message == null || message.isBlank()) {
            return AppointmentIntent.none();
        }
        
        String normalized = message.toLowerCase().trim();
        
        // Verificar si contiene palabras clave de agendamiento
        if (!APPOINTMENT_PATTERNS.matcher(normalized).find()) {
            return AppointmentIntent.none();
        }
        
        // Por ahora, solo detectamos la intención
        // El parser de fecha/hora se hará en el siguiente paso
        return new AppointmentIntent(true, null, null, null);
    }
}

