package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.knowledge.*;
import com.relative.chat.bot.ia.domain.ports.knowledge.KbRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.*;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class KbRepositoryJpaAdapter implements KbRepository {
    
    private final KbJpa kbRepo;
    private final KbDocumentJpa docRepo;
    private final KbChunkJpa chunkRepo;
    private final KbVectorRefJpa vectorRepo;
    
    @PersistenceContext
    private EntityManager em;
    
    private static Kb toDomain(KbEntity e) {
        return new Kb(
            UuidId.of(e.getId()),
            UuidId.of(e.getClientEntity().getId()),
            e.getName(),
            e.getDescription()
        );
    }
    
    private static KbDocument toDomain(KbDocumentEntity e) {
        return new KbDocument(
            UuidId.of(e.getId()),
            UuidId.of(e.getKbEntity().getId()),
            e.getSourceUri(),
            e.getSourceUri(),
            e.getSourceUri(),
            e.getMimeType(),
            null,
            null,
            e.getLanguage()
        );
    }
    
    private static KbChunk toDomain(KbChunkEntity e) {
        return new KbChunk(
            UuidId.of(e.getId()),
            UuidId.of(e.getDocument().getId()),
            e.getChunkIndex(),
            e.getContent(),
            null
        );
    }
    
    @Override
    public Optional<Kb> findById(UuidId<Kb> id) {
        return kbRepo.findById(id.value()).map(KbRepositoryJpaAdapter::toDomain);
    }
    
    @Override
    public List<Kb> findByClientId(UuidId<Client> clientId) {
        return kbRepo.findByClientEntityId(clientId.value())
            .stream()
            .map(KbRepositoryJpaAdapter::toDomain)
            .toList();
    }

    @Override
    public List<Kb> findAll() {
        return kbRepo.findAll()
                .stream()
                .map(KbRepositoryJpaAdapter::toDomain)
                .toList();
    }

    @Override
    public void save(Kb kb) {
        KbEntity e = new KbEntity();
        e.setId(kb.id().value());
        e.setClientEntity(em.getReference(ClientEntity.class, kb.clientId().value()));
        e.setName(kb.name());
        e.setDescription(kb.description());
        e.setCreatedAt(java.time.OffsetDateTime.now());
        e.setUpdatedAt(java.time.OffsetDateTime.now());
        kbRepo.save(e);
    }
    
    @Override
    public void saveDocument(KbDocument doc) {
        KbDocumentEntity e = new KbDocumentEntity();
        e.setId(doc.id().value());
        e.setKbEntity(em.getReference(KbEntity.class, doc.kbId().value()));
        e.setSourceUri(doc.source());
        e.setMimeType(doc.mimeType());
        e.setLanguage(doc.language());
        e.setStatus("READY");
        e.setChunkCount(0);
        e.setMetadata(new java.util.HashMap<>());
        e.setCreatedAt(java.time.OffsetDateTime.now());
        e.setUpdatedAt(java.time.OffsetDateTime.now());
        docRepo.save(e);
    }
    
    @Override
    public void saveChunk(KbChunk chunk) {
        KbChunkEntity e = new KbChunkEntity();
        e.setId(chunk.id().value());
        e.setDocument(em.getReference(KbDocumentEntity.class, chunk.documentId().value()));
        e.setChunkIndex(chunk.index());
        e.setContent(chunk.content());
        e.setMetadata(new java.util.HashMap<>());
        e.setCreatedAt(java.time.OffsetDateTime.now());
        chunkRepo.save(e);
    }
    
    @Override
    public List<KbDocument> docsOf(UuidId<Kb> kbId) {
        return docRepo.findByKbEntityId(kbId.value())
            .stream()
            .map(KbRepositoryJpaAdapter::toDomain)
            .toList();
    }
    
    @Override
    public List<KbChunk> chunksOf(UuidId<KbDocument> docId) {
        return chunkRepo.findByDocumentIdOrderByChunkIndexAsc(docId.value())
            .stream()
            .map(KbRepositoryJpaAdapter::toDomain)
            .toList();
    }
    
    @Override
    public void saveVectorRef(VectorRef ref) {
        KbVectorRefEntity e = new KbVectorRefEntity();
        e.setChunkId(ref.chunkId().value());
        e.setBackend(ref.backend().name());
        e.setIndexName(ref.indexName());
        e.setVectorId(ref.vectorId());
        e.setCreatedAt(java.time.OffsetDateTime.now());
        vectorRepo.save(e);
    }
}
