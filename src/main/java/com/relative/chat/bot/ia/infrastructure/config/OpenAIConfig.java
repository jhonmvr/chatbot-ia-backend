package com.relative.chat.bot.ia.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuración de WebClient para OpenAI API
 */
@Slf4j
@Configuration
public class OpenAIConfig {
    
    @Value("${app.ai.openai.api-url:https://api.openai.com}")
    private String openAiApiUrl;
    
    @Value("${app.ai.openai.timeout:60}")
    private int timeoutSeconds;
    
    /**
     * WebClient configurado específicamente para OpenAI API
     * 
     * Características:
     * - Timeouts configurables (default: 60s)
     * - Logging de requests/responses
     * - Connection pooling
     */
    @Bean
    public WebClient openAiWebClient() {
        // Configurar HttpClient con timeouts simples
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(timeoutSeconds));
        
        return WebClient.builder()
            .baseUrl(openAiApiUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
            .build();
    }
    
    /**
     * Filter para logging de requests
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("OpenAI API Request: {} {}", 
                    clientRequest.method(), clientRequest.url());
            }
            return Mono.just(clientRequest);
        });
    }
    
    /**
     * Filter para logging de responses
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("OpenAI API Response: {} ", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
}

