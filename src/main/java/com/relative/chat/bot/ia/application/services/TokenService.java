package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Servicio para generar y validar tokens simples
 * Formato del token: {clientId}-{randomHash}
 */
@Slf4j
@Service
public class TokenService {
    
    private static final String TOKEN_SEPARATOR = "-";
    private static final int RANDOM_BYTES = 32; // 256 bits
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Genera un token simple para un cliente
     * Formato: {clientId}-{randomHash}
     */
    public String generateToken(UuidId<Client> clientId) {
        String randomHash = generateRandomHash();
        return clientId.value().toString() + TOKEN_SEPARATOR + randomHash;
    }
    
    /**
     * Extrae el clientId de un token
     * @return clientId si el token es válido, null si no
     */
    public UuidId<Client> extractClientId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        
        try {
            int separatorIndex = token.indexOf(TOKEN_SEPARATOR);
            if (separatorIndex <= 0 || separatorIndex >= token.length() - 1) {
                log.warn("Token con formato inválido: {}", token);
                return null;
            }
            
            String clientIdStr = token.substring(0, separatorIndex);
            return UuidId.of(java.util.UUID.fromString(clientIdStr));
        } catch (IllegalArgumentException e) {
            log.warn("Error al extraer clientId del token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Valida el formato de un token
     */
    public boolean isValidFormat(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        
        int separatorIndex = token.indexOf(TOKEN_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex >= token.length() - 1) {
            return false;
        }
        
        try {
            String clientIdStr = token.substring(0, separatorIndex);
            java.util.UUID.fromString(clientIdStr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Genera un hash aleatorio seguro
     */
    private String generateRandomHash() {
        byte[] randomBytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

