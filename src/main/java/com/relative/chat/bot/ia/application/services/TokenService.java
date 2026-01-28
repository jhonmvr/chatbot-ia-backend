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
            // Un UUID tiene exactamente 36 caracteres (formato: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
            // Buscamos el separador después del UUID completo (posición 36)
            int uuidLength = 36;
            if (token.length() <= uuidLength) {
                log.warn("Token demasiado corto para contener UUID y hash: {}", token);
                return null;
            }
            
            // El separador debe estar en la posición 36 (justo después del UUID)
            if (token.charAt(uuidLength) != TOKEN_SEPARATOR.charAt(0)) {
                log.warn("Token sin separador después del UUID en posición 36: {}", token);
                return null;
            }
            
            String clientIdStr = token.substring(0, uuidLength);
            return UuidId.of(java.util.UUID.fromString(clientIdStr));
        } catch (IllegalArgumentException e) {
            log.warn("Error al extraer clientId del token: {}", e.getMessage());
            return null;
        } catch (StringIndexOutOfBoundsException e) {
            log.warn("Error al extraer clientId del token (índice fuera de rango): {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Valida el formato de un token
     * Formato esperado: {UUID de 36 caracteres}-{hash aleatorio}
     */
    public boolean isValidFormat(String token) {
        if (token == null || token.isBlank()) {
            log.debug("Token nulo o vacío");
            return false;
        }
        
        // Un UUID tiene exactamente 36 caracteres
        int uuidLength = 36;
        if (token.length() <= uuidLength) {
            log.debug("Token demasiado corto (length: {}, mínimo esperado: {})", token.length(), uuidLength + 1);
            return false;
        }
        
        // El separador debe estar en la posición 36 (justo después del UUID)
        if (token.charAt(uuidLength) != TOKEN_SEPARATOR.charAt(0)) {
            log.debug("Token sin separador en posición 36 (char en posición 36: '{}')", 
                token.length() > uuidLength ? token.charAt(uuidLength) : "N/A");
            return false;
        }
        
        try {
            String clientIdStr = token.substring(0, uuidLength);
            java.util.UUID.fromString(clientIdStr);
            log.debug("Formato de token válido (clientId: {})", clientIdStr);
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("Error al validar formato UUID del token: {}", e.getMessage());
            return false;
        } catch (StringIndexOutOfBoundsException e) {
            log.debug("Error al validar formato del token (índice fuera de rango): {}", e.getMessage());
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

