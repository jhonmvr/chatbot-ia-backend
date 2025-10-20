package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio para obtener configuración de proveedores de WhatsApp desde la base de datos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppProviderConfigService {
    
    private final ClientPhoneRepository clientPhoneRepository;
    
    /**
     * Obtiene la configuración de Meta WhatsApp para un cliente específico
     * 
     * @param clientId ID del cliente
     * @return Configuración de Meta WhatsApp o Optional.empty() si no está configurado
     */
    public Optional<MetaWhatsAppConfig> getMetaConfig(UuidId<Client> clientId) {
        return clientPhoneRepository.findByClient(clientId)
                .stream()
                .filter(phone -> "META".equalsIgnoreCase(phone.provider()))
                .filter(phone -> phone.metaAccessTokenOpt().isPresent())
                .findFirst()
                .map(phone -> new MetaWhatsAppConfig(
                        phone.metaAccessTokenOpt().orElse(null),
                        phone.metaPhoneNumberIdOpt().orElse(null),
                        phone.metaApiVersionOrDefault(),
                        phone.apiBaseUrlOpt().orElse("https://graph.facebook.com"),
                        phone.webhookUrlOpt().orElse(null),
                        phone.verifyTokenOpt().orElse(null)
                ));
    }
    
    /**
     * Obtiene la configuración de Meta WhatsApp por provider SID
     * 
     * @param providerSid Provider SID (phone_number_id de Meta)
     * @return Configuración de Meta WhatsApp o Optional.empty() si no está configurado
     */
    public Optional<MetaWhatsAppConfig> getMetaConfigByProviderSid(String providerSid) {
        return clientPhoneRepository.findByProviderSid(providerSid, "META")
                .filter(phone -> phone.metaAccessTokenOpt().isPresent())
                .map(phone -> new MetaWhatsAppConfig(
                        phone.metaAccessTokenOpt().orElse(null),
                        phone.metaPhoneNumberIdOpt().orElse(null),
                        phone.metaApiVersionOrDefault(),
                        phone.apiBaseUrlOpt().orElse("https://graph.facebook.com"),
                        phone.webhookUrlOpt().orElse(null),
                        phone.verifyTokenOpt().orElse(null)
                ));
    }
    
    /**
     * Obtiene la configuración de Twilio para un cliente específico
     * 
     * @param clientId ID del cliente
     * @return Configuración de Twilio o Optional.empty() si no está configurado
     */
    public Optional<TwilioConfig> getTwilioConfig(UuidId<Client> clientId) {
        return clientPhoneRepository.findByClient(clientId)
                .stream()
                .filter(phone -> "TWILIO".equalsIgnoreCase(phone.provider()))
                .filter(phone -> phone.twilioAccountSidOpt().isPresent())
                .findFirst()
                .map(phone -> new TwilioConfig(
                        phone.twilioAccountSidOpt().orElse(null),
                        phone.twilioAuthTokenOpt().orElse(null),
                        phone.apiBaseUrlOpt().orElse("https://api.twilio.com/2010-04-01"),
                        phone.webhookUrlOpt().orElse(null),
                        phone.verifyTokenOpt().orElse(null)
                ));
    }
    
    /**
     * Obtiene la configuración de WWebJs para un cliente específico
     * 
     * @param clientId ID del cliente
     * @return Configuración de WWebJs o Optional.empty() si no está configurado
     */
    public Optional<WWebJsConfig> getWWebJsConfig(UuidId<Client> clientId) {
        return clientPhoneRepository.findByClient(clientId)
                .stream()
                .filter(phone -> "WWEBJS".equalsIgnoreCase(phone.provider()))
                .filter(phone -> phone.wwebjsSessionIdOpt().isPresent())
                .findFirst()
                .map(phone -> new WWebJsConfig(
                        phone.wwebjsSessionIdOpt().orElse(null),
                        phone.wwebjsWebhookUrlOpt().orElse(null),
                        phone.apiBaseUrlOpt().orElse(null),
                        phone.webhookUrlOpt().orElse(null),
                        phone.verifyTokenOpt().orElse(null)
                ));
    }
    
    /**
     * Configuración de Meta WhatsApp
     */
    public record MetaWhatsAppConfig(
            String accessToken,
            String phoneNumberId,
            String apiVersion,
            String apiBaseUrl,
            String webhookUrl,
            String verifyToken
    ) {}
    
    /**
     * Configuración de Twilio
     */
    public record TwilioConfig(
            String accountSid,
            String authToken,
            String apiBaseUrl,
            String webhookUrl,
            String verifyToken
    ) {}
    
    /**
     * Configuración de WWebJs
     */
    public record WWebJsConfig(
            String sessionId,
            String webhookUrl,
            String apiBaseUrl,
            String generalWebhookUrl,
            String verifyToken
    ) {}
}
