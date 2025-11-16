package com.relative.chat.bot.ia.domain.scheduling.exceptions;

/**
 * Excepción para errores de API de proveedores de calendario
 * (errores HTTP, límites de rate, etc.)
 */
public class CalendarApiException extends CalendarException {
    
    private final int statusCode;
    private final String errorCode;
    
    public CalendarApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = null;
    }
    
    public CalendarApiException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public CalendarApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = null;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

