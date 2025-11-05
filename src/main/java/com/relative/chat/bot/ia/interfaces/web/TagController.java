package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Tag;
import com.relative.chat.bot.ia.domain.ports.messaging.TagRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller REST para gestión de Etiquetas
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Gestión de etiquetas para contactos")
public class TagController {

    private final TagRepository tagRepository;

    /**
     * Crear nueva etiqueta
     */
    @Operation(
        summary = "Crear nueva etiqueta",
        description = "Crea una nueva etiqueta para etiquetar contactos"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Etiqueta creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Ya existe una etiqueta con ese nombre")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTag(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos de la nueva etiqueta",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateTagRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "name": "Lead Caliente",
                      "description": "Contactos con alta probabilidad de conversión",
                      "color": "#28A745"
                    }
                    """)
            )
        )
        @Valid @RequestBody CreateTagRequest request
    ) {
        try {
            // Verificar si ya existe una etiqueta con ese nombre
            if (tagRepository.findByName(request.getName()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", "Ya existe una etiqueta con el nombre: " + request.getName()
                ));
            }

            // Crear nueva etiqueta
            Tag tag = Tag.create(
                request.getName(),
                request.getDescription(),
                request.getColor()
            );

            tagRepository.save(tag);

            log.info("Etiqueta creada: {} (ID: {})", tag.name(), tag.id().value());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "message", "Etiqueta creada exitosamente",
                "data", toDto(tag)
            ));

        } catch (Exception e) {
            log.error("Error creando etiqueta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener todas las etiquetas
     */
    @Operation(
        summary = "Listar etiquetas",
        description = "Obtiene todas las etiquetas, activas por defecto"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de etiquetas obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTags(
        @Parameter(description = "Incluir etiquetas inactivas")
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        try {
            List<Tag> tags = includeInactive ? 
                tagRepository.findAll() : 
                tagRepository.findActiveTags();

            List<Map<String, Object>> tagDtos = tags.stream()
                .map(this::toDto)
                .toList();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", tagDtos,
                "total", tagDtos.size()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo etiquetas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener etiqueta por ID
     */
    @Operation(
        summary = "Obtener etiqueta por ID",
        description = "Obtiene una etiqueta específica por su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Etiqueta encontrada"),
        @ApiResponse(responseCode = "404", description = "Etiqueta no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTag(
        @Parameter(description = "ID de la etiqueta")
        @PathVariable String id
    ) {
        try {
            Optional<Tag> tagOpt = tagRepository.findById(UuidId.of(UUID.fromString(id)));

            if (tagOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Etiqueta no encontrada: " + id
                ));
            }

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toDto(tagOpt.get())
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", "ID inválido: " + id
            ));
        } catch (Exception e) {
            log.error("Error obteniendo etiqueta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Actualizar etiqueta
     */
    @Operation(
        summary = "Actualizar etiqueta",
        description = "Actualiza una etiqueta existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Etiqueta actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Etiqueta no encontrada"),
        @ApiResponse(responseCode = "409", description = "Ya existe una etiqueta con ese nombre")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTag(
        @Parameter(description = "ID de la etiqueta")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos actualizados de la etiqueta",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdateTagRequest.class)
            )
        )
        @Valid @RequestBody UpdateTagRequest request
    ) {
        try {
            Optional<Tag> tagOpt = tagRepository.findById(UuidId.of(UUID.fromString(id)));

            if (tagOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Etiqueta no encontrada: " + id
                ));
            }

            Tag existingTag = tagOpt.get();

            // Verificar si el nuevo nombre ya existe en otra etiqueta
            if (!existingTag.name().equals(request.getName()) && 
                tagRepository.existsByNameAndIdNot(request.getName(), existingTag.id())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", "Ya existe una etiqueta con el nombre: " + request.getName()
                ));
            }

            // Actualizar etiqueta
            Tag updatedTag = existingTag
                .withName(request.getName())
                .withDescription(request.getDescription())
                .withColor(request.getColor())
                .withIsActive(request.getIsActive());

            tagRepository.save(updatedTag);

            log.info("Etiqueta actualizada: {} (ID: {})", updatedTag.name(), updatedTag.id().value());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Etiqueta actualizada exitosamente",
                "data", toDto(updatedTag)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", "ID inválido: " + id
            ));
        } catch (Exception e) {
            log.error("Error actualizando etiqueta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Eliminar etiqueta
     */
    @Operation(
        summary = "Eliminar etiqueta",
        description = "Elimina una etiqueta (solo si no tiene contactos asociados)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Etiqueta eliminada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Etiqueta no encontrada"),
        @ApiResponse(responseCode = "409", description = "No se puede eliminar: tiene contactos asociados")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTag(
        @Parameter(description = "ID de la etiqueta")
        @PathVariable String id
    ) {
        try {
            Optional<Tag> tagOpt = tagRepository.findById(UuidId.of(UUID.fromString(id)));

            if (tagOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Etiqueta no encontrada: " + id
                ));
            }

            Tag tag = tagOpt.get();

            // Verificar si tiene contactos asociados
            Long contactCount = tagRepository.countContactsByTagId(tag.id());
            if (contactCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", "No se puede eliminar la etiqueta: tiene " + contactCount + " contactos asociados"
                ));
            }

            tagRepository.delete(tag.id());

            log.info("Etiqueta eliminada: {} (ID: {})", tag.name(), tag.id().value());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Etiqueta eliminada exitosamente"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", "ID inválido: " + id
            ));
        } catch (Exception e) {
            log.error("Error eliminando etiqueta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Buscar etiquetas por nombre
     */
    @Operation(
        summary = "Buscar etiquetas",
        description = "Busca etiquetas por nombre que contenga el texto especificado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Búsqueda realizada exitosamente")
    })
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTags(
        @Parameter(description = "Texto a buscar en el nombre")
        @RequestParam String q
    ) {
        try {
            List<Tag> tags = tagRepository.findByNameContaining(q);

            List<Map<String, Object>> tagDtos = tags.stream()
                .map(this::toDto)
                .toList();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", tagDtos,
                "total", tagDtos.size(),
                "query", q
            ));

        } catch (Exception e) {
            log.error("Error buscando etiquetas con query '{}': {}", q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener etiquetas sugeridas
     */
    @Operation(
        summary = "Etiquetas sugeridas",
        description = "Obtiene etiquetas sugeridas basadas en texto de búsqueda"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sugerencias obtenidas exitosamente")
    })
    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getSuggestedTags(
        @Parameter(description = "Texto para generar sugerencias")
        @RequestParam String q
    ) {
        try {
            List<Tag> tags = tagRepository.findSuggestedTags(q);

            List<Map<String, Object>> tagDtos = tags.stream()
                .map(this::toDto)
                .toList();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", tagDtos,
                "total", tagDtos.size(),
                "query", q
            ));

        } catch (Exception e) {
            log.error("Error obteniendo sugerencias de etiquetas con query '{}': {}", q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener etiquetas más utilizadas
     */
    @Operation(
        summary = "Etiquetas más utilizadas",
        description = "Obtiene las etiquetas ordenadas por número de contactos asociados"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    })
    @GetMapping("/most-used")
    public ResponseEntity<Map<String, Object>> getMostUsedTags() {
        try {
            List<Tag> tags = tagRepository.findMostUsedTags();

            List<Map<String, Object>> tagDtos = tags.stream()
                .map(this::toDto)
                .toList();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", tagDtos,
                "total", tagDtos.size()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo etiquetas más utilizadas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener etiquetas populares
     */
    @Operation(
        summary = "Etiquetas populares",
        description = "Obtiene etiquetas con más de X usos"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    })
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularTags(
        @Parameter(description = "Número mínimo de usos")
        @RequestParam(defaultValue = "5") Integer minUsageCount
    ) {
        try {
            List<Tag> tags = tagRepository.findPopularTags(minUsageCount);

            List<Map<String, Object>> tagDtos = tags.stream()
                .map(this::toDto)
                .toList();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", tagDtos,
                "total", tagDtos.size(),
                "minUsageCount", minUsageCount
            ));

        } catch (Exception e) {
            log.error("Error obteniendo etiquetas populares: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    // ==================== DTOs ====================

    @Data
    public static class CreateTagRequest {
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        private String name;

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        private String description;

        @Size(max = 7, message = "El color debe ser un código hexadecimal válido")
        private String color;
    }

    @Data
    public static class UpdateTagRequest {
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        private String name;

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        private String description;

        @Size(max = 7, message = "El color debe ser un código hexadecimal válido")
        private String color;

        private Boolean isActive = true;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Map<String, Object> toDto(Tag tag) {
        return Map.of(
            "id", tag.id().value().toString(),
            "name", tag.name(),
            "description", tag.description() != null ? tag.description() : "",
            "color", tag.color() != null ? tag.color() : "",
            "isActive", tag.isActive(),
            "usageCount", tag.usageCount(),
            "contactCount", tag.contactIds().size(),
            "createdAt", tag.createdAt().toString(),
            "updatedAt", tag.updatedAt().toString()
        );
    }
}
