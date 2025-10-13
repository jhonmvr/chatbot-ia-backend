package com.relative.chat.bot.ia.domain.ports.knowledge;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.knowledge.Kb;
import com.relative.chat.bot.ia.domain.knowledge.KbDocument;
import com.relative.chat.bot.ia.domain.knowledge.KbChunk;
import com.relative.chat.bot.ia.domain.knowledge.VectorRef;

import java.util.List;
import java.util.Optional;

public interface KbRepository {
    Optional<Kb> findById(UuidId<Kb> id);
    List<Kb> findByClientId(UuidId<Client> clientId);
    void save(Kb kb);
    
    void saveDocument(KbDocument doc);
    void saveChunk(KbChunk chunk);
    
    List<KbDocument> docsOf(UuidId<Kb> kbId);
    List<KbChunk> chunksOf(UuidId<KbDocument> docId);
    
    void saveVectorRef(VectorRef ref);
}
