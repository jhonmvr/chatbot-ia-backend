package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.application.usecases.ManageCalendarProviderAccount;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.ports.scheduling.CalendarProviderAccountRepository;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.domain.scheduling.exceptions.CalendarAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Servicio para renovar tokens OAuth2 automáticamente cuando expiran
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {
    
    private final TokenExchangeService tokenExchangeService;
    private final CalendarProviderAccountRepository accountRepository;
    private final ManageCalendarProviderAccount manageCalendarProviderAccount;
    
    /**
     * Renueva el token de una cuenta si está expirado o próximo a expirar
     * 
     * @param account Cuenta de proveedor de calendario
     * @return Cuenta con tokens renovados
     */
    public CalendarProviderAccount refreshTokenIfNeeded(CalendarProviderAccount account) {
        // Si el token no está expirado y no está próximo a expirar (más de 5 minutos), no hacer nada
        if (!isTokenExpiredOrExpiringSoon(account)) {
            return account;
        }
        
        log.info("Renovando token para account: {} (provider: {})", account.id().value(), account.provider());
        
        if (account.refreshToken() == null || account.refreshToken().isBlank()) {
            throw new CalendarAuthenticationException(
                    "No se puede renovar el token: refresh_token no disponible para account: " + account.id().value()
            );
        }
        
        try {
            // Renovar token usando el refresh token
            TokenExchangeService.OAuth2Tokens newTokens = tokenExchangeService.refreshTokens(
                    account.refreshToken(),
                    account.provider()
            );
            
            // Actualizar la cuenta con los nuevos tokens
            var updateRequest = new com.relative.chat.bot.ia.application.dto.UpdateCalendarProviderAccountRequest(
                    newTokens.accessToken(),
                    newTokens.refreshToken(),
                    newTokens.expiresAt(),
                    null,
                    null
            );
            
            manageCalendarProviderAccount.update(account.id().value().toString(), updateRequest);
            
            // Obtener la cuenta actualizada
            return accountRepository.findById(account.id())
                    .orElseThrow(() -> new RuntimeException("Cuenta no encontrada después de actualizar tokens"));
            
        } catch (Exception e) {
            log.error("Error al renovar token para account {}: {}", account.id().value(), e.getMessage(), e);
            throw new CalendarAuthenticationException(
                    "Error al renovar token: " + e.getMessage(), e
            );
        }
    }
    
    /**
     * Verifica si el token está expirado o próximo a expirar (menos de 5 minutos)
     */
    private boolean isTokenExpiredOrExpiringSoon(CalendarProviderAccount account) {
        if (account.tokenExpiresAt() == null) {
            // Si no hay fecha de expiración, asumimos que no está expirado
            return false;
        }
        
        Instant now = Instant.now();
        Instant expiresAt = account.tokenExpiresAt();
        
        // Considerar expirado si ya pasó la fecha o si falta menos de 5 minutos
        Instant fiveMinutesBeforeExpiry = expiresAt.minusSeconds(5 * 60);
        
        return now.isAfter(fiveMinutesBeforeExpiry);
    }
    
    /**
     * Obtiene un token válido, renovándolo si es necesario
     * 
     * @param accountId ID de la cuenta
     * @return Token de acceso válido
     */
    public String getValidAccessToken(UuidId<CalendarProviderAccount> accountId) {
        CalendarProviderAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CalendarAuthenticationException(
                        "Cuenta de calendario no encontrada: " + accountId.value()
                ));
        
        account = refreshTokenIfNeeded(account);
        return account.accessToken();
    }
}

