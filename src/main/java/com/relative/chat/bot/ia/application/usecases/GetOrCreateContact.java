package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.vo.PhoneE164;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso: Obtener o crear un contacto
 */
@Service
@RequiredArgsConstructor
public class GetOrCreateContact {
    
    private final ContactRepository contactRepository;
    
    /**
     * Obtiene un contacto existente o crea uno nuevo
     * 
     * @param clientId ID del cliente
     * @param phoneNumber Número de teléfono del contacto
     * @param displayName Nombre del contacto
     * @param channel Canal de comunicación
     * @return Contacto encontrado o creado
     */
    @Transactional
    public Contact handle(
            UuidId<Client> clientId,
            String phoneNumber,
            String displayName,
            Channel channel
    ) {
        // Normalizar el número de teléfono a formato E164
        String normalizedPhone = normalizeToE164(phoneNumber);
        
        // Buscar contacto existente por cliente y teléfono
        Optional<Contact> existingContact = contactRepository.findByClientAndPhone(clientId, normalizedPhone);
        
        if (existingContact.isPresent()) {
            return existingContact.get();
        }
        
        // Crear nuevo contacto
        Contact newContact = Contact.create(
                clientId,
                displayName != null ? displayName : phoneNumber,
                null, // firstName
                null, // lastName
                new PhoneE164(normalizedPhone),
                null  // email
        );
        
        contactRepository.save(newContact);
        
        return newContact;
    }
    
    /**
     * Normaliza un número de teléfono al formato E164
     * - Elimina espacios, guiones, paréntesis y puntos
     * - Si no tiene prefijo "+", lo agrega
     * - Si empieza con "00", lo reemplaza con "+"
     * 
     * @param phoneNumber Número de teléfono a normalizar
     * @return Número normalizado en formato E164
     * @throws IllegalArgumentException si el número está vacío o no es válido
     */
    private String normalizeToE164(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("El número de teléfono no puede estar vacío");
        }
        
        // Eliminar espacios, guiones, paréntesis, puntos y otros caracteres no numéricos (excepto +)
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)\\.]", "");
        
        // Si no empieza con +, agregarlo
        if (!cleaned.startsWith("+")) {
            // Si empieza con 00 (formato internacional alternativo), reemplazarlo con +
            if (cleaned.startsWith("00")) {
                cleaned = "+" + cleaned.substring(2);
            } else {
                // Agregar el prefijo + (asumiendo que ya tiene código de país)
                cleaned = "+" + cleaned;
            }
        }
        
        return cleaned;
    }
}

