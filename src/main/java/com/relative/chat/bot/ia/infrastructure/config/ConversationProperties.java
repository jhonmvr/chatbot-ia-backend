package com.relative.chat.bot.ia.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para gestión de conversaciones
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.conversation")
public class ConversationProperties {
    
    private AutoClose autoClose = new AutoClose();
    private DailyClose dailyClose = new DailyClose();
    
    @PostConstruct
    public void logConfiguration() {
        log.info("Configuración de conversaciones cargada:");
        log.info("  - Auto-cierre por inactividad: {} ({} horas)", 
                autoClose.isEnabled(), autoClose.getInactivityHours());
        log.info("  - Cierre diario: {} (a las {} en {})", 
                dailyClose.isEnabled(), dailyClose.getTime(), dailyClose.getTimezone());
    }
    
    @Getter
    @Setter
    public static class AutoClose {
        private boolean enabled = true;
        private int inactivityHours = 24;  // Horas de inactividad antes de cerrar
    }
    
    @Getter
    @Setter
    public static class DailyClose {
        private boolean enabled = true;
        private String time = "00:00";  // Hora en formato HH:mm
        private String timezone = "America/Guayaquil";
    }
}

