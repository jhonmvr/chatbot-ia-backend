package com.relative.chat.bot.ia.infrastructure.config;

import com.relative.chat.bot.ia.infrastructure.filters.ApiKeyAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuración de seguridad para el filtro de autenticación
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    
    /**
     * Registra el filtro de autenticación
     * El filtro solo se aplica a /api/v1/* y excluye endpoints públicos
     */
    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilterRegistration() {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(apiKeyAuthenticationFilter);
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("apiKeyAuthenticationFilter");
        return registration;
    }
}

