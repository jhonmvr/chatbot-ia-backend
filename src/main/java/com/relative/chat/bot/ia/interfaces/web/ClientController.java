package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.ports.identity.ClientRepository;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * API REST para gestión de clientes
 */
@Slf4j
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "API para gestionar clientes del sistema")
public class ClientController {
    
    private final ClientRepository clientRepository;
    
    /**
     * Crea un nuevo cliente
     * POST /api/clients
     */
    @Operation(
        summary = "Crear un nuevo cliente",
        description = "Crea un cliente en el sistema. El cliente es la entidad principal que agrupa conversaciones y knowledge bases."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cliente creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "clientId": "123e4567-e89b-12d3-a456-426614174000",
                      "code": "CLI-001",
                      "message": "Cliente creado exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "code y name son requeridos"
                    }
                    """)
            )
        )
    })
    @PostMapping
    public ResponseEntity<Map<String, String>> createClient(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del cliente a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "code": "CLI-001",
                      "name": "Empresa Demo S.A.",
                      "status": "ACTIVE"
                    }
                    """)
            )
        )
        @RequestBody Map<String, String> request
    ) {
        try {
            String code = request.get("code");
            String name = request.get("name");
            String statusStr = request.getOrDefault("status", "ACTIVE");
            
            if (code == null || code.isBlank() || name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "code y name son requeridos"
                ));
            }
            
            // Verificar si ya existe un cliente con ese código
            Optional<Client> existing = clientRepository.findByCode(code);
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Ya existe un cliente con el código: " + code
                ));
            }
            
            EntityStatus status = EntityStatus.valueOf(statusStr);
            
            // Crear nuevo cliente
            Client client = new Client(
                UuidId.newId(),
                code,
                name,
                status
            );
            
            // Guardar
            clientRepository.save(client);
            
            log.info("Cliente creado: id={}, code={}, name={}", 
                client.id().value(), code, name);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "clientId", client.id().value().toString(),
                "code", code,
                "message", "Cliente creado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Status inválido. Valores permitidos: ACTIVE, INACTIVE"
            ));
        } catch (Exception e) {
            log.error("Error al crear cliente: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Obtiene un cliente por ID
     * GET /api/clients/{id}
     */
    @Operation(
        summary = "Obtener un cliente por ID",
        description = "Retorna la información de un cliente específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cliente encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "code": "CLI-001",
                      "name": "Empresa Demo S.A.",
                      "status": "ACTIVE"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getClient(
        @Parameter(description = "UUID del cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable String id
    ) {
        try {
            UuidId<Client> clientId = UuidId.of(UUID.fromString(id));
            Optional<Client> client = clientRepository.findById(clientId);
            
            if (client.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Client c = client.get();
            Map<String, String> response = new HashMap<>();
            response.put("id", c.id().value().toString());
            response.put("code", c.code());
            response.put("name", c.name());
            response.put("status", c.status().name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener cliente: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Obtiene un cliente por código
     * GET /api/clients/by-code/{code}
     */
    @Operation(
        summary = "Obtener un cliente por código",
        description = "Retorna la información de un cliente buscando por su código único"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cliente encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "code": "CLI-001",
                      "name": "Empresa Demo S.A.",
                      "status": "ACTIVE"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/by-code/{code}")
    public ResponseEntity<Map<String, Object>> getClientByCode(
        @Parameter(description = "Código del cliente", required = true, example = "CLI-001")
        @PathVariable String code
    ) {
        try {
            Optional<Client> client = clientRepository.findByCode(code);
            
            if (client.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Client c = client.get();
            Map<String, String> response = new HashMap<>();
            response.put("id", c.id().value().toString());
            response.put("code", c.code());
            response.put("name", c.name());
            response.put("status", c.status().name());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "client", response
            ));

        } catch (Exception e) {
            log.error("Error al obtener cliente por código: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene todos los clientes
     * GET /api/clients
     */
    @Operation(
            summary = "Obtener todos los clientes",
            description = "Retorna la lista completa de clientes registrados en el sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de clientes obtenida correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                {
                  "status": "success",
                  "count": 2,
                  "clients": [
                    {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "code": "CLI-001",
                      "name": "Empresa Demo S.A.",
                      "status": "ACTIVE"
                    },
                    {
                      "id": "223e4567-e89b-12d3-a456-426614174111",
                      "code": "CLI-002",
                      "name": "Comercial Andina S.A.",
                      "status": "INACTIVE"
                    }
                  ]
                }
                """)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllClients() {
        try {
            List<Client> clients = clientRepository.findAll();

            List<Map<String, Object>> clientsDto = clients.stream()
                    .map(this::toDto)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "clients", clientsDto,
                    "count", clientsDto.size()
            ));

        } catch (Exception e) {
            log.error("Error al obtener la lista de clientes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Convierte un Client del dominio a DTO
     */
    private Map<String, Object> toDto(Client client) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", client.id().value().toString());
        dto.put("code", client.code());
        dto.put("name", client.name());
        dto.put("status", client.status().name());
        return dto;
    }
}

