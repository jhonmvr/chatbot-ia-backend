
package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.ports.out.EmbeddingsPort;

import com.relative.chat.bot.ia.application.ports.out.VectorStore;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

import java.util.Map;

@Service @RequiredArgsConstructor
public class SearchDocuments {
    private final EmbeddingsPort embeddings;

    private final VectorStore vectorStore;

    public List<VectorStore.QueryResult> handle(String namespace, String query, int topK) {
        float[] q = embeddings.embedOne(query);

        return vectorStore.query(namespace, q, topK, Map.of());

    }
}
