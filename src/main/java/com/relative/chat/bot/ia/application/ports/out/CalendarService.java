package com.relative.chat.bot.ia.application.ports.out;

import com.relative.chat.bot.ia.application.dto.CalendarEvent;
import com.relative.chat.bot.ia.application.dto.CalendarEventResponse;
import com.relative.chat.bot.ia.application.dto.FreeBusyQuery;
import com.relative.chat.bot.ia.application.dto.FreeBusyResponse;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;

import java.util.List;

/**
 * Puerto para interactuar con proveedores de calendario (Google Calendar, Outlook)
 */
public interface CalendarService {
    
    /**
     * Crea un evento en el calendario
     * 
     * @param account Cuenta de proveedor de calendario
     * @param event Datos del evento a crear
     * @return Respuesta con el ID del evento creado
     */
    CalendarEventResponse createEvent(CalendarProviderAccount account, CalendarEvent event);
    
    /**
     * Actualiza un evento existente
     * 
     * @param account Cuenta de proveedor de calendario
     * @param eventId ID del evento a actualizar
     * @param event Datos actualizados del evento
     * @return Respuesta con el evento actualizado
     */
    CalendarEventResponse updateEvent(CalendarProviderAccount account, String eventId, CalendarEvent event);
    
    /**
     * Elimina un evento del calendario
     * 
     * @param account Cuenta de proveedor de calendario
     * @param eventId ID del evento a eliminar
     */
    void deleteEvent(CalendarProviderAccount account, String eventId);
    
    /**
     * Obtiene un evento por su ID
     * 
     * @param account Cuenta de proveedor de calendario
     * @param eventId ID del evento
     * @return Datos del evento
     */
    CalendarEventResponse getEvent(CalendarProviderAccount account, String eventId);
    
    /**
     * Lista eventos del calendario en un rango de fechas
     * 
     * @param account Cuenta de proveedor de calendario
     * @param startTime Fecha/hora de inicio
     * @param endTime Fecha/hora de fin
     * @return Lista de eventos
     */
    List<CalendarEventResponse> listEvents(CalendarProviderAccount account, java.time.Instant startTime, java.time.Instant endTime);
    
    /**
     * Consulta disponibilidad (Free/Busy) en un rango de fechas
     * 
     * @param account Cuenta de proveedor de calendario
     * @param query Consulta de disponibilidad
     * @return Respuesta con información de disponibilidad
     */
    FreeBusyResponse getFreeBusy(CalendarProviderAccount account, FreeBusyQuery query);
}

