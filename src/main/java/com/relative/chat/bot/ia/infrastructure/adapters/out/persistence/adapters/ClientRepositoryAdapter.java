package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
import com.relative.chat.bot.ia.domain.ports.identity.ClientRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ClientJpa;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClientRepositoryAdapter implements ClientRepository {
    
    private final ClientJpa repo;
    
    private static Client toDomain(ClientEntity e) {
        return new Client(
            UuidId.of(e.getId()),
            e.getTaxId() != null ? e.getTaxId() : e.getName(), // code from taxId or name
            e.getName(),
            EntityStatus.valueOf(e.getStatus())
        );
    }
    
    private static ClientEntity toEntity(Client d) {
        ClientEntity e = new ClientEntity();
        e.setId(d.id().value());
        e.setName(d.name());
        e.setTaxId(d.code()); // Guardar el code en taxId
        e.setStatus(d.status().name());
        e.setTimezone("America/Guayaquil");
        e.setMetadata(new java.util.HashMap<>()); // metadata vacío por defecto
        e.setCreatedAt(java.time.OffsetDateTime.now());
        e.setUpdatedAt(java.time.OffsetDateTime.now());
        return e;
    }
    
    @Override
    public Optional<Client> findById(UuidId<Client> id) {
        return repo.findById(id.value()).map(ClientRepositoryAdapter::toDomain);
    }
    
    @Override
    public Optional<Client> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        
        return repo.findAll().stream()
            .filter(c -> code.equals(c.getTaxId()))
            .findFirst()
            .map(ClientRepositoryAdapter::toDomain);
    }

    @Override
    public List<Client> findAll() {
        return repo.findAll().stream().map(ClientRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(Client client) {
        repo.save(toEntity(client));
    }
}
