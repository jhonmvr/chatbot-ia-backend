package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.knowledge.Kb;
import com.relative.chat.bot.ia.domain.ports.knowledge.KbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Crear un Knowledge Base
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateKnowledgeBase {
    
    private final KbRepository kbRepository;
    
    /**
     * Crea un nuevo Knowledge Base para un cliente
     * 
     * @param clientId ID del cliente
     * @param name Nombre del KB
     * @param description Descripción del KB
     * @return KB creado
     */
    @Transactional
    public Kb handle(UuidId<Client> clientId, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del KB es requerido");
        }
        
        // Crear nuevo KB
        Kb kb = new Kb(
                UuidId.newId(),
                clientId,
                name,
                description != null ? description : ""
        );
        
        // Guardar en repositorio
        kbRepository.save(kb);
        
        log.info("Knowledge Base creado: id={}, name={}, client={}", 
                kb.id().value(), name, clientId.value());
        
        return kb;
    }
}

