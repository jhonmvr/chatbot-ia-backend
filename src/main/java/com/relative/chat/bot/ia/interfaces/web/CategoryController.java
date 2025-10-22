package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Category;
import com.relative.chat.bot.ia.domain.ports.messaging.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Controller REST para gestión de Categorías
 */
@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Gestión de categorías para contactos")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * Crear nueva categoría
     */
    @Operation(
        summary = "Crear nueva categoría",
        description = "Crea una nueva categoría para clasificar contactos"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCategory(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos de la nueva categoría",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateCategoryRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "name": "Clientes VIP",
                      "description": "Clientes con alto valor",
                      "color": "#FF6B35",
                      "icon": "star",
                      "sortOrder": 1
                    }
                    """)
            )
        )
        @Valid @RequestBody CreateCategoryRequest request
    ) {
        try {
            // Verificar si ya existe una categoría con ese nombre
            if (categoryRepository.findByName(request.getName()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", "Ya existe una categoría con el nombre: " + request.getName()
                ));
            }

            // Crear nueva categoría
            Category category = Category.create(
                request.getName(),
                request.getDescription(),
                request.getColor(),
                request.getIcon(),
                request.getSortOrder()
            );

            categoryRepository.save(category);

            log.info("Categoría creada: {} (ID: {})", category.name(), category.id().value());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "message", "Categoría creada exitosamente",
                "data", toDto(category)
            ));

        } catch (Exception e) {
            log.error("Error creando categoría: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener todas las categorías
     */
    @Operation(
        summary = "Listar categorías",
        description = "Obtiene todas las categorías, activas por defecto"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCategories(
        @Parameter(description = "Incluir categorías inactivas")
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        try {
            List<Category> categories = includeInactive ? 
                categoryRepository.findAll() : 
                categoryRepository.findActiveCategories();

            List<Map<String, Object>> categoryDtos = categories.stream()
                .map(this::toDto)
                .toList();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", categoryDtos,
                "total", categoryDtos.size()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo categorías: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener categoría por ID
     */
    @Operation(
        summary = "Obtener categoría por ID",
        description = "Obtiene una categoría específica por su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCategory(
        @Parameter(description = "ID de la categoría")
        @PathVariable String id
    ) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(UuidId.of(UUID.fromString(id)));

            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Categoría no encontrada: " + id
                ));
            }

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toDto(categoryOpt.get())
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", "ID inválido: " + id
            ));
        } catch (Exception e) {
            log.error("Error obteniendo categoría {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Actualizar categoría
     */
    @Operation(
        summary = "Actualizar categoría",
        description = "Actualiza una categoría existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
        @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(
        @Parameter(description = "ID de la categoría")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos actualizados de la categoría",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdateCategoryRequest.class)
            )
        )
        @Valid @RequestBody UpdateCategoryRequest request
    ) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(UuidId.of(UUID.fromString(id)));

            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Categoría no encontrada: " + id
                ));
            }

            Category existingCategory = categoryOpt.get();

            // Verificar si el nuevo nombre ya existe en otra categoría
            if (!existingCategory.name().equals(request.getName()) && 
                categoryRepository.existsByNameAndIdNot(request.getName(), existingCategory.id())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", "Ya existe una categoría con el nombre: " + request.getName()
                ));
            }

            // Actualizar categoría
            Category updatedCategory = existingCategory
                .withName(request.getName())
                .withDescription(request.getDescription())
                .withColor(request.getColor())
                .withIcon(request.getIcon())
                .withIsActive(request.getIsActive())
                .withSortOrder(request.getSortOrder());

            categoryRepository.save(updatedCategory);

            log.info("Categoría actualizada: {} (ID: {})", updatedCategory.name(), updatedCategory.id().value());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Categoría actualizada exitosamente",
                "data", toDto(updatedCategory)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", "ID inválido: " + id
            ));
        } catch (Exception e) {
            log.error("Error actualizando categoría {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Eliminar categoría
     */
    @Operation(
        summary = "Eliminar categoría",
        description = "Elimina una categoría (solo si no tiene contactos asociados)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categoría eliminada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
        @ApiResponse(responseCode = "409", description = "No se puede eliminar: tiene contactos asociados")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(
        @Parameter(description = "ID de la categoría")
        @PathVariable String id
    ) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(UuidId.of(UUID.fromString(id)));

            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Categoría no encontrada: " + id
                ));
            }

            Category category = categoryOpt.get();

            // Verificar si tiene contactos asociados
            Long contactCount = categoryRepository.countContactsByCategoryId(category.id());
            if (contactCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", "No se puede eliminar la categoría: tiene " + contactCount + " contactos asociados"
                ));
            }

            categoryRepository.delete(category.id());

            log.info("Categoría eliminada: {} (ID: {})", category.name(), category.id().value());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Categoría eliminada exitosamente"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", "ID inválido: " + id
            ));
        } catch (Exception e) {
            log.error("Error eliminando categoría {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Buscar categorías por nombre
     */
    @Operation(
        summary = "Buscar categorías",
        description = "Busca categorías por nombre que contenga el texto especificado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Búsqueda realizada exitosamente")
    })
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCategories(
        @Parameter(description = "Texto a buscar en el nombre")
        @RequestParam String q
    ) {
        try {
            List<Category> categories = categoryRepository.findByNameContaining(q);

            List<Map<String, Object>> categoryDtos = categories.stream()
                .map(this::toDto)
                .toList();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", categoryDtos,
                "total", categoryDtos.size(),
                "query", q
            ));

        } catch (Exception e) {
            log.error("Error buscando categorías con query '{}': {}", q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener categorías más utilizadas
     */
    @Operation(
        summary = "Categorías más utilizadas",
        description = "Obtiene las categorías ordenadas por número de contactos asociados"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    })
    @GetMapping("/most-used")
    public ResponseEntity<Map<String, Object>> getMostUsedCategories() {
        try {
            List<Category> categories = categoryRepository.findMostUsedCategories();

            List<Map<String, Object>> categoryDtos = categories.stream()
                .map(this::toDto)
                .toList();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", categoryDtos,
                "total", categoryDtos.size()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo categorías más utilizadas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    // ==================== DTOs ====================

    @Data
    public static class CreateCategoryRequest {
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        private String name;

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        private String description;

        @Size(max = 7, message = "El color debe ser un código hexadecimal válido")
        private String color;

        @Size(max = 50, message = "El icono no puede exceder 50 caracteres")
        private String icon;

        private Integer sortOrder = 0;
    }

    @Data
    public static class UpdateCategoryRequest {
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        private String name;

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        private String description;

        @Size(max = 7, message = "El color debe ser un código hexadecimal válido")
        private String color;

        @Size(max = 50, message = "El icono no puede exceder 50 caracteres")
        private String icon;

        private Boolean isActive = true;

        private Integer sortOrder = 0;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Map<String, Object> toDto(Category category) {
        return Map.of(
            "id", category.id().value().toString(),
            "name", category.name(),
            "description", category.description() != null ? category.description() : "",
            "color", category.color() != null ? category.color() : "",
            "icon", category.icon() != null ? category.icon() : "",
            "isActive", category.isActive(),
            "sortOrder", category.sortOrder(),
            "contactCount", category.contactIds().size(),
            "createdAt", category.createdAt().toString(),
            "updatedAt", category.updatedAt().toString()
        );
    }
}
