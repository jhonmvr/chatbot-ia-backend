package com.relative.chat.bot.ia.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wwebjs")
public class WWebJsProperties {
    /**
     * Base URL del microservicio wwebjs (p.ej. http://localhost:8080)
     */
    private String baseUrl = "http://localhost:8080";

    /**
     * Rutas (por si cambian en el futuro)
     */
    private String sendTextPath = "/api/v1/messages/text";
    private String sendTemplatePath = "/api/v1/messages/template";

    /**
     * Timeouts en milisegundos
     */
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 10000;

    // Getters/Setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getSendTextPath() { return sendTextPath; }
    public void setSendTextPath(String sendTextPath) { this.sendTextPath = sendTextPath; }
    public String getSendTemplatePath() { return sendTemplatePath; }
    public void setSendTemplatePath(String sendTemplatePath) { this.sendTemplatePath = sendTemplatePath; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
