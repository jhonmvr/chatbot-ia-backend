package com.relative.chat.bot.ia.domain.messaging;

import java.util.List;
import java.util.Optional;

/**
 * Componente de plantilla de WhatsApp
 */
public record TemplateComponent(
    /**
     * Tipo de componente
     */
    ComponentType type,
    
    /**
     * Texto del componente (para HEADER, BODY, FOOTER)
     */
    String text,
    
    /**
     * Parámetros del componente
     */
    List<ComponentParameter> parameters,
    
    /**
     * Botones del componente (para tipo BUTTONS)
     */
    List<TemplateButton> buttons,
    
    /**
     * Componente multimedia (para HEADER con imagen/video)
     */
    MediaComponent media
) {
    /**
     * Constructor para componente de texto
     */
    public static TemplateComponent textComponent(ComponentType type, String text, List<ComponentParameter> parameters) {
        return new TemplateComponent(type, text, parameters, null, null);
    }
    
    /**
     * Constructor para componente de botones
     */
    public static TemplateComponent buttonComponent(List<TemplateButton> buttons) {
        return new TemplateComponent(ComponentType.BUTTONS, null, null, buttons, null);
    }
    
    /**
     * Constructor para componente multimedia
     */
    public static TemplateComponent mediaComponent(ComponentType type, MediaComponent media) {
        return new TemplateComponent(type, null, null, null, media);
    }
    
    /**
     * Obtiene parámetros como Optional
     */
    public Optional<List<ComponentParameter>> parametersOpt() {
        return Optional.ofNullable(parameters);
    }
    
    /**
     * Obtiene botones como Optional
     */
    public Optional<List<TemplateButton>> buttonsOpt() {
        return Optional.ofNullable(buttons);
    }
    
    /**
     * Obtiene media como Optional
     */
    public Optional<MediaComponent> mediaOpt() {
        return Optional.ofNullable(media);
    }
}
