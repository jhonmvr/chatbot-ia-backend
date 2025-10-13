package com.relative.chat.bot.ia.domain.knowledge;

import com.relative.chat.bot.ia.domain.common.*;

public final class KbChunk {
    
    private final UuidId<KbChunk> id;
    private final UuidId<KbDocument> documentId;
    private final int index;
    private final String content;
    private final Integer tokens;
    
    public KbChunk(UuidId<KbChunk> id, UuidId<KbDocument> documentId, int index, String content, Integer tokens) {
        this.id = id;
        this.documentId = documentId;
        this.index = index;
        this.content = content;
        this.tokens = tokens;
        if (index < 0) {
            throw new DomainException("chunk index < 0");
        }
        if (content == null || content.isBlank()) {
            throw new DomainException("chunk sin contenido");
        }
    }
    
    public UuidId<KbChunk> id() {
        return id;
    }
    
    public UuidId<KbDocument> documentId() {
        return documentId;
    }
    
    public int index() {
        return index;
    }
    
    public String content() {
        return content;
    }
    
    public Integer tokens() {
        return tokens;
    }
}