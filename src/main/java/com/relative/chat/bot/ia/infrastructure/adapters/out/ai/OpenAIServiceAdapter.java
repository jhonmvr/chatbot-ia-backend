package com.relative.chat.bot.ia.infrastructure.adapters.out.ai;

import com.relative.chat.bot.ia.application.ports.out.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementación de AIService usando Spring AI con OpenAI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIServiceAdapter implements AIService {
    
    private final ChatClient.Builder chatClientBuilder;
    private static final Pattern SMALL_TALK = Pattern.compile(
            "\\b(hola|buen[ao]s? (días?|tardes?|noches?)|hey|qué tal|como estas|¿me ayudas|tengo una pregunta|ayuda|gracias|ok|listo|de acuerdo|entendido|hola\\s*,?\\s*tengo una pregunta)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final String SYSTEM_PROMPT = """
            Eres un asistente de preguntas y respuestas ESTRICTO basado en recuperación (RAG).
                
                REGLAS OBLIGATORIAS
                1) Responde ÚNICAMENTE con información dentro de <CONTEXT>.
                2) Si el contexto NO contiene la respuesta, responde exactamente:
                   "No encontrado en el contexto. ¿Quieres aclarar la pregunta?"
                3) No inventes datos ni uses conocimiento externo. No completes lagunas.
                4) Responde en español, breve y directo (máx. 3–4 oraciones).
                5) Usa el HISTORIAL solo si es relevante y NO contradice el CONTEXTO.
                6) Siempre responde con palabras de gentilesa y agradecimiento.
                
                PROCESO DE RAZONAMIENTO (aplícalo SIEMPRE antes de responder)
                A) Normaliza consulta y contexto: pasa a minúsculas, elimina tildes (ubicacion=ubicación), ignora puntuación y plurales/singulares.
                B) Indexa el CONTEXTO en ENTRADAS: cada línea o viñeta es una ENTRADA con:
                   - titulo (primeras palabras hasta “:”), p. ej. “ubicación sede cutuglagua”
                   - cuerpo (lo que sigue), p. ej. URL, instrucciones o texto
                   - entidades: nombres propios presentes en el titulo o cuerpo (p. ej., “cutuglagua”)
                   - alias: genera automáticamente sinónimos y variantes simples del titulo:\s
                       {ubicacion, ubicaciones, direccion, sede, sedes, mapa, como llegar},
                       {pago, pagos, medio de pago, formas de pago, transferencia, efectivo},
                       {agendar, agendamiento, agenda, turno, cita, reservar},
                       {posponer, reprogramar, cambiar cita, cancelar, anular},
                       {recordatorio, plantilla, horarios vigentes}
                   - Además, crea alias a partir de palabras clave del propio titulo/cuerpo (p. ej., si aparece “cutuglagua”, añade “cutuglagua” como alias).
                C) Empareja la PREGUNTA con la ENTRADA más específica:
                   - Coincidencia por alias/entidad: si la pregunta menciona una entidad o alias de una ENTRADA (p. ej. “sedes”, “ubicaciones”, “cutuglagua”), selecciona esa ENTRADA.
                   - Si la pregunta es genérica (“ubicaciones”, “sedes”, “medios de pago”) y hay varias ENTRADAS del mismo tipo, devuelve una lista breve con cada ENTRADA relevante (separadas por “ | ”).
                   - Si hay empate, elige la ENTRADA cuyo titulo sea más específico (más términos coincidentes) o que contenga una entidad mencionada explícitamente.
                D) Si tras este proceso no hay coincidencia clara, usa el mensaje de fallback del punto (2).
                
                ESTILO DE RESPUESTA
                - Cita SOLO el texto necesario de la(s) ENTRADA(S) elegida(s).
                - Si hay enlace en la ENTRADA, inclúyelo tal cual.
                - 1–2 oraciones cuando sea posible.
                
                EJEMPLOS CANÓNICOS (aplica el proceso anterior)
                P: "ubicaciones"
                R: "Ubicación sede Cutuglagua: https://maps.app.goo.gl/ziPWfEUHYwPMEhXJ8"
                
                P: "¿dónde queda la sede de Cutuglagua?"
                R: "Ubicación sede Cutuglagua: https://maps.app.goo.gl/ziPWfEUHYwPMEhXJ8"
                
                P: "¿cómo agendo un turno?"
                R: "Agendamiento de turno en línea: https://agenda.induesa.com/index.php?ac=agendar&cda_id=106"
                
                P: "¿qué medios de pago aceptan?"
                R: "Medios de pago: efectivo en ventanillas; transferencias banco Pichincha (según fechas indicadas internamente)."
                
                P: "¿puedo reprogramar o cancelar?"
                R: "Posponer/cancelar cita: solicitar placa, fecha/hora original y nueva fecha/hora deseada."
                                
                P: "informacion"
                R: "¿Que iformacion necesitas?"                        
        """;

    @Override
    public String generateResponse(
            String userMessage,
            List<String> context,
            List<Map<String, String>> conversationHistory
    ) {
        try {
            if (SMALL_TALK.matcher(userMessage.trim()).find()) {
                return "¡Hola! Claro, dime tu consulta.";
            }
            // Construir el prompt con contexto
            String contextStr = buildContextString(context);
            String historyStr = buildHistoryString(conversationHistory);

            String enhancedPrompt = String.format("""
                <CONTEXT>
                %s
                </CONTEXT>

                <HISTORIAL>
                %s
                </HISTORIAL>
                
                <PREGUNTA>
                %s
                </PREGUNTA>
                
                <INSTRUCCIONES_DE_RESPUESTA>
                - Primero, verifica si la respuesta está explícitamente cubierta por el CONTEXTO.
                - Segundo, siempre se gentil y amable
                </INSTRUCCIONES_DE_RESPUESTA>
                """, contextStr, historyStr, userMessage);

            ChatClient chatClient = chatClientBuilder
                    .defaultOptions(OpenAiChatOptions.builder()
                            .temperature(0.0)       // minimizar divagación
                            .build())
                    .build();
            log.info("chat: {}", enhancedPrompt);
            String response = chatClient
                    .prompt(new PromptTemplate("{msg}").create(Map.of("msg", enhancedPrompt)))
                    .system(SYSTEM_PROMPT)
                    .call()
                    .content();
            
            log.info("Respuesta generada exitosamente para mensaje: {}", 
                    userMessage.substring(0, Math.min(50, userMessage.length())));
            
            return response != null ? response : "Lo siento, no pude generar una respuesta.";
            
        } catch (ResourceAccessException e) {
            // Error de conexión (timeout, connection reset, etc.)
            Throwable cause = e.getCause();
            if (cause instanceof SocketException || 
                (cause != null && cause.getMessage() != null && 
                 (cause.getMessage().contains("Connection reset") || 
                  cause.getMessage().contains("timeout") ||
                  cause.getMessage().contains("Connection refused")))) {
                log.error("Error de conexión con OpenAI API: {}. Verificando conectividad de red.", e.getMessage());
                // Intentar proporcionar una respuesta basada en el contexto si está disponible
                if (context != null && !context.isEmpty()) {
                    log.info("Intentando proporcionar respuesta basada en contexto debido a error de conexión");
                    return generateFallbackResponse(context, userMessage);
                }
                return "Lo siento, estoy teniendo problemas de conexión en este momento. Por favor, intenta de nuevo en unos momentos.";
            }
            log.error("Error de acceso a recursos de OpenAI: {}", e.getMessage(), e);
            return "Lo siento, ocurrió un error al procesar tu mensaje. ¿Puedes intentar de nuevo?";
        } catch (Exception e) {
            log.error("Error al generar respuesta con IA: {}", e.getMessage(), e);
            // Si hay contexto disponible, intentar proporcionar una respuesta básica
            if (context != null && !context.isEmpty()) {
                log.info("Intentando proporcionar respuesta basada en contexto debido a error general");
                return generateFallbackResponse(context, userMessage);
            }
            return "Lo siento, ocurrió un error al procesar tu mensaje. ¿Puedes intentar de nuevo?";
        }
    }
    
    @Override
    public String generateSimpleResponse(String userMessage) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            
            String response = chatClient.prompt()
                    .system("Eres un asistente virtual amigable y útil.")
                    .user(userMessage)
                    .call()
                    .content();
            
            return response != null ? response : "Lo siento, no pude generar una respuesta.";
            
        } catch (Exception e) {
            log.error("Error al generar respuesta simple: {}", e.getMessage(), e);
            return "Lo siento, ocurrió un error. ¿Puedes intentar de nuevo?";
        }
    }
    
    /**
     * Construye una cadena de texto con el contexto relevante
     */
    private String buildContextString(List<String> context) {
        if (context == null || context.isEmpty()) {
            return "No hay contexto disponible.";
        }
        
        return context.stream()
                .limit(5) // Limitar a los 5 documentos más relevantes
                .map(doc -> "- " + doc)
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Construye una cadena de texto con el historial de conversación
     */
    private String buildHistoryString(List<Map<String, String>> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "Esta es una nueva conversación.";
        }
        
        return conversationHistory.stream()
                .limit(10) // Limitar a los últimos 10 mensajes
                .map(entry -> {
                    String role = entry.getOrDefault("role", "UNKNOWN");
                    String content = entry.getOrDefault("content", "");
                    String roleLabel = role.equals("IN") ? "Usuario" : "Asistente";
                    return String.format("%s: %s", roleLabel, content);
                })
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Genera una respuesta de fallback basada en el contexto cuando hay problemas de conexión
     * Intenta extraer información relevante del contexto sin usar la IA
     */
    private String generateFallbackResponse(List<String> context, String userMessage) {
        if (context == null || context.isEmpty()) {
            return "Lo siento, no tengo información disponible en este momento. Por favor, intenta más tarde.";
        }
        
        // Normalizar la pregunta del usuario para búsqueda simple
        String normalizedQuery = userMessage.toLowerCase().trim();
        
        // Buscar coincidencias simples en el contexto
        for (String contextItem : context) {
            String normalizedContext = contextItem.toLowerCase();
            // Si la pregunta contiene palabras clave del contexto o viceversa
            if (normalizedContext.contains(normalizedQuery) || 
                normalizedQuery.contains(contextItem.toLowerCase().split(":")[0].trim()) ||
                contextItem.toLowerCase().contains(normalizedQuery.split("\\s+")[0])) {
                // Extraer información relevante
                String response = contextItem.trim();
                // Limitar la longitud de la respuesta
                if (response.length() > 200) {
                    response = response.substring(0, 197) + "...";
                }
                return "Basándome en la información disponible: " + response;
            }
        }
        
        // Si no hay coincidencia exacta, devolver el primer elemento del contexto
        String firstContext = context.get(0).trim();
        if (firstContext.length() > 200) {
            firstContext = firstContext.substring(0, 197) + "...";
        }
        return "Basándome en la información disponible: " + firstContext;
    }
}

