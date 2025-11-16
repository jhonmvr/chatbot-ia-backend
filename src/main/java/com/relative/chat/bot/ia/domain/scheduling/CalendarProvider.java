package com.relative.chat.bot.ia.domain.scheduling;

/**
 * Proveedores de calendario soportados
 */
public enum CalendarProvider {
    GOOGLE("Google Calendar"),
    OUTLOOK("Microsoft Outlook / Microsoft 365");
    
    private final String displayName;
    
    CalendarProvider(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

