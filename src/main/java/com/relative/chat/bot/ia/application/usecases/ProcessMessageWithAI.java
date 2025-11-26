package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.dto.AppointmentIntent;
import com.relative.chat.bot.ia.application.ports.out.AIService;
import com.relative.chat.bot.ia.application.ports.out.EmbeddingsPort;
import com.relative.chat.bot.ia.application.ports.out.VectorStore;
import com.relative.chat.bot.ia.application.services.*;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.MessageRepository;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.domain.ports.scheduling.CalendarProviderAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Caso de uso: Procesar mensaje con IA
 * Busca contexto relevante en el knowledge base y genera una respuesta
 * Integra flujo híbrido para agendamiento (IA + Estado)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessMessageWithAI {
    
    private final EmbeddingsPort embeddingsPort;
    private final VectorStore vectorStore;
    private final AIService aiService;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final AppointmentIntentDetector intentDetector;
    private final AppointmentStateService stateService;
    private final AppointmentAvailabilityService availabilityService;
    private final NaturalDateTimeParser dateTimeParser;
    private final CreateAppointmentFromChat createAppointment;
    private final CalendarProviderAccountRepository accountRepository;
    private final CloseConversation closeConversation;
    
    private static final int TOP_K_RESULTS = 5;
    private static final int MAX_CONVERSATION_HISTORY = 10;
    
    /**
     * Procesa un mensaje del usuario y genera una respuesta usando IA
     * 
     * @param userMessage Mensaje del usuario
     * @param conversationId ID de la conversación
     * @param namespace Namespace del knowledge base
     * @param clientId ID del cliente
     * @param contactId ID del contacto
     * @return Respuesta generada por la IA
     */
    public String handle(
            String userMessage, 
            UuidId<Conversation> conversationId, 
            String namespace,
            UuidId<Client> clientId,
            UuidId<Contact> contactId
    ) {
        try {
            // 1. Verificar si está en modo agendamiento
            boolean inAppointmentMode = stateService.isInAppointmentMode(conversationId);
            log.info("Verificando modo agendamiento para conversación {}: {}", conversationId.value(), inAppointmentMode);
            
            if (inAppointmentMode) {
                log.info("Procesando mensaje en modo agendamiento: {}", userMessage);
                return handleAppointmentFlow(userMessage, conversationId, clientId, contactId);
            }
            
            // 2. Detectar intención de agendamiento
            AppointmentIntent intent = intentDetector.detect(userMessage);
            log.info("Intención de agendamiento detectada: {}", intent.hasIntent());
            
            if (intent.hasIntent()) {
                // Iniciar flujo de agendamiento
                log.info("Iniciando flujo de agendamiento para conversación: {}", conversationId.value());
                return startAppointmentFlow(conversationId, clientId);
            }
            
            // 3. Flujo normal con IA
            log.debug("Procesando mensaje con flujo normal de IA");
            return handleNormalFlow(userMessage, conversationId, namespace);
            
        } catch (Exception e) {
            log.error("Error al procesar mensaje con IA: {}", e.getMessage(), e);
            return "Lo siento, ocurrió un error al procesar tu mensaje. ¿Puedes intentar de nuevo?";
        }
    }
    
    /**
     * Método legacy para compatibilidad (sin clientId y contactId)
     */
    public String handle(String userMessage, UuidId<Conversation> conversationId, String namespace) {
        return handleNormalFlow(userMessage, conversationId, namespace);
    }
    
    /**
     * Inicia el flujo de agendamiento
     */
    private String startAppointmentFlow(UuidId<Conversation> conversationId, UuidId<Client> clientId) {
        // Verificar si hay cuenta de calendario configurada
        Optional<CalendarProviderAccount> accountOpt = accountRepository
                .findActiveByClientIdAndProvider(clientId, CalendarProvider.GOOGLE);
        
        if (accountOpt.isEmpty()) {
            accountOpt = accountRepository.findActiveByClientIdAndProvider(clientId, CalendarProvider.OUTLOOK);
        }
        
        if (accountOpt.isEmpty()) {
            return "Lo siento, no hay calendario configurado para agendamientos. " +
                   "Por favor, contacta con el administrador.";
        }
        
        CalendarProviderAccount account = accountOpt.get();
        
        // Verificar si hay disponibilidad configurada
        AvailabilityConfigService configService = new AvailabilityConfigService(accountRepository);
        com.relative.chat.bot.ia.application.dto.AvailabilityConfig config = 
                configService.getAvailabilityConfig(account.id());
        
        if (!config.enabled()) {
            return "Lo siento, el agendamiento no está disponible en este momento. " +
                   "Por favor, contacta directamente.";
        }
        
        // Iniciar estado de agendamiento
        stateService.startAppointmentFlow(conversationId);
        
        return "¡Por supuesto! ¿Qué día te conviene? Puedes decirme el día de la semana o la fecha " +
               "(ej: viernes, mañana, 25 de noviembre).";
    }
    
    /**
     * Maneja el flujo de agendamiento
     */
    private String handleAppointmentFlow(
            String userMessage,
            UuidId<Conversation> conversationId,
            UuidId<Client> clientId,
            UuidId<Contact> contactId
    ) {
        Optional<AppointmentStateService.AppointmentState> stateOpt = 
                stateService.getState(conversationId);
        
        if (stateOpt.isEmpty()) {
            return "Error en el flujo de agendamiento. Por favor, intenta de nuevo.";
        }
        
        AppointmentStateService.AppointmentState state = stateOpt.get();
        String step = state.step();
        
        // Obtener cuenta de calendario
        Optional<CalendarProviderAccount> accountOpt = accountRepository
                .findActiveByClientIdAndProvider(clientId, CalendarProvider.GOOGLE);
        
        if (accountOpt.isEmpty()) {
            accountOpt = accountRepository.findActiveByClientIdAndProvider(clientId, CalendarProvider.OUTLOOK);
        }
        
        if (accountOpt.isEmpty()) {
            stateService.clearState(conversationId);
            return "Error: No se encontró cuenta de calendario.";
        }
        
        CalendarProviderAccount account = accountOpt.get();
        
        switch (step) {
            case "collecting_date":
                return handleDateCollection(userMessage, conversationId, account, state);
                
            case "collecting_time":
                return handleTimeCollection(userMessage, conversationId, account, state);
                
            case "confirming":
                return handleConfirmation(userMessage, conversationId, account, clientId, contactId, state);
                
            default:
                stateService.clearState(conversationId);
                return "Error en el flujo de agendamiento. Por favor, intenta de nuevo.";
        }
    }
    
    private String handleDateCollection(
            String userMessage,
            UuidId<Conversation> conversationId,
            CalendarProviderAccount account,
            AppointmentStateService.AppointmentState state
    ) {
        log.info("📅 Procesando recolección de fecha: '{}'", userMessage);
        
        // Intentar parsear fecha y hora del mismo mensaje
        LocalDate date = dateTimeParser.parseDate(userMessage);
        LocalTime time = dateTimeParser.parseTime(userMessage);
        
        log.info("📅 Fecha parseada: {}, Hora parseada: {}", date, time);
        
        if (date == null) {
            log.warn("⚠️ No se pudo parsear la fecha del mensaje: '{}'", userMessage);
            return "No entendí la fecha. ¿Puedes decirme el día? " +
                   "(ej: viernes, mañana, 25 de noviembre, la próxima semana)";
        }
        
        // Si el usuario proporcionó fecha Y hora en el mismo mensaje
        if (time != null) {
            LocalDateTime dateTime = LocalDateTime.of(date, time);
            
            // Verificar disponibilidad específica
            if (!availabilityService.isSlotAvailable(account, dateTime)) {
                List<AppointmentAvailabilityService.TimeSlot> slots = 
                        availabilityService.getAvailableSlots(account, date);
                return String.format(
                        "❌ El horario %s del %s no está disponible.\n\n%s",
                        formatTime(time),
                        formatDate(date),
                        availabilityService.formatSlotsForWhatsApp(slots)
                );
            }
            
            // Si está disponible, avanzar directamente a confirmación
            stateService.setDate(conversationId, date);
            stateService.setTime(conversationId, time);
            
            return String.format(
                    "¿Confirmas tu cita para el %s a las %s?\n\n" +
                    "Responde 'sí' para confirmar o 'no' para cancelar.",
                    formatDate(date),
                    formatTime(time)
            );
        }
        
        // Solo fecha, continuar con el flujo normal
        // Verificar disponibilidad para ese día
        List<AppointmentAvailabilityService.TimeSlot> availableSlots = 
                availabilityService.getAvailableSlots(account, date);
        
        if (availableSlots.isEmpty()) {
            return String.format(
                    "❌ No hay horarios disponibles para el %s. " +
                    "¿Puedes elegir otro día?",
                    formatDate(date)
            );
        }
        
        stateService.setDate(conversationId, date);
        
        String slotsMessage = availabilityService.formatSlotsForWhatsApp(availableSlots);
        
        return String.format(
                "Perfecto, elegiste el %s.\n\n%s",
                formatDate(date),
                slotsMessage
        );
    }
    
    private String handleTimeCollection(
            String userMessage,
            UuidId<Conversation> conversationId,
            CalendarProviderAccount account,
            AppointmentStateService.AppointmentState state
    ) {
        if (state.date() == null) {
            stateService.clearState(conversationId);
            return "Error: No se encontró la fecha. Por favor, intenta de nuevo.";
        }
        
        LocalTime time = dateTimeParser.parseTime(userMessage);
        
        if (time == null) {
            // Mostrar slots disponibles nuevamente
            List<AppointmentAvailabilityService.TimeSlot> slots = 
                    availabilityService.getAvailableSlots(account, state.date());
            return "No entendí la hora. " + availabilityService.formatSlotsForWhatsApp(slots);
        }
        
        LocalDateTime dateTime = LocalDateTime.of(state.date(), time);
        
        // Verificar disponibilidad específica
        if (!availabilityService.isSlotAvailable(account, dateTime)) {
            List<AppointmentAvailabilityService.TimeSlot> slots = 
                    availabilityService.getAvailableSlots(account, state.date());
            return String.format(
                    "❌ El horario %s no está disponible.\n\n%s",
                    formatTime(time),
                    availabilityService.formatSlotsForWhatsApp(slots)
            );
        }
        
        stateService.setTime(conversationId, time);
        
        return String.format(
                "¿Confirmas tu cita para el %s a las %s?\n\n" +
                "Responde 'sí' para confirmar o 'no' para cancelar.",
                formatDate(state.date()),
                formatTime(time)
        );
    }
    
    private String handleConfirmation(
            String userMessage,
            UuidId<Conversation> conversationId,
            CalendarProviderAccount account,
            UuidId<Client> clientId,
            UuidId<Contact> contactId,
            AppointmentStateService.AppointmentState state
    ) {
        String confirmation = userMessage.toLowerCase().trim();
        
        if (!confirmation.contains("sí") && !confirmation.contains("si") && 
            !confirmation.contains("confirmo") && !confirmation.contains("ok") &&
            !confirmation.contains("confirmar")) {
            // Limpiar estado y volver a inicio
            stateService.clearState(conversationId);
            return "Agendamiento cancelado. ¿En qué más puedo ayudarte?";
        }
        
        // Crear appointment
        if (state.date() == null || state.time() == null) {
            stateService.clearState(conversationId);
            return "Error: Faltan datos del agendamiento. Por favor, intenta de nuevo.";
        }
        
        LocalDateTime dateTime = LocalDateTime.of(state.date(), state.time());
        
        try {
            createAppointment.handle(
                    clientId,
                    contactId,
                    dateTime,
                    state.description() != null ? state.description() : "Cita agendada desde WhatsApp"
            );
            
            // Limpiar estado
            stateService.clearState(conversationId);
            
            // Cerrar conversación después de completar agendamiento
            closeConversation.handle(conversationId);
            log.info("✅ Conversación {} cerrada automáticamente después de agendamiento", conversationId.value());
            
            return String.format(
                    "✅ ¡Perfecto! Tu cita ha sido agendada exitosamente.\n\n" +
                    "📅 Fecha: %s\n" +
                    "🕐 Hora: %s\n\n" +
                    "Te enviaré un recordatorio antes de la fecha. " +
                    "¿Necesitas algo más?",
                    formatDate(state.date()),
                    formatTime(state.time())
            );
            
        } catch (Exception e) {
            log.error("Error al crear appointment: {}", e.getMessage(), e);
            stateService.clearState(conversationId);
            return "Lo siento, ocurrió un error al agendar tu cita. " +
                   "Por favor, intenta de nuevo o contacta directamente.";
        }
    }
    
    /**
     * Flujo normal de procesamiento con IA
     */
    private String handleNormalFlow(String userMessage, UuidId<Conversation> conversationId, String namespace) {
        // 1. Buscar contexto relevante en el knowledge base
        List<String> contextDocs = searchRelevantContext(userMessage, namespace);
        
        // 2. Obtener historial de conversación
        List<Map<String, String>> conversationHistory = getConversationHistory(conversationId);
        
        // 3. Generar respuesta con IA
        String response = aiService.generateResponse(userMessage, contextDocs, conversationHistory);
        
        log.info("Respuesta generada para conversación {}: {} caracteres", 
                conversationId.value(), response.length());
        
        return response;
    }
    
    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", 
                java.util.Locale.forLanguageTag("es-ES")));
    }
    
    private String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH));
    }
    
    /**
     * Busca documentos relevantes en el knowledge base
     */
    private List<String> searchRelevantContext(String query, String namespace) {
        try {
            // Generar embedding de la consulta
            float[] queryEmbedding = embeddingsPort.embedOne(query);
            
            // Buscar documentos similares
            List<VectorStore.QueryResult> results = vectorStore.query(
                    namespace,
                    queryEmbedding,
                    TOP_K_RESULTS,
                    Map.of()
            );
            
            // Extraer el texto de los resultados
            return results.stream()
                    .filter(r -> r.payload() != null && r.payload().containsKey("text"))
                    .map(r -> (String) r.payload().get("text"))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.warn("Error al buscar contexto: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Obtiene el historial reciente de la conversación
     */
    private List<Map<String, String>> getConversationHistory(UuidId<Conversation> conversationId) {
        try {
            List<Message> messages = messageRepository.findByConversation(
                    conversationId,
                    MAX_CONVERSATION_HISTORY
            );
            
            List<Map<String, String>> history = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> entry = new HashMap<>();
                entry.put("role", msg.direction().name());
                entry.put("content", msg.content());
                history.add(entry);
            }
            
            return history;
            
        } catch (Exception e) {
            log.warn("Error al obtener historial: {}", e.getMessage());
            return List.of();
        }
    }
}

