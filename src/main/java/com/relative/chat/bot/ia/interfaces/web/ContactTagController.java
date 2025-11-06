package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ContactTag;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactTagRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para gestionar la relación entre Contactos y Etiquetas
 */
@Slf4j
@RestController
@RequestMapping("/api/contact-tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(
        name = "Contact-Tag",
        description = "Gestión de relaciones entre contactos y etiquetas"
)
public class ContactTagController {

    private final ContactTagRepository contactTagRepository;

    /**
     * Asocia una etiqueta a un contacto
     */
    @Operation(
            summary = "Asociar etiqueta a contacto",
            description = "Crea una relación entre un contacto y una etiqueta existente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Etiqueta asociada exitosamente al contacto"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o faltantes"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> addTagToContact(
            @RequestBody(
                    description = "Datos de la relación contacto-etiqueta",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddContactTagRequest.class),
                            examples = @ExampleObject(value = """
                    {
                      "contactId": "d6b8a14e-2df3-45b0-a1cd-8c4aabf04e6f",
                      "tagId": "e2f2c2de-6bc9-48de-bc13-4efdd3b2a85e"
                    }
                    """)
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AddContactTagRequest request
    ) {
        try {
            ContactTag contactTag = ContactTag.create(
                    new UuidId<>(request.getContactId()),
                    new UuidId<>(request.getTagId())
            );

            contactTagRepository.create(contactTag);

            log.info("Etiqueta {} asociada al contacto {}", request.getTagId(), request.getContactId());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "success",
                    "message", "Etiqueta asociada exitosamente al contacto"
            ));

        } catch (Exception e) {
            log.error("Error asociando etiqueta a contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Elimina la asociación entre un contacto y una etiqueta
     */
    @Operation(
            summary = "Eliminar etiqueta de contacto",
            description = "Elimina la relación entre un contacto y una etiqueta"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Relación no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/{contactId}/{tagId}")
    public ResponseEntity<Map<String, Object>> removeTagFromContact(
            @PathVariable UUID contactId,
            @PathVariable UUID tagId
    ) {
        try {
            contactTagRepository.delete(
                    new UuidId<>(contactId),
                    new UuidId<>(tagId)
            );

            log.info("Eliminada relación entre contacto {} y etiqueta {}", contactId, tagId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Etiqueta eliminada del contacto exitosamente"
            ));

        } catch (Exception e) {
            log.error("Error eliminando etiqueta del contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene todas las etiquetas asociadas a un contacto
     */
    @Operation(
            summary = "Listar etiquetas de un contacto",
            description = "Devuelve todas las etiquetas asociadas a un contacto específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "No se encontraron etiquetas para el contacto"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{contactId}/tags")
    public ResponseEntity<Map<String, Object>> getTagsByContactId(
            @PathVariable UUID contactId
    ) {
        try {
            List<ContactTag> contactTags = contactTagRepository.findAllByContactId(new UuidId<>(contactId));

            if (contactTags == null || contactTags.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "not_found",
                        "message", "No se encontraron etiquetas para el contacto especificado"
                ));
            }

            log.info("Encontradas {} etiquetas para el contacto {}", contactTags.size(), contactId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", contactTags.size(),
                    "data", contactTags
            ));

        } catch (Exception e) {
            log.error("Error obteniendo etiquetas del contacto {}: {}", contactId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    // DTO interno
    @Data
    public static class AddContactTagRequest {
        @NotNull
        @Schema(description = "UUID del contacto", example = "d6b8a14e-2df3-45b0-a1cd-8c4aabf04e6f")
        private UUID contactId;

        @NotNull
        @Schema(description = "UUID de la etiqueta", example = "e2f2c2de-6bc9-48de-bc13-4efdd3b2a85e")
        private UUID tagId;
    }
}