package com.relative.chat.bot.ia.domain.messaging;

import java.util.Optional;

/**
 * Componente multimedia para plantillas de WhatsApp
 */
public record MediaComponent(
    /**
     * Tipo de media (IMAGE, VIDEO, DOCUMENT)
     */
    String type,
    
    /**
     * URL del archivo multimedia
     */
    String url,
    
    /**
     * ID del archivo en Meta (para archivos subidos)
     */
    String mediaId,
    
    /**
     * Nombre del archivo
     */
    String filename,
    
    /**
     * Texto alternativo para accesibilidad
     */
    String altText
) {
    /**
     * Constructor para imagen
     */
    public static MediaComponent image(String url, String altText) {
        return new MediaComponent("IMAGE", url, null, null, altText);
    }
    
    /**
     * Constructor para video
     */
    public static MediaComponent video(String url, String filename) {
        return new MediaComponent("VIDEO", url, null, filename, null);
    }
    
    /**
     * Constructor para documento
     */
    public static MediaComponent document(String url, String filename) {
        return new MediaComponent("DOCUMENT", url, null, filename, null);
    }
    
    /**
     * Constructor para media con ID de Meta
     */
    public static MediaComponent withMediaId(String type, String mediaId, String filename) {
        return new MediaComponent(type, null, mediaId, filename, null);
    }
    
    /**
     * Obtiene URL como Optional
     */
    public Optional<String> urlOpt() {
        return Optional.ofNullable(url);
    }
    
    /**
     * Obtiene mediaId como Optional
     */
    public Optional<String> mediaIdOpt() {
        return Optional.ofNullable(mediaId);
    }
    
    /**
     * Obtiene filename como Optional
     */
    public Optional<String> filenameOpt() {
        return Optional.ofNullable(filename);
    }
    
    /**
     * Obtiene altText como Optional
     */
    public Optional<String> altTextOpt() {
        return Optional.ofNullable(altText);
    }
}
