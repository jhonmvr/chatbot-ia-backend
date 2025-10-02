package com.relative.chat.bot.ia.domain.knowledge;
 import com.relative.chat.bot.ia.domain.common.*;
 public final class KbChunk{
private final Id<KbChunk> id;
 private final Id<KbDocument> documentId;
 private final int index;
 private final String content;
 private final Integer tokens;
 public KbChunk(Id<KbChunk> id, Id<KbDocument> documentId, int index, String content, Integer tokens){
this.id=id;
 this.documentId=documentId;
 this.index=index;
 this.content=content;
 this.tokens=tokens;
 if(index<0) throw new DomainException("chunk index < 0");
 if(content==null||content.isBlank()) throw new DomainException("chunk sin contenido");
 }
public Id<KbChunk> id(){return id;
}
public Id<KbDocument> documentId(){return documentId;
}
public int index(){return index;
}
public String content(){return content;
}
public Integer tokens(){return tokens;
}
}