package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.usecases.CreateKnowledgeBase;
import com.relative.chat.bot.ia.application.usecases.GetKnowledgeBase;
import com.relative.chat.bot.ia.application.usecases.IngestDocuments;
import com.relative.chat.bot.ia.application.usecases.SearchDocuments;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.knowledge.Kb;
import com.relative.chat.bot.ia.domain.model.Document;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Knowledge Base", description = "API para gestionar la base de conocimiento (RAG) del chatbot")
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
    @Operation(
        summary = "Crear una nueva base de conocimiento",
        description = "Crea un Knowledge Base para un cliente específico donde se podrán almacenar documentos para el sistema RAG"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Knowledge Base creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "kbId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "KB creado"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o error al crear",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "clientId y name son requeridos"
                    }
                    """)
            )
        )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createKb(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del Knowledge Base a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "123e4567-e89b-12d3-a456-426614174000",
                      "name": "Productos y Servicios",
                      "description": "Base de conocimiento sobre nuestros productos"
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
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
    @Operation(
        summary = "Ingestar documentos en el Knowledge Base",
        description = "Agrega documentos al Knowledge Base para que el chatbot pueda usar esa información en sus respuestas (RAG). Los documentos se convierten en embeddings y se almacenan en el vector store."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Documentos ingestados exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Documentos ingestados exitosamente",
                      "count": 3
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Error al ingestar documentos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Se requiere al menos un documento"
                    }
                    """)
            )
        )
    })
    @PostMapping("/{kbId}/ingest")
    public ResponseEntity<Map<String, Object>> ingestDocuments(
            @Parameter(description = "UUID del Knowledge Base", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String kbId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Lista de documentos a ingestar",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "documents": [
                            {
                              "content": "Nuestro horario de atención es de Lunes a Viernes de 9am a 6pm",
                              "source": "FAQ",
                              "category": "horarios"
                            },
                            {
                              "content": "Ofrecemos envíos gratuitos en compras mayores a $50",
                              "source": "Políticas",
                              "category": "envios"
                            }
                          ]
                        }
                        """)
                )
            )
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
    @Operation(
        summary = "Buscar en el Knowledge Base",
        description = "Realiza una búsqueda semántica en el Knowledge Base usando embeddings. Retorna los documentos más relevantes según la consulta."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Búsqueda realizada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "query": "¿Cuál es el horario de atención?",
                      "results": [
                        "Nuestro horario de atención es de Lunes a Viernes de 9am a 6pm",
                        "Los sábados atendemos de 10am a 2pm"
                      ],
                      "count": 2
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Query inválido",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "query es requerido"
                    }
                    """)
            )
        )
    })
    @PostMapping("/{kbId}/search")
    public ResponseEntity<Map<String, Object>> search(
            @Parameter(description = "UUID del Knowledge Base", required = true, example = "b7b0a4d7-744b-4bd7-aae9-1ae0be626e7c")
            @PathVariable String kbId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Consulta de búsqueda",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "query": "¿Cuál es el horario de atención?",
                          "topK": 5
                        }
                        """)
                )
            )
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
    @Operation(
        summary = "Obtener un Knowledge Base",
        description = "Retorna la información de un Knowledge Base específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Knowledge Base encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "id": "550e8400-e29b-41d4-a716-446655440000",
                      "clientId": "123e4567-e89b-12d3-a456-426614174000",
                      "name": "Productos y Servicios",
                      "description": "Base de conocimiento sobre nuestros productos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "ID inválido"),
        @ApiResponse(responseCode = "404", description = "Knowledge Base no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getKb(
        @Parameter(description = "UUID del Knowledge Base", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<Kb> kbUuid = UuidId.of(UUID.fromString(id));
            
            // Delegar al Use Case
            Optional<Kb> kb = getKnowledgeBase.handle(kbUuid);
            
            if (kb.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Kb k = kb.get();
            
            Map<String, Object> result = this.toDto(k);

            /* new HashMap<>();
            result.put("id", k.id().value().toString());
            result.put("clientId", k.clientId().value().toString());
            result.put("name", k.name());
            result.put("description", k.description());
            */
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al obtener KB: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene todas las Knowledge Bases de un cliente
     * GET /api/knowledge-base/by-client/{clientId}
     */
    @Operation(
            summary = "Obtener Knowledge Bases por cliente",
            description = "Retorna todas las bases de conocimiento asociadas a un cliente específico"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de Knowledge Bases obtenida correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                {
                  "status": "success",
                  "count": 2,
                  "knowledgeBases": [
                    {
                      "id": "550e8400-e29b-41d4-a716-446655440000",
                      "clientId": "123e4567-e89b-12d3-a456-426614174000",
                      "name": "Productos y Servicios",
                      "description": "Base de conocimiento sobre nuestros productos"
                    },
                    {
                      "id": "660e8400-e29b-41d4-a716-446655440111",
                      "clientId": "123e4567-e89b-12d3-a456-426614174000",
                      "name": "Preguntas Frecuentes",
                      "description": "Respuestas comunes para soporte al cliente"
                    }
                  ]
                }
                """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado o sin bases de conocimiento"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<Map<String, Object>> listKnowledgeBasesByClient(
            @Parameter(description = "UUID del cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String clientId
    ) {
        try {
            UuidId<Client> clientUuid = UuidId.of(UUID.fromString(clientId));

            List<Kb> knowledgeBases = getKnowledgeBase.listByClient(clientUuid);

            if (knowledgeBases == null || knowledgeBases.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "error",
                        "message", "No se encontraron bases de conocimiento para el cliente: " + clientId
                ));
            }

            List<Map<String, Object>> kbDtos = knowledgeBases.stream()
                    .map(this::toDto)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "knowledgeBases", kbDtos,
                    "count", kbDtos.size()
            ));

        } catch (IllegalArgumentException e) {
            log.error("UUID inválido: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "UUID de cliente inválido"
            ));
        } catch (Exception e) {
            log.error("Error al obtener Knowledge Bases por cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Convierte un Knowledge Base del dominio a DTO
     */
    private Map<String, Object> toDto(Kb kb) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", kb.id().value().toString());
        dto.put("clientId", kb.clientId().value().toString());
        dto.put("name", kb.name());
        dto.put("description", kb.description());
        return dto;
    }

}
