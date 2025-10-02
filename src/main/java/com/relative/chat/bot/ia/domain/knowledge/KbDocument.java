package com.relative.chat.bot.ia.domain.knowledge;
 import com.relative.chat.bot.ia.domain.common.*;
 import com.relative.chat.bot.ia.domain.vo.*;
 import java.util.*;
 public record KbDocument(Id<KbDocument> id, Id<Kb> kbId, String source, String sourceId, String filename, String mimeType, Long sizeBytes, ChecksumSha256 checksum, String language){
public Optional<String> sourceIdOpt(){return Optional.ofNullable(sourceId);
}
}