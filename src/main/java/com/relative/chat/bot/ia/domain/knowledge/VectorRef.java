package com.relative.chat.bot.ia.domain.knowledge;
 import com.relative.chat.bot.ia.domain.common.*;
 import com.relative.chat.bot.ia.domain.types.*;
 public record VectorRef(Id<KbChunk> chunkId, VectorBackend backend, String indexName, String vectorId){}