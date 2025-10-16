package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.ClientPhoneMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ClientJpa;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ClientPhoneJpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de repositorio para ClientPhone
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ClientPhoneRepositoryAdapter implements ClientPhoneRepository {
    
    private final ClientPhoneJpa clientPhoneJpa;
    private final ClientJpa clientJpa;
    
    @Override
    public Optional<ClientPhone> findById(UuidId<ClientPhone> id) {
        return clientPhoneJpa.findById(id.value())
                .map(ClientPhoneMapper::toDomain);
    }
    
    @Override
    public List<ClientPhone> findByClient(UuidId<Client> clientId) {
        return clientPhoneJpa.findByClientEntityId(clientId.value())
                .stream()
                .map(ClientPhoneMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Client> findClientByProviderSid(String providerSid, String provider) {
        if (providerSid == null || provider == null) {
            log.warn("Intento de buscar cliente con providerSid o provider null");
            return Optional.empty();
        }
        
        return clientPhoneJpa.findByProviderSidAndProvider(providerSid, provider)
                .map(clientPhone -> {
                    ClientEntity clientEntity = clientPhone.getClientEntity();
                    return new Client(
                        UuidId.of(clientEntity.getId()),
                        clientEntity.getTaxId() != null ? clientEntity.getTaxId() : clientEntity.getName(),
                        clientEntity.getName(),
                        EntityStatus.valueOf(clientEntity.getStatus())
                    );
                });
    }
    
    @Override
    public Optional<ClientPhone> findByProviderSid(String providerSid, String provider) {
        if (providerSid == null || provider == null) {
            return Optional.empty();
        }
        
        return clientPhoneJpa.findByProviderSidAndProvider(providerSid, provider)
                .map(ClientPhoneMapper::toDomain);
    }
    
    @Override
    public Optional<ClientPhone> findByPhoneAndChannel(String e164, Channel channel) {
        return clientPhoneJpa.findByE164AndChannel(e164, channel.name())
                .map(ClientPhoneMapper::toDomain);
    }
    
    @Override
    public void save(ClientPhone clientPhone) {
        // Obtener la entidad del cliente
        ClientEntity clientEntity = clientJpa.findById(clientPhone.clientId().value())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + clientPhone.clientId().value()));
        
        ClientPhoneEntity entity = ClientPhoneMapper.toEntity(clientPhone, clientEntity);
        clientPhoneJpa.save(entity);
    }

    @Override
    public List<ClientPhone> findAll() {
        return clientPhoneJpa.findAll().stream().map(ClientPhoneMapper::toDomain).toList();
    }
}

