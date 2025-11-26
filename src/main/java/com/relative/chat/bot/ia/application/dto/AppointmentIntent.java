package com.relative.chat.bot.ia.application.dto;

/**
 * Intención de agendamiento detectada en un mensaje
 */
public record AppointmentIntent(
    boolean hasIntent,
    String date,
    String time,
    String description
) {
    public static AppointmentIntent none() {
        return new AppointmentIntent(false, null, null, null);
    }
    
    public static AppointmentIntent withDate(String date) {
        return new AppointmentIntent(true, date, null, null);
    }
    
    public static AppointmentIntent withDateTime(String date, String time) {
        return new AppointmentIntent(true, date, time, null);
    }
}

