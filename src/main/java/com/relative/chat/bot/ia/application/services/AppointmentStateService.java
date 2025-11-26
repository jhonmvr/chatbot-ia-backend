package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestionar el estado del flujo de agendamiento por conversación
 * Almacena temporalmente el estado hasta completar el agendamiento
 */
@Slf4j
@Service
public class AppointmentStateService {
    
    private final Map<UuidId<Conversation>, AppointmentState> states = new ConcurrentHashMap<>();
    
    /**
     * Inicia el flujo de agendamiento para una conversación
     */
    public void startAppointmentFlow(UuidId<Conversation> conversationId) {
        states.put(conversationId, new AppointmentState("collecting_date", null, null, null));
        log.info("✅ Iniciado flujo de agendamiento para conversación: {}", conversationId.value());
        log.info("Estado actual: {}", states.size() + " conversaciones en modo agendamiento");
    }
    
    /**
     * Verifica si una conversación está en modo agendamiento
     */
    public boolean isInAppointmentMode(UuidId<Conversation> conversationId) {
        boolean inMode = states.containsKey(conversationId);
        if (inMode) {
            AppointmentState state = states.get(conversationId);
            log.info("🔍 Conversación {} está en modo agendamiento. Paso: {}", 
                    conversationId.value(), state != null ? state.step() : "null");
        } else {
            log.debug("Conversación {} NO está en modo agendamiento", conversationId.value());
        }
        return inMode;
    }
    
    /**
     * Obtiene el estado actual del agendamiento
     */
    public Optional<AppointmentState> getState(UuidId<Conversation> conversationId) {
        return Optional.ofNullable(states.get(conversationId));
    }
    
    /**
     * Actualiza la fecha del agendamiento
     */
    public void setDate(UuidId<Conversation> conversationId, LocalDate date) {
        AppointmentState state = states.get(conversationId);
        if (state != null) {
            states.put(conversationId, new AppointmentState("collecting_time", date, null, state.description()));
        }
    }
    
    /**
     * Actualiza la hora del agendamiento
     */
    public void setTime(UuidId<Conversation> conversationId, LocalTime time) {
        AppointmentState state = states.get(conversationId);
        if (state != null && state.date() != null) {
            states.put(conversationId, new AppointmentState("confirming", state.date(), time, state.description()));
        }
    }
    
    /**
     * Actualiza la descripción del agendamiento
     */
    public void setDescription(UuidId<Conversation> conversationId, String description) {
        AppointmentState state = states.get(conversationId);
        if (state != null) {
            states.put(conversationId, new AppointmentState(state.step(), state.date(), state.time(), description));
        }
    }
    
    /**
     * Obtiene la fecha y hora completa del agendamiento
     */
    public Optional<LocalDateTime> getDateTime(UuidId<Conversation> conversationId) {
        AppointmentState state = states.get(conversationId);
        if (state != null && state.date() != null && state.time() != null) {
            return Optional.of(LocalDateTime.of(state.date(), state.time()));
        }
        return Optional.empty();
    }
    
    /**
     * Limpia el estado del agendamiento (después de completar o cancelar)
     */
    public void clearState(UuidId<Conversation> conversationId) {
        states.remove(conversationId);
        log.debug("Estado de agendamiento limpiado para conversación: {}", conversationId.value());
    }
    
    /**
     * Estado interno del agendamiento
     */
    public record AppointmentState(
        String step,  // "collecting_date", "collecting_time", "confirming"
        LocalDate date,
        LocalTime time,
        String description
    ) {}
}

