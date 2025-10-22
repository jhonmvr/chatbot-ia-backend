package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Caso de uso: Envío masivo de mensajes con plantillas
 * 
 * Basado en la documentación de WhatsApp Business Management API:
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates/template-messaging-limits
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates/template-pacing
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates/template-pausing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendBulkTemplate {
    
    private final SendTemplate sendTemplate;
    private final ContactRepository contactRepository;
    private final ClientPhoneRepository clientPhoneRepository;
    
    // Executor para procesamiento paralelo
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    /**
     * Filtros para envío masivo
     */
    public record BulkSendFilters(
        Boolean onlyActive,
        Boolean onlyVip,
        List<String> tagNames,
        List<UuidId<Category>> categoryIds,
        String preferredContactMethod,
        Boolean marketingConsent
    ) {
        public static BulkSendFilters createDefault() {
            return new BulkSendFilters(
                true,  // onlyActive
                null,  // onlyVip
                null,  // tagNames
                null,  // categoryIds
                "WHATSAPP",  // preferredContactMethod
                true   // marketingConsent
            );
        }
        
        public static BulkSendFilters forVipContacts() {
            return new BulkSendFilters(
                true,  // onlyActive
                true,  // onlyVip
                null,  // tagNames
                null,  // categoryIds
                "WHATSAPP",  // preferredContactMethod
                true   // marketingConsent
            );
        }
        
        public static BulkSendFilters forTaggedContacts(List<String> tagNames) {
            return new BulkSendFilters(
                true,  // onlyActive
                null,  // onlyVip
                tagNames,  // tagNames
                null,  // categoryIds
                "WHATSAPP",  // preferredContactMethod
                true   // marketingConsent
            );
        }
        
        public static BulkSendFilters forCategorizedContacts(List<UuidId<Category>> categoryIds) {
            return new BulkSendFilters(
                true,  // onlyActive
                null,  // onlyVip
                null,  // tagNames
                categoryIds,  // categoryIds
                "WHATSAPP",  // preferredContactMethod
                true   // marketingConsent
            );
        }
    }
    
    /**
     * Resultado del envío masivo
     */
    public record BulkSendResult(
        int totalContacts,
        int successfulSends,
        int failedSends,
        List<String> errors,
        Instant startedAt,
        Instant completedAt
    ) {}
    
    /**
     * Envía plantillas masivamente con filtros
     * 
     * @param clientId ID del cliente
     * @param phoneId ID del teléfono del cliente
     * @param templateName Nombre de la plantilla
     * @param parameters Parámetros de la plantilla
     * @param parameterFormat Formato de parámetros
     * @param filters Filtros para seleccionar contactos
     * @return Resultado del envío masivo
     */
    @Transactional
    public BulkSendResult handle(
            UuidId<Client> clientId,
            UuidId<ClientPhone> phoneId,
            String templateName,
            Map<String, String> parameters,
            ParameterFormat parameterFormat,
            BulkSendFilters filters
    ) {
        Instant startedAt = Instant.now();
        
        try {
            // 1. Validar límites de envío masivo
            validateBulkSendLimits(clientId, phoneId);
            
            // 2. Obtener contactos filtrados
            List<Contact> contacts = getFilteredContacts(clientId, filters);
            
            if (contacts.isEmpty()) {
                log.warn("No se encontraron contactos que cumplan los filtros especificados");
                return new BulkSendResult(0, 0, 0, List.of("No se encontraron contactos"), startedAt, Instant.now());
            }
            
            log.info("Iniciando envío masivo a {} contactos con plantilla '{}'", contacts.size(), templateName);
            
            // 3. Procesar envíos en lotes para respetar límites de pacing
            List<CompletableFuture<SendResult>> futures = new ArrayList<>();
            List<String> errors = Collections.synchronizedList(new ArrayList<>());
            int[] counters = {0, 0}; // [successfulSends, failedSends]
            
            // Procesar en lotes de 10 para respetar límites de pacing
            List<List<Contact>> batches = partitionList(contacts, 10);
            
            for (List<Contact> batch : batches) {
                CompletableFuture<List<SendResult>> batchFuture = CompletableFuture.supplyAsync(() -> {
                    List<SendResult> batchResults = new ArrayList<>();
                    
                    for (Contact contact : batch) {
                        try {
                            // Aplicar pacing entre mensajes (1 segundo de delay)
                            Thread.sleep(1000);
                            
                            SendResult result = sendTemplateToContact(
                                clientId, phoneId, contact, templateName, parameters, parameterFormat
                            );
                            batchResults.add(result);
                            
                        } catch (Exception e) {
                            log.error("Error al enviar plantilla a contacto {}: {}", contact.id().value(), e.getMessage());
                            batchResults.add(new SendResult(false, contact.id().value().toString(), e.getMessage()));
                        }
                    }
                    
                    return batchResults;
                }, executorService);
                
                futures.add(batchFuture.thenApply(results -> {
                    for (SendResult result : results) {
                        if (result.success()) {
                            counters[0]++;
                        } else {
                            counters[1]++;
                            errors.add(String.format("Contacto %s: %s", result.contactId(), result.error()));
                        }
                    }
                    return null; // No necesitamos el resultado específico
                }));
            }
            
            // 4. Esperar a que todos los lotes terminen
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            Instant completedAt = Instant.now();
            
            log.info("Envío masivo completado. Total: {}, Exitosos: {}, Fallidos: {}", 
                    contacts.size(), counters[0], counters[1]);
            
            return new BulkSendResult(
                contacts.size(),
                counters[0],
                counters[1],
                errors,
                startedAt,
                completedAt
            );
            
        } catch (Exception e) {
            log.error("Error en envío masivo: {}", e.getMessage(), e);
            return new BulkSendResult(
                0, 0, 0, 
                List.of("Error general: " + e.getMessage()), 
                startedAt, Instant.now()
            );
        }
    }
    
    /**
     * Envía plantilla a un contacto específico
     */
    private SendResult sendTemplateToContact(
            UuidId<Client> clientId,
            UuidId<ClientPhone> phoneId,
            Contact contact,
            String templateName,
            Map<String, String> parameters,
            ParameterFormat parameterFormat
    ) {
        try {
            // Crear conversación para el contacto
            UuidId<Conversation> conversationId = UuidId.newId();
            
            // Enviar plantilla usando el caso de uso SendTemplate
            sendTemplate.handle(
                clientId,
                conversationId,
                contact.id(),
                phoneId,
                templateName,
                parameters,
                parameterFormat,
                contact.phoneE164() != null ? contact.phoneE164().value() : null
            );
            
            return new SendResult(true, contact.id().value().toString(), null);
            
        } catch (Exception e) {
            return new SendResult(false, contact.id().value().toString(), e.getMessage());
        }
    }
    
    /**
     * Obtiene contactos filtrados según los criterios especificados
     */
    private List<Contact> getFilteredContacts(UuidId<Client> clientId, BulkSendFilters filters) {
        List<Contact> allContacts = contactRepository.findByClientId(clientId);
        
        return allContacts.stream()
            .filter(contact -> filters.onlyActive() == null || contact.isActive() == filters.onlyActive())
            .filter(contact -> filters.onlyVip() == null || contact.isVip() == filters.onlyVip())
            .filter(contact -> filters.tagNames() == null || 
                (contact.tagNames() != null && contact.tagNames().stream()
                    .anyMatch(tag -> filters.tagNames().contains(tag))))
            .filter(contact -> filters.preferredContactMethod() == null || 
                filters.preferredContactMethod().equals(contact.preferredContactMethod()))
            .filter(contact -> filters.marketingConsent() == null || 
                contact.marketingConsent() == filters.marketingConsent())
            .filter(contact -> contact.phoneE164() != null) // Debe tener teléfono
            .collect(Collectors.toList());
    }
    
    /**
     * Valida límites de envío masivo
     */
    private void validateBulkSendLimits(UuidId<Client> clientId, UuidId<ClientPhone> phoneId) {
        // Obtener contactos del cliente
        List<Contact> contacts = contactRepository.findByClientId(clientId);
        
        // Límite máximo de 1000 contactos por envío masivo
        if (contacts.size() > 1000) {
            throw new IllegalStateException(
                String.format("Límite de envío masivo excedido. Máximo: 1000, Contactos del cliente: %d", contacts.size())
            );
        }
        
        // Verificar que el teléfono esté activo
        Optional<ClientPhone> phoneOpt = clientPhoneRepository.findById(phoneId);
        if (phoneOpt.isEmpty() || phoneOpt.get().status() != com.relative.chat.bot.ia.domain.types.EntityStatus.ACTIVE) {
            throw new IllegalStateException("Teléfono no encontrado o inactivo: " + phoneId.value());
        }
    }
    
    /**
     * Divide una lista en lotes más pequeños
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
    
    /**
     * Resultado de envío individual
     */
    private record SendResult(
        boolean success,
        String contactId,
        String error
    ) {}
}