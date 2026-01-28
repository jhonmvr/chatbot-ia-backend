package com.relative.chat.bot.ia.infrastructure.filters;

import com.relative.chat.bot.ia.application.services.ApiKeyService;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.infrastructure.security.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro para autenticación basada en API Key (token)
 * Intercepta requests a /api/v1/* y valida el token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final ApiKeyService apiKeyService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Solo procesar requests a /api/v1/*
        String path = request.getRequestURI();
        // Manejar tanto con context-path como sin él
        if (!path.contains("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extraer token del header
        String token = extractToken(request);
        
        if (token == null || token.isBlank()) {
            log.warn("Request sin token a endpoint protegido: {} (headers: Authorization={}, X-API-Key={})", 
                path, 
                request.getHeader(AUTHORIZATION_HEADER) != null ? "presente" : "ausente",
                request.getHeader(API_KEY_HEADER) != null ? "presente" : "ausente");
            sendUnauthorizedResponse(response, "Token requerido. Use header 'Authorization: Bearer <token>' o 'X-API-Key: <token>'");
            return;
        }
        
        log.debug("Validando token para path: {} (token length: {})", path, token.length());
        
        // Validar token
        Optional<UuidId<Client>> clientIdOpt = apiKeyService.validateToken(token);
        
        if (clientIdOpt.isEmpty()) {
            log.warn("Token inválido en request a: {} (token preview: {}...)", 
                path, 
                token.length() > 20 ? token.substring(0, 20) : token);
            sendUnauthorizedResponse(response, "Token inválido o expirado");
            return;
        }
        
        // Establecer clientId en el contexto (tanto en request como en RequestContextHolder)
        UuidId<Client> clientId = clientIdOpt.get();
        SecurityUtils.setAuthenticatedClientId(request, clientId);
        
        log.debug("Cliente autenticado: {} para path: {}", clientId.value(), path);
        
        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extrae el token del request
     * Busca en: Authorization: Bearer <token> o X-API-Key: <token>
     */
    private String extractToken(HttpServletRequest request) {
        // Intentar desde Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        
        // Intentar desde X-API-Key header
        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            return apiKeyHeader.trim();
        }
        
        return null;
    }
    
    /**
     * Envía respuesta 401 Unauthorized
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
            "{\"status\":\"error\",\"message\":\"%s\"}",
            message.replace("\"", "\\\"")
        ));
    }
}

