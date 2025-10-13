package com.relative.chat.bot.ia.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
    
    @Value("${app.ai.openai.max-buffer-size:10485760}")
    private int maxBufferSize;  // Default: 10MB (10 * 1024 * 1024)
    
    @Value("${app.ai.openai.connection.max-connections:100}")
    private int maxConnections;
    
    @Value("${app.ai.openai.connection.pending-acquire-timeout:45}")
    private int pendingAcquireTimeoutSeconds;
    
    @Value("${app.ai.openai.connection.max-idle-time:20}")
    private int maxIdleTimeSeconds;
    
    @Value("${app.ai.openai.connection.max-life-time:60}")
    private int maxLifeTimeSeconds;
    
    @Value("${app.ai.openai.connection.eviction-interval:30}")
    private int evictionIntervalSeconds;
    
    /**
     * WebClient configurado específicamente para OpenAI API
     * 
     * Características:
     * - Timeouts configurables (default: 60s)
     * - Buffer de respuesta aumentado (default: 10MB) para embeddings batch
     * - Logging de requests/responses
     * - Connection pooling con gestión de conexiones idle
     * - Timeouts de conexión, lectura y escritura
     * - Keep-alive habilitado
     */
    @Bean
    public WebClient openAiWebClient() {
        // Configurar connection provider con pooling avanzado
        ConnectionProvider connectionProvider = ConnectionProvider.builder("openai-pool")
            .maxConnections(maxConnections)
            .pendingAcquireTimeout(Duration.ofSeconds(pendingAcquireTimeoutSeconds))
            .maxIdleTime(Duration.ofSeconds(maxIdleTimeSeconds))
            .maxLifeTime(Duration.ofSeconds(maxLifeTimeSeconds))
            .evictInBackground(Duration.ofSeconds(evictionIntervalSeconds))
            .build();
        
        // Configurar HttpClient con timeouts completos y connection pooling
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10 segundos para conectar
            .option(ChannelOption.SO_KEEPALIVE, true) // Keep-alive habilitado
            .responseTimeout(Duration.ofSeconds(timeoutSeconds))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
            );
        
        // Configurar estrategias de intercambio con límite de buffer aumentado
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(maxBufferSize);
            })
            .build();
        
        log.info("Configurando WebClient de OpenAI:");
        log.info("  - Buffer máximo: {} bytes ({} MB)", maxBufferSize, maxBufferSize / (1024 * 1024));
        log.info("  - Timeout: {} segundos", timeoutSeconds);
        log.info("  - Max conexiones: {}", maxConnections);
        log.info("  - Max idle time: {} segundos", maxIdleTimeSeconds);
        log.info("  - Max life time: {} segundos", maxLifeTimeSeconds);
        
        return WebClient.builder()
            .baseUrl(openAiApiUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
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

