package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import com.relative.chat.bot.ia.domain.types.*;
import com.relative.chat.bot.ia.domain.vo.*;
import java.time.*;
import java.util.*;

public record ClientPhone(
        UuidId<ClientPhone> id,
        UuidId<Client> clientId,
        PhoneE164 phone,
        Channel channel,
        String provider,
        String providerSid,  // phone_number_id de Meta, SID de Twilio, etc.
        EntityStatus status,
        Instant verifiedAt,
        // Campos específicos para Meta WhatsApp
        String metaAccessToken,
        String metaPhoneNumberId,
        String metaApiVersion,
        // Campos específicos para Twilio
        String twilioAccountSid,
        String twilioAuthToken,
        // Campos específicos para WWebJs
        String wwebjsSessionId,
        String wwebjsWebhookUrl,
        // Campos comunes para todos los proveedores
        String apiBaseUrl,
        String webhookUrl,
        String verifyToken,
        String webhookSecret
) {
    public Optional<Instant> verifiedAtOpt() {
        return Optional.ofNullable(verifiedAt);
    }
    
    public Optional<String> providerSidOpt() {
        return Optional.ofNullable(providerSid);
    }
    
    // Métodos de conveniencia para Meta
    public Optional<String> metaAccessTokenOpt() {
        return Optional.ofNullable(metaAccessToken);
    }
    
    public Optional<String> metaPhoneNumberIdOpt() {
        return Optional.ofNullable(metaPhoneNumberId);
    }
    
    public String metaApiVersionOrDefault() {
        return metaApiVersion != null ? metaApiVersion : "v21.0";
    }
    
    // Métodos de conveniencia para Twilio
    public Optional<String> twilioAccountSidOpt() {
        return Optional.ofNullable(twilioAccountSid);
    }
    
    public Optional<String> twilioAuthTokenOpt() {
        return Optional.ofNullable(twilioAuthToken);
    }
    
    // Métodos de conveniencia para WWebJs
    public Optional<String> wwebjsSessionIdOpt() {
        return Optional.ofNullable(wwebjsSessionId);
    }
    
    public Optional<String> wwebjsWebhookUrlOpt() {
        return Optional.ofNullable(wwebjsWebhookUrl);
    }
    
    // Métodos de conveniencia para campos comunes
    public Optional<String> apiBaseUrlOpt() {
        return Optional.ofNullable(apiBaseUrl);
    }
    
    public Optional<String> webhookUrlOpt() {
        return Optional.ofNullable(webhookUrl);
    }
    
    public Optional<String> verifyTokenOpt() {
        return Optional.ofNullable(verifyToken);
    }
    
    public Optional<String> webhookSecretOpt() {
        return Optional.ofNullable(webhookSecret);
    }
}