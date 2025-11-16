package com.relative.chat.bot.ia.domain.scheduling.exceptions;

/**
 * Excepción base para errores relacionados con calendarios
 */
public class CalendarException extends RuntimeException {
    
    public CalendarException(String message) {
        super(message);
    }
    
    public CalendarException(String message, Throwable cause) {
        super(message, cause);
    }
}

