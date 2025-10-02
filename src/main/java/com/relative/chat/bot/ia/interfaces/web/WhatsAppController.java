
package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.usecases.SearchDocuments;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {
    private final SearchDocuments search;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> receive(@RequestBody Mono<Map<String,Object>> payload) {
        return payload
            .map(p -> String.valueOf(p.getOrDefault("text", "hola")))
            .map(text -> {
                var results = search.handle("kb", text, 5);

                if (results.isEmpty()) return "No encontré información.";

                var top = results.getFirst();

                return "Respuesta basada en: " + (top.payload() != null ? top.payload().toString() : top.id());

            });

    }
}
