package com.relative.chat.bot.ia.domain.scheduling.exceptions;

/**
 * Excepción para errores de autenticación con proveedores de calendario
 * (tokens expirados, inválidos, etc.)
 */
public class CalendarAuthenticationException extends CalendarException {
    
    public CalendarAuthenticationException(String message) {
        super(message);
    }
    
    public CalendarAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

