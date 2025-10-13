package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.dto.SendResponse;
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.dto.SendTemplateRequest;
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.dto.SendTextRequest;
import com.relative.chat.bot.ia.infrastructure.config.WWebJsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "wwebjs")
public class WWebJsWhatsAppAdapter implements WhatsAppService {

    private final RestClient wwebjsRestClient;
    private final WWebJsProperties props;

    @Override
    public String sendMessage(String from, String to, String message) {
        var request = new SendTextRequest(from, to, message);
        try {
            SendResponse resp = wwebjsRestClient.post()
                    .uri(props.getSendTextPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SendResponse.class);
            String id = resp != null ? resp.id() : null;
            log.info("wwebjs sendMessage OK -> id={}", id);
            return id;
        } catch (RestClientException ex) {
            log.error("wwebjs sendMessage error: {}", ex.getMessage(), ex);
            throw ex; // o mapear a tu excepción de dominio
        }
    }

    @Override
    public String sendTemplate(String from, String to, String templateId, Map<String, String> parameters) {
        var request = new SendTemplateRequest(from, to, templateId, parameters);
        try {
            SendResponse resp = wwebjsRestClient.post()
                    .uri(props.getSendTemplatePath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SendResponse.class);
            String id = resp != null ? resp.id() : null;
            log.info("wwebjs sendTemplate OK -> id={}", id);
            return id;
        } catch (RestClientException ex) {
            log.error("wwebjs sendTemplate error: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
