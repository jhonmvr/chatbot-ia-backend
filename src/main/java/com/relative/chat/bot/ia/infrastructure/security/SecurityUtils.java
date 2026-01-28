package com.relative.chat.bot.ia.infrastructure.security;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Utilidades para trabajar con el contexto de seguridad
 */
public class SecurityUtils {
    
    /**
     * Constante para el atributo del clientId en el request
     */
    private static final String CLIENT_ID_ATTRIBUTE = "authenticatedClientId";
    
    /**
     * Obtiene el clientId del contexto de autenticación
     * @return Optional con el clientId si está autenticado
     */
    public static Optional<UuidId<Client>> getAuthenticatedClientId() {
        // Intentar obtener desde RequestContextHolder primero
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            Object clientIdObj = requestAttributes.getAttribute(CLIENT_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (clientIdObj instanceof UuidId) {
                @SuppressWarnings("unchecked")
                UuidId<Client> clientId = (UuidId<Client>) clientIdObj;
                return Optional.of(clientId);
            }
            
            // Si RequestContextHolder está disponible pero no tiene el atributo,
            // intentar obtenerlo desde el HttpServletRequest
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                if (request != null) {
                    Object requestAttr = request.getAttribute(CLIENT_ID_ATTRIBUTE);
                    if (requestAttr instanceof UuidId) {
                        @SuppressWarnings("unchecked")
                        UuidId<Client> clientId = (UuidId<Client>) requestAttr;
                        return Optional.of(clientId);
                    }
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Obtiene el clientId del contexto o lanza excepción si no está autenticado
     * @return clientId autenticado
     * @throws IllegalStateException si no hay cliente autenticado
     */
    public static UuidId<Client> requireAuthenticatedClientId() {
        return getAuthenticatedClientId()
                .orElseThrow(() -> new IllegalStateException("Cliente no autenticado"));
    }
    
    /**
     * Establece el clientId en el contexto de autenticación usando RequestContextHolder
     * (usado internamente por el filtro cuando RequestContextHolder está disponible)
     */
    public static void setAuthenticatedClientId(UuidId<Client> clientId) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(CLIENT_ID_ATTRIBUTE, clientId, RequestAttributes.SCOPE_REQUEST);
        }
    }
    
    /**
     * Establece el clientId directamente en el HttpServletRequest
     * (usado internamente por el filtro cuando RequestContextHolder no está disponible)
     */
    public static void setAuthenticatedClientId(HttpServletRequest request, UuidId<Client> clientId) {
        if (request != null) {
            request.setAttribute(CLIENT_ID_ATTRIBUTE, clientId);
            // También establecer en RequestContextHolder si está disponible
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                requestAttributes.setAttribute(CLIENT_ID_ATTRIBUTE, clientId, RequestAttributes.SCOPE_REQUEST);
            }
        }
    }
}

