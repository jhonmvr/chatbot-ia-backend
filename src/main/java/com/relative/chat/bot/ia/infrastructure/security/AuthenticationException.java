package com.relative.chat.bot.ia.infrastructure.security;

/**
 * Excepción para errores de autenticación
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

