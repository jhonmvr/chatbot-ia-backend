package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.ApiKey;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.ports.identity.ApiKeyRepository;
import com.relative.chat.bot.ia.domain.ports.identity.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Servicio para gestionar API Keys de clientes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private final ClientRepository clientRepository;
    private final TokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Crea una nueva API Key para un cliente
     * @return ApiKey con apiKey y apiSecret (solo se muestra una vez)
     */
    @Transactional
    public ApiKeyCreationResult createApiKey(UuidId<Client> clientId) {
        // Verificar que el cliente existe
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new IllegalArgumentException("Cliente no encontrado: " + clientId.value());
        }
        
        // Generar apiKey único
        String apiKey = generateUniqueApiKey();
        
        // Generar apiSecret
        String apiSecret = generateApiSecret();
        
        // Hash del secreto
        String apiSecretHash = hashSecret(apiSecret);
        
        // Crear ApiKey
        ApiKey apiKeyEntity = ApiKey.create(clientId, apiKey, apiSecretHash);
        apiKeyRepository.save(apiKeyEntity);
        
        log.info("API Key creada para cliente: {}", clientId.value());
        
        return new ApiKeyCreationResult(apiKeyEntity, apiSecret);
    }
    
    /**
     * Valida las credenciales (apiKey + apiSecret) y retorna el token
     */
    @Transactional
    public Optional<TokenResult> authenticate(String apiKey, String apiSecret) {
        // Buscar API Key
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKey);
        
        if (apiKeyOpt.isEmpty()) {
            log.warn("Intento de autenticación con API Key no encontrada: {}", apiKey);
            return Optional.empty();
        }
        
        ApiKey apiKeyEntity = apiKeyOpt.get();
        
        // Verificar que esté activa
        if (!apiKeyEntity.isActive()) {
            log.warn("Intento de autenticación con API Key inactiva: {}", apiKey);
            return Optional.empty();
        }
        
        // Verificar el secreto
        String providedSecretHash = hashSecret(apiSecret);
        if (!apiKeyEntity.apiSecretHash().equals(providedSecretHash)) {
            log.warn("Intento de autenticación con secreto inválido para API Key: {}", apiKey);
            return Optional.empty();
        }
        
        // Generar token
        String token = tokenService.generateToken(apiKeyEntity.clientId());
        
        // Marcar como usada
        ApiKey updatedApiKey = apiKeyEntity.markAsUsed();
        apiKeyRepository.save(updatedApiKey);
        
        log.info("Autenticación exitosa para cliente: {}", apiKeyEntity.clientId().value());
        
        return Optional.of(new TokenResult(token, apiKeyEntity.clientId()));
    }
    
    /**
     * Valida un token y retorna el clientId si es válido
     */
    public Optional<UuidId<Client>> validateToken(String token) {
        if (!tokenService.isValidFormat(token)) {
            return Optional.empty();
        }
        
        UuidId<Client> clientId = tokenService.extractClientId(token);
        if (clientId == null) {
            return Optional.empty();
        }
        
        // Verificar que el cliente existe y está activo
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (clientOpt.isEmpty() || clientOpt.get().status().name().equals("INACTIVE")) {
            return Optional.empty();
        }
        
        return Optional.of(clientId);
    }
    
    /**
     * Genera un apiKey único
     */
    private String generateUniqueApiKey() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            String apiKey = "sk_" + generateRandomString(32);
            if (!apiKeyRepository.existsByApiKey(apiKey)) {
                return apiKey;
            }
        }
        throw new IllegalStateException("No se pudo generar un API Key único después de " + maxAttempts + " intentos");
    }
    
    /**
     * Genera un apiSecret aleatorio
     */
    private String generateApiSecret() {
        return generateRandomString(64);
    }
    
    /**
     * Genera una cadena aleatoria segura
     */
    private String generateRandomString(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Hashea un secreto usando SHA-256
     */
    private String hashSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
    
    /**
     * Resultado de la creación de API Key
     */
    public record ApiKeyCreationResult(ApiKey apiKey, String apiSecret) {
        // apiSecret solo se muestra una vez al crear
    }
    
    /**
     * Resultado de la autenticación
     */
    public record TokenResult(String token, UuidId<Client> clientId) {
    }
}

