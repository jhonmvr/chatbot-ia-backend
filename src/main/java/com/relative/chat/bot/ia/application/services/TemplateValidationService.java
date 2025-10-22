package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio para validación de plantillas de WhatsApp
 */
@Slf4j
@Service
public class TemplateValidationService {
    
    // Patrones de validación según documentación de Meta
    private static final Pattern TEMPLATE_NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile("^[a-z]{2}_[A-Z]{2}$");
    
    // Límites según Meta API
    private static final int MAX_TEMPLATE_NAME_LENGTH = 512;
    private static final int MAX_HEADER_TEXT_LENGTH = 60;
    private static final int MAX_BODY_TEXT_LENGTH = 1024;
    private static final int MAX_FOOTER_TEXT_LENGTH = 60;
    private static final int MAX_BUTTON_TEXT_LENGTH = 25;
    private static final int MAX_BUTTONS_COUNT = 3;
    private static final int MAX_PARAMETERS_COUNT = 10;
    
    /**
     * Valida una plantilla completa
     */
    public ValidationResult validateTemplate(WhatsAppTemplate template) {
        log.debug("Validando plantilla: {}", template.name());
        
        ValidationResult result = new ValidationResult();
        
        // Validar nombre
        validateTemplateName(template.name(), result);
        
        // Validar idioma
        validateLanguage(template.language(), result);
        
        // Validar categoría
        validateCategory(template.category(), result);
        
        // Validar componentes
        validateComponents(template.components(), template.category(), result);
        
        // Validar formato de parámetros
        validateParameterFormat(template.parameterFormat(), template.components(), result);
        
        return result;
    }
    
    /**
     * Valida el nombre de la plantilla
     */
    private void validateTemplateName(String name, ValidationResult result) {
        if (name == null || name.trim().isEmpty()) {
            result.addError("El nombre de la plantilla es requerido");
            return;
        }
        
        if (name.length() > MAX_TEMPLATE_NAME_LENGTH) {
            result.addError("El nombre de la plantilla no puede exceder " + MAX_TEMPLATE_NAME_LENGTH + " caracteres");
        }
        
        if (!TEMPLATE_NAME_PATTERN.matcher(name).matches()) {
            result.addError("El nombre de la plantilla solo puede contener letras minúsculas, números y guiones bajos");
        }
    }
    
    /**
     * Valida el código de idioma
     */
    private void validateLanguage(String language, ValidationResult result) {
        if (language == null || language.trim().isEmpty()) {
            result.addError("El código de idioma es requerido");
            return;
        }
        
        if (!LANGUAGE_CODE_PATTERN.matcher(language).matches()) {
            result.addError("El código de idioma debe estar en formato ISO 639-1 (ej: es_ES, en_US)");
        }
    }
    
    /**
     * Valida la categoría
     */
    private void validateCategory(TemplateCategory category, ValidationResult result) {
        if (category == null) {
            result.addError("La categoría de la plantilla es requerida");
        }
    }
    
    /**
     * Valida los componentes de la plantilla
     */
    private void validateComponents(List<TemplateComponent> components, TemplateCategory category, ValidationResult result) {
        if (components == null || components.isEmpty()) {
            result.addError("La plantilla debe tener al menos un componente");
            return;
        }
        
        // Validar estructura básica
        validateBasicComponentStructure(components, result);
        
        // Validar cada componente individualmente
        for (int i = 0; i < components.size(); i++) {
            TemplateComponent component = components.get(i);
            validateComponent(component, i, result);
        }
        
        // Validaciones específicas por categoría
        validateCategorySpecificComponents(components, category, result);
    }
    
    /**
     * Valida la estructura básica de componentes
     */
    private void validateBasicComponentStructure(List<TemplateComponent> components, ValidationResult result) {
        long headerCount = components.stream().filter(c -> c.type() == ComponentType.HEADER).count();
        long bodyCount = components.stream().filter(c -> c.type() == ComponentType.BODY).count();
        long footerCount = components.stream().filter(c -> c.type() == ComponentType.FOOTER).count();
        long buttonsCount = components.stream().filter(c -> c.type() == ComponentType.BUTTONS).count();
        
        if (headerCount > 1) {
            result.addError("Solo puede haber un componente HEADER");
        }
        
        if (bodyCount > 1) {
            result.addError("Solo puede haber un componente BODY");
        }
        
        if (footerCount > 1) {
            result.addError("Solo puede haber un componente FOOTER");
        }
        
        if (buttonsCount > 1) {
            result.addError("Solo puede haber un componente BUTTONS");
        }
        
        if (bodyCount == 0) {
            result.addError("La plantilla debe tener un componente BODY");
        }
    }
    
    /**
     * Valida un componente individual
     */
    private void validateComponent(TemplateComponent component, int index, ValidationResult result) {
        switch (component.type()) {
            case HEADER -> validateHeaderComponent(component, index, result);
            case BODY -> validateBodyComponent(component, index, result);
            case FOOTER -> validateFooterComponent(component, index, result);
            case BUTTONS -> validateButtonsComponent(component, index, result);
        }
    }
    
    /**
     * Valida componente HEADER
     */
    private void validateHeaderComponent(TemplateComponent component, int index, ValidationResult result) {
        if (component.text() != null && component.text().length() > MAX_HEADER_TEXT_LENGTH) {
            result.addError("El texto del HEADER no puede exceder " + MAX_HEADER_TEXT_LENGTH + " caracteres");
        }
        
        if (component.media() != null) {
            validateMediaComponent(component.media(), "HEADER", result);
        }
        
        if (component.parameters() != null && !component.parameters().isEmpty()) {
            result.addError("El componente HEADER no puede tener parámetros");
        }
        
        if (component.buttons() != null && !component.buttons().isEmpty()) {
            result.addError("El componente HEADER no puede tener botones");
        }
    }
    
    /**
     * Valida componente BODY
     */
    private void validateBodyComponent(TemplateComponent component, int index, ValidationResult result) {
        if (component.text() == null || component.text().trim().isEmpty()) {
            result.addError("El componente BODY debe tener texto");
            return;
        }
        
        if (component.text().length() > MAX_BODY_TEXT_LENGTH) {
            result.addError("El texto del BODY no puede exceder " + MAX_BODY_TEXT_LENGTH + " caracteres");
        }
        
        if (component.media() != null) {
            result.addError("El componente BODY no puede tener media");
        }
        
        if (component.buttons() != null && !component.buttons().isEmpty()) {
            result.addError("El componente BODY no puede tener botones");
        }
        
        // Validar parámetros
        if (component.parameters() != null) {
            validateParameters(component.parameters(), result);
        }
    }
    
    /**
     * Valida componente FOOTER
     */
    private void validateFooterComponent(TemplateComponent component, int index, ValidationResult result) {
        if (component.text() != null && component.text().length() > MAX_FOOTER_TEXT_LENGTH) {
            result.addError("El texto del FOOTER no puede exceder " + MAX_FOOTER_TEXT_LENGTH + " caracteres");
        }
        
        if (component.media() != null) {
            result.addError("El componente FOOTER no puede tener media");
        }
        
        if (component.parameters() != null && !component.parameters().isEmpty()) {
            result.addError("El componente FOOTER no puede tener parámetros");
        }
        
        if (component.buttons() != null && !component.buttons().isEmpty()) {
            result.addError("El componente FOOTER no puede tener botones");
        }
    }
    
    /**
     * Valida componente BUTTONS
     */
    private void validateButtonsComponent(TemplateComponent component, int index, ValidationResult result) {
        if (component.buttons() == null || component.buttons().isEmpty()) {
            result.addError("El componente BUTTONS debe tener al menos un botón");
            return;
        }
        
        if (component.buttons().size() > MAX_BUTTONS_COUNT) {
            result.addError("No puede haber más de " + MAX_BUTTONS_COUNT + " botones");
        }
        
        if (component.text() != null) {
            result.addError("El componente BUTTONS no puede tener texto");
        }
        
        if (component.media() != null) {
            result.addError("El componente BUTTONS no puede tener media");
        }
        
        if (component.parameters() != null && !component.parameters().isEmpty()) {
            result.addError("El componente BUTTONS no puede tener parámetros");
        }
        
        // Validar cada botón
        for (int i = 0; i < component.buttons().size(); i++) {
            validateButton(component.buttons().get(i), i, result);
        }
    }
    
    /**
     * Valida un botón individual
     */
    private void validateButton(TemplateButton button, int index, ValidationResult result) {
        if (button.text() == null || button.text().trim().isEmpty()) {
            result.addError("El botón " + index + " debe tener texto");
            return;
        }
        
        if (button.text().length() > MAX_BUTTON_TEXT_LENGTH) {
            result.addError("El texto del botón " + index + " no puede exceder " + MAX_BUTTON_TEXT_LENGTH + " caracteres");
        }
        
        switch (button.type()) {
            case "URL" -> {
                if (button.url() == null || button.url().trim().isEmpty()) {
                    result.addError("El botón URL " + index + " debe tener una URL");
                } else if (!isValidUrl(button.url())) {
                    result.addError("La URL del botón " + index + " no es válida");
                }
            }
            case "PHONE_NUMBER" -> {
                if (button.phoneNumber() == null || button.phoneNumber().trim().isEmpty()) {
                    result.addError("El botón PHONE_NUMBER " + index + " debe tener un número de teléfono");
                } else if (!isValidPhoneNumber(button.phoneNumber())) {
                    result.addError("El número de teléfono del botón " + index + " no es válido");
                }
            }
            case "QUICK_REPLY" -> {
                // Los botones de respuesta rápida no necesitan URL ni teléfono
            }
            default -> result.addError("Tipo de botón no válido: " + button.type());
        }
    }
    
    /**
     * Valida componente multimedia
     */
    private void validateMediaComponent(MediaComponent media, String componentType, ValidationResult result) {
        if (media.type() == null) {
            result.addError("El componente " + componentType + " con media debe especificar el tipo");
            return;
        }
        
        switch (media.type()) {
            case "IMAGE" -> {
                if (media.url() == null && media.mediaId() == null) {
                    result.addError("El componente " + componentType + " con imagen debe tener URL o mediaId");
                }
            }
            case "VIDEO" -> {
                if (media.url() == null && media.mediaId() == null) {
                    result.addError("El componente " + componentType + " con video debe tener URL o mediaId");
                }
            }
            case "DOCUMENT" -> {
                if (media.url() == null && media.mediaId() == null) {
                    result.addError("El componente " + componentType + " con documento debe tener URL o mediaId");
                }
                if (media.filename() == null) {
                    result.addError("El componente " + componentType + " con documento debe especificar el nombre del archivo");
                }
            }
            default -> result.addError("Tipo de media no válido: " + media.type());
        }
    }
    
    /**
     * Valida parámetros
     */
    private void validateParameters(List<ComponentParameter> parameters, ValidationResult result) {
        if (parameters.size() > MAX_PARAMETERS_COUNT) {
            result.addError("No puede haber más de " + MAX_PARAMETERS_COUNT + " parámetros");
        }
        
        for (int i = 0; i < parameters.size(); i++) {
            ComponentParameter param = parameters.get(i);
            validateParameter(param, i, result);
        }
    }
    
    /**
     * Valida un parámetro individual
     */
    private void validateParameter(ComponentParameter parameter, int index, ValidationResult result) {
        if (parameter.type() == null || parameter.type().trim().isEmpty()) {
            result.addError("El parámetro " + index + " debe tener tipo");
        }
        
        if (parameter.text() == null || parameter.text().trim().isEmpty()) {
            result.addError("El parámetro " + index + " debe tener texto");
        }
        
        if (parameter.example() == null || parameter.example().trim().isEmpty()) {
            result.addError("El parámetro " + index + " debe tener un ejemplo");
        }
    }
    
    /**
     * Valida formato de parámetros
     */
    private void validateParameterFormat(ParameterFormat format, List<TemplateComponent> components, ValidationResult result) {
        if (format == null) {
            return; // Formato opcional
        }
        
        // Verificar que todos los parámetros usen el formato especificado
        for (TemplateComponent component : components) {
            if (component.parameters() != null) {
                for (ComponentParameter param : component.parameters()) {
                    if (format == ParameterFormat.NAMED && param.parameterName() == null) {
                        result.addError("Con formato NAMED, todos los parámetros deben tener parameterName");
                    }
                }
            }
        }
    }
    
    /**
     * Validaciones específicas por categoría
     */
    private void validateCategorySpecificComponents(List<TemplateComponent> components, TemplateCategory category, ValidationResult result) {
        switch (category) {
            case AUTHENTICATION -> validateAuthenticationTemplate(components, result);
            case MARKETING -> validateMarketingTemplate(components, result);
            case UTILITY -> validateUtilityTemplate(components, result);
        }
    }
    
    /**
     * Valida plantilla de autenticación
     */
    private void validateAuthenticationTemplate(List<TemplateComponent> components, ValidationResult result) {
        // Debe tener BODY con parámetros
        boolean hasBodyWithParams = components.stream()
            .anyMatch(comp -> comp.type() == ComponentType.BODY && 
                comp.parameters() != null && !comp.parameters().isEmpty());
        
        if (!hasBodyWithParams) {
            result.addError("Las plantillas de autenticación deben tener un componente BODY con parámetros");
        }
        
        // No puede tener botones de URL
        boolean hasUrlButtons = components.stream()
            .anyMatch(comp -> comp.type() == ComponentType.BUTTONS && 
                comp.buttons() != null && 
                comp.buttons().stream().anyMatch(btn -> "URL".equals(btn.type())));
        
        if (hasUrlButtons) {
            result.addError("Las plantillas de autenticación no pueden tener botones de URL");
        }
    }
    
    /**
     * Valida plantilla de marketing
     */
    private void validateMarketingTemplate(List<TemplateComponent> components, ValidationResult result) {
        // Las plantillas de marketing pueden tener HEADER multimedia
        boolean hasHeaderWithMedia = components.stream()
            .anyMatch(comp -> comp.type() == ComponentType.HEADER && comp.media() != null);
        
        if (hasHeaderWithMedia) {
            // Validar que el media sea imagen o video
            components.stream()
                .filter(comp -> comp.type() == ComponentType.HEADER && comp.media() != null)
                .forEach(comp -> {
                    if (!"IMAGE".equals(comp.media().type()) && !"VIDEO".equals(comp.media().type())) {
                        result.addError("Las plantillas de marketing solo pueden tener imágenes o videos en el HEADER");
                    }
                });
        }
    }
    
    /**
     * Valida plantilla de utilidad
     */
    private void validateUtilityTemplate(List<TemplateComponent> components, ValidationResult result) {
        // Las plantillas de utilidad no pueden tener HEADER multimedia
        boolean hasHeaderWithMedia = components.stream()
            .anyMatch(comp -> comp.type() == ComponentType.HEADER && comp.media() != null);
        
        if (hasHeaderWithMedia) {
            result.addError("Las plantillas de utilidad no pueden tener HEADER multimedia");
        }
    }
    
    /**
     * Valida URL
     */
    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Valida número de teléfono
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Formato E.164: +[código de país][número]
        return phoneNumber.startsWith("+") && phoneNumber.length() >= 8 && phoneNumber.length() <= 15;
    }
    
    /**
     * Resultado de validación
     */
    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public java.util.List<String> getErrors() {
            return new java.util.ArrayList<>(errors);
        }
        
        public java.util.List<String> getWarnings() {
            return new java.util.ArrayList<>(warnings);
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
        
        public String getWarningMessage() {
            return String.join("; ", warnings);
        }
    }
}
