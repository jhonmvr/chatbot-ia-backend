package com.relative.chat.bot.ia.infrastructure.security;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

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
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return Optional.empty();
        }
        
        Object clientIdObj = requestAttributes.getAttribute(CLIENT_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (clientIdObj instanceof UuidId) {
            @SuppressWarnings("unchecked")
            UuidId<Client> clientId = (UuidId<Client>) clientIdObj;
            return Optional.of(clientId);
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
     * Establece el clientId en el contexto de autenticación
     * (usado internamente por el filtro)
     */
    public static void setAuthenticatedClientId(UuidId<Client> clientId) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(CLIENT_ID_ATTRIBUTE, clientId, RequestAttributes.SCOPE_REQUEST);
        }
    }
}

