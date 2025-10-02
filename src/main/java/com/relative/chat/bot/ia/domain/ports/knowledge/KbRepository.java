package com.relative.chat.bot.ia.domain.ports.knowledge;

import com.relative.chat.bot.ia.domain.common.Id;
import com.relative.chat.bot.ia.domain.knowledge.Kb;
import com.relative.chat.bot.ia.domain.knowledge.KbDocument;
import com.relative.chat.bot.ia.domain.knowledge.KbChunk;
import com.relative.chat.bot.ia.domain.knowledge.VectorRef;

import java.util.List;
import java.util.Optional;

public interface KbRepository {
  Optional<Kb> findById(Id<Kb> id);
  void save(Kb kb);

  void saveDocument(KbDocument doc);
  void saveChunk(KbChunk chunk);

  List<KbDocument> docsOf(Id<Kb> kbId);
  List<KbChunk> chunksOf(Id<KbDocument> docId);

  void saveVectorRef(VectorRef ref);
}
