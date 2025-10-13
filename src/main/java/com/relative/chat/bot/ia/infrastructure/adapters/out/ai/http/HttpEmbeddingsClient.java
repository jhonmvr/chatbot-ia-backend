
package com.relative.chat.bot.ia.infrastructure.adapters.out.ai.http;

import com.relative.chat.bot.ia.application.ports.out.EmbeddingsPort;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Adapter genérico HTTP para generar embeddings
 * 
 * Se activa cuando: app.ai.embeddings.provider=http
 * Útil para servicios de embeddings locales o custom
 */
@Component 
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.embeddings.provider", havingValue = "http", matchIfMissing = false)
public class HttpEmbeddingsClient implements EmbeddingsPort {
    private final WebClient aiClient;

    private final String model = System.getProperty("AI_EMBED_MODEL",
            System.getenv().getOrDefault("AI_EMBED_MODEL", "all-MiniLM-L6-v2"));

    record EmbedRequest(String model, java.util.List<String> input) {}
    record EmbedResponse(java.util.List<java.util.List<Double>> data, Integer dim) {}
    @Override public String model() {
return model;
 }
    @Override public float[] embedOne(String text) {
return embedMany(java.util.List.of(text)).get(0);
 }
    @Override public List<float[]> embedMany(List<String> texts) {
        var resp = aiClient.post().uri("/v1/embeddings")
                .bodyValue(new EmbedRequest(model, texts))
                .retrieve().bodyToMono(EmbedResponse.class).block();

        return resp.data().stream().map(list -> {
            var f = new float[list.size()];

            for (int i=0;
i<list.size();
i++) f[i] = list.get(i).floatValue();

            return f;

        }).toList();

    }
}
