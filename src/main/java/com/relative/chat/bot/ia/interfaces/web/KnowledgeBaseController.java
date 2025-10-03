package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.usecases.CreateKnowledgeBase;
import com.relative.chat.bot.ia.application.usecases.GetKnowledgeBase;
import com.relative.chat.bot.ia.application.usecases.IngestDocuments;
import com.relative.chat.bot.ia.application.usecases.SearchDocuments;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.knowledge.Kb;
import com.relative.chat.bot.ia.domain.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * API REST para gestión del Knowledge Base
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    
    // ✅ CORRECTO: Solo inyectamos Use Cases (Application Layer)
    private final CreateKnowledgeBase createKnowledgeBase;
    private final GetKnowledgeBase getKnowledgeBase;
    private final IngestDocuments ingestDocuments;
    private final SearchDocuments searchDocuments;
    
    /**
     * Crea un Knowledge Base
     * POST /api/knowledge-base
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createKb(@RequestBody Map<String, Object> request) {
        try {
            String clientId = (String) request.get("clientId");
            String name = (String) request.get("name");
            String description = (String) request.getOrDefault("description", "");
            
            if (clientId == null || name == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "clientId y name son requeridos"
                ));
            }
            
            UuidId<Client> clientUuid = UuidId.of(UUID.fromString(clientId));
            
            // Delegar al Use Case
            Kb kb = createKnowledgeBase.handle(clientUuid, name, description);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "kbId", kb.id().value().toString(),
                    "message", "KB creado"
            ));
            
        } catch (Exception e) {
            log.error("Error al crear KB: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Ingesta documentos al Knowledge Base
     * POST /api/knowledge-base/{kbId}/ingest
     */
    @PostMapping("/{kbId}/ingest")
    public ResponseEntity<Map<String, Object>> ingestDocuments(
            @PathVariable String kbId,
            @RequestBody Map<String, Object> request
    ) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> docs = (List<Map<String, String>>) request.get("documents");
            
            if (docs == null || docs.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Se requiere al menos un documento"
                ));
            }
            
            // Convertir a Documents del dominio
            List<Document> documents = docs.stream()
                    .map(doc -> {
                        String content = doc.get("content");
                        Map<String, Object> metadata = new HashMap<>(doc);
                        metadata.remove("content");
                        return new Document(
                                UUID.randomUUID().toString(),
                                content,
                                metadata
                        );
                    })
                    .collect(Collectors.toList());
            
            log.info("Ingesta de {} documentos al KB {}", documents.size(), kbId);
            
            // Usar namespace del KB
            String namespace = "kb_" + kbId;
            ingestDocuments.handle(namespace, documents);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Documentos ingestados exitosamente",
                    "count", documents.size()
            ));
            
        } catch (Exception e) {
            log.error("Error al ingestar documentos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Busca en el Knowledge Base
     * POST /api/knowledge-base/{kbId}/search
     */
    @PostMapping("/{kbId}/search")
    public ResponseEntity<Map<String, Object>> search(
            @PathVariable String kbId,
            @RequestBody Map<String, Object> request
    ) {
        try {
            String query = (String) request.get("query");
            Integer topK = (Integer) request.getOrDefault("topK", 5);
            
            if (query == null || query.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "query es requerido"
                ));
            }
            
            String namespace = "kb_" + kbId;
            var queryResults = searchDocuments.handle(namespace, query, topK);
            
            // Extraer solo el payload de los resultados
            List<String> results = queryResults.stream()
                    .map(r -> r.payload() != null && r.payload().containsKey("text") 
                            ? (String) r.payload().get("text") 
                            : "")
                    .filter(s -> !s.isBlank())
                    .toList();
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "query", query,
                    "results", results,
                    "count", results.size()
            ));
            
        } catch (Exception e) {
            log.error("Error al buscar en KB: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Obtiene un Knowledge Base específico
     * GET /api/knowledge-base/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getKb(@PathVariable String id) {
        try {
            UuidId<Kb> kbUuid = UuidId.of(UUID.fromString(id));
            
            // Delegar al Use Case
            Optional<Kb> kb = getKnowledgeBase.handle(kbUuid);
            
            if (kb.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Kb k = kb.get();
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", k.id().value().toString());
            result.put("clientId", k.clientId().value().toString());
            result.put("name", k.name());
            result.put("description", k.description());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al obtener KB: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
