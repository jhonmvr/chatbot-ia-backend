package com.relative.chat.bot.ia.infrastructure.adapters.out.calendar;

import com.relative.chat.bot.ia.application.ports.out.CalendarService;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Router para seleccionar el adaptador de calendario correcto según el proveedor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarServiceRouter {
    
    private final GoogleCalendarAdapter googleCalendarAdapter;
    private final OutlookCalendarAdapter outlookCalendarAdapter;
    
    /**
     * Obtiene el servicio de calendario apropiado según el proveedor
     * 
     * @param provider Proveedor de calendario
     * @return Servicio de calendario
     */
    public CalendarService getCalendarService(CalendarProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleCalendarAdapter;
            case OUTLOOK -> outlookCalendarAdapter;
        };
    }
}

