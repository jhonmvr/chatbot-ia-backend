
package com.relative.chat.bot.ia.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.reactive.function.client.ExchangeStrategies;

import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {
    @Bean WebClient aiClient(@Value("${ai.base-url:http://localhost:8000}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(64 * 1024 * 1024))
                        .build())
                .build();

    }
}
