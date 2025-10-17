package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.knowledge.Kb;
import com.relative.chat.bot.ia.domain.ports.knowledge.KbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Caso de uso: Obtener Knowledge Base
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetKnowledgeBase {
    
    private final KbRepository kbRepository;
    
    /**
     * Obtiene un KB por su ID
     * 
     * @param kbId ID del Knowledge Base
     * @return KB si existe
     */
    public Optional<Kb> handle(UuidId<Kb> kbId) {
        return kbRepository.findById(kbId);
    }
    
    /**
     * Lista todos los KBs de un cliente
     * 
     * @param clientId ID del cliente
     * @return Lista de KBs del cliente
     */
    public List<Kb> listByClient(UuidId<Client> clientId) {
        return kbRepository.findByClientId(clientId);
    }

    public List<Kb> findAll() {
        return kbRepository.findAll();
    }
}

