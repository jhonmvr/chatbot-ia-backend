
package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.ports.out.VectorStore;

import com.relative.chat.bot.ia.domain.model.Document;

import com.relative.chat.bot.ia.application.ports.out.EmbeddingsPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class IngestDocuments {
    private final EmbeddingsPort embeddings;

    private final VectorStore vectorStore;

    public void handle(String namespace, List<Document> docs) {
        var vectors = embeddings.embedMany(docs.stream().map(Document::text).toList());

        var records = new java.util.ArrayList<VectorStore.VectorRecord>(docs.size());

        for (int i=0;
i<docs.size();
i++) {
            var d = docs.get(i);

            records.add(new VectorStore.VectorRecord(d.id(), vectors.get(i), d.metadata()));

        }
        vectorStore.upsert(namespace, records);

    }
}
