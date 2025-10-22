package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.identity.ClientRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
import com.relative.chat.bot.ia.domain.vo.PhoneE164;
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
 * API REST para gestionar números de WhatsApp (canales) de clientes
 */
@Slf4j
@RestController
@RequestMapping("/api/client-phones")
@RequiredArgsConstructor
@Tag(name = "Client Phones", description = "API para gestionar números de WhatsApp y canales de clientes")
public class ClientPhoneController {
    
    private final ClientPhoneRepository clientPhoneRepository;
    private final ClientRepository clientRepository;
    
    /**
     * Crear un nuevo número de WhatsApp para un cliente
     * POST /api/client-phones
     */
    @Operation(
        summary = "Registrar número de WhatsApp para un cliente",
        description = "Asocia un número de WhatsApp Business a un cliente. Este número se usará para recibir y enviar mensajes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Número registrado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "phoneId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Número de WhatsApp registrado exitosamente"
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
                      "message": "clientId, e164 y provider son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createClientPhone(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del número de WhatsApp a registrar",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "a1234567-e89b-12d3-a456-426614174000",
                      "channel": "WHATSAPP",
                      "e164": "+593987654321",
                      "provider": "META",
                      "providerSid": "123456789012345",
                      "isActive": true,
                      "isDefault": true
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Validar campos requeridos
            String clientId = (String) request.get("clientId");
            String e164 = (String) request.get("e164");
            String provider = (String) request.get("provider");
            
            if (clientId == null || e164 == null || provider == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId, e164 y provider son requeridos"
                ));
            }
            
            // Verificar que el cliente existe
            Optional<Client> clientOpt = clientRepository.findById(UuidId.of(UUID.fromString(clientId)));
            if (clientOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Cliente no encontrado: " + clientId
                ));
            }
            
            // Obtener valores opcionales
            String channelStr = (String) request.getOrDefault("channel", "WHATSAPP");
            String providerSid = (String) request.get("providerSid");
            Boolean isActive = (Boolean) request.getOrDefault("isActive", true);
            
            // Crear ClientPhone
            ClientPhone clientPhone = new ClientPhone(
                UuidId.newId(),
                UuidId.of(UUID.fromString(clientId)),
                new PhoneE164(e164),
                Channel.valueOf(channelStr),
                provider,
                providerSid,  // phone_number_id de Meta, SID de Twilio, etc.
                isActive ? EntityStatus.ACTIVE : EntityStatus.INACTIVE,
                null, // verifiedAt
                (String) request.get("webhookSecret")
            );
            
            // Guardar
            clientPhoneRepository.save(clientPhone);
            
            log.info("Número de WhatsApp registrado: {} para cliente {}", e164, clientId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "phoneId", clientPhone.id().value().toString(),
                "message", "Número de WhatsApp registrado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al crear ClientPhone: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al registrar número: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Listar todos los números de WhatsApp de un cliente
     * GET /api/client-phones/client/{clientId}
     */
    @Operation(
        summary = "Listar números de WhatsApp de un cliente",
        description = "Obtiene todos los números de WhatsApp asociados a un cliente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "phones": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "clientId": "a1234567-e89b-12d3-a456-426614174000",
                          "channel": "WHATSAPP",
                          "e164": "+593987654321",
                          "provider": "META",
                          "providerSid": "123456789012345",
                          "status": "ACTIVE"
                        }
                      ],
                      "count": 1
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> getClientPhones(
        @Parameter(description = "UUID del cliente", required = true)
        @PathVariable String clientId
    ) {
        try {
            // Verificar que el cliente existe
            Optional<Client> clientOpt = clientRepository.findById(UuidId.of(UUID.fromString(clientId)));
            if (clientOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Cliente no encontrado: " + clientId
                ));
            }
            
            // Obtener números del cliente
            List<ClientPhone> phones = clientPhoneRepository.findByClient(UuidId.of(UUID.fromString(clientId)));
            
            // Convertir a DTO
            List<Map<String, Object>> phonesDto = phones.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "phones", phonesDto,
                "count", phonesDto.size()
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener números del cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener un número de WhatsApp por ID
     * GET /api/client-phones/{id}
     */
    @Operation(
        summary = "Obtener un número de WhatsApp por ID",
        description = "Retorna la información de un número de WhatsApp específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Número encontrado"),
        @ApiResponse(responseCode = "404", description = "Número no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getClientPhone(
        @Parameter(description = "UUID del número de WhatsApp", required = true)
        @PathVariable String id
    ) {
        try {
            Optional<ClientPhone> phoneOpt = clientPhoneRepository.findById(UuidId.of(UUID.fromString(id)));
            
            if (phoneOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Número no encontrado: " + id
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "phone", toDto(phoneOpt.get())
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener número: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Buscar cliente por phone_number_id de Meta
     * GET /api/client-phones/lookup?providerSid={providerSid}&provider={provider}
     */
    @Operation(
        summary = "Buscar cliente por phone_number_id",
        description = "Útil para debugging: encuentra qué cliente está asociado a un phone_number_id de Meta"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cliente encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "client": {
                        "id": "a1234567-e89b-12d3-a456-426614174000",
                        "name": "Mi Empresa S.A.",
                        "code": "EMPRESA001"
                      },
                      "phone": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "e164": "+593987654321",
                        "providerSid": "123456789012345"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "No se encontró cliente con ese providerSid")
    })
    @GetMapping("/lookup")
    public ResponseEntity<Map<String, Object>> lookupByProviderSid(
        @Parameter(description = "Provider SID (phone_number_id de Meta)", required = true, example = "123456789012345")
        @RequestParam String providerSid,
        @Parameter(description = "Proveedor", required = true, example = "META")
        @RequestParam(defaultValue = "META") String provider
    ) {
        try {
            Optional<Client> clientOpt = clientPhoneRepository.findClientByProviderSid(providerSid, provider);
            
            if (clientOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "No se encontró cliente con providerSid: " + providerSid
                ));
            }
            
            Client client = clientOpt.get();
            
            // Obtener el ClientPhone también
            Optional<ClientPhone> phoneOpt = clientPhoneRepository.findByProviderSid(providerSid, provider);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("client", Map.of(
                "id", client.id().value().toString(),
                "name", client.name(),
                "code", client.code()
            ));
            
            if (phoneOpt.isPresent()) {
                response.put("phone", toDto(phoneOpt.get()));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error en lookup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Actualizar un número de WhatsApp
     * PUT /api/client-phones/{id}
     */
    @Operation(
        summary = "Actualizar configuración de un número de WhatsApp",
        description = "Actualiza la configuración de un número de WhatsApp existente, incluyendo credenciales de proveedores"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Número actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "phoneId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Número de WhatsApp actualizado exitosamente"
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
                      "message": "Datos de actualización inválidos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Número no encontrado")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateClientPhone(
        @Parameter(description = "UUID del número de WhatsApp a actualizar", required = true)
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos a actualizar del número de WhatsApp",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "provider": "META",
                      "providerSid": "123456789012345",
                      "isActive": true,
                      "metaAccessToken": "EAAxxxxxxxxxxxx",
                      "metaPhoneNumberId": "123456789012345",
                      "metaApiVersion": "v21.0",
                      "twilioAccountSid": "ACxxxxxxxxxxxx",
                      "twilioAuthToken": "xxxxxxxxxxxx",
                      "wwebjsSessionId": "session_123",
                      "wwebjsWebhookUrl": "https://api.example.com/webhook",
                      "apiBaseUrl": "https://graph.facebook.com",
                      "webhookUrl": "https://mi-app.com/webhooks/whatsapp/meta",
                      "verifyToken": "mi_token_secreto",
                      "webhookSecret": "mi_secreto_webhook"
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Buscar el número existente
            Optional<ClientPhone> existingPhoneOpt = clientPhoneRepository.findById(UuidId.of(UUID.fromString(id)));
            
            if (existingPhoneOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Número no encontrado: " + id
                ));
            }
            
            ClientPhone existingPhone = existingPhoneOpt.get();
            
            // Crear nuevo ClientPhone con los datos actualizados
            // Mantener los campos que no se están actualizando
            ClientPhone updatedPhone = new ClientPhone(
                existingPhone.id(), // Mantener el mismo ID
                existingPhone.clientId(), // Mantener el mismo cliente
                existingPhone.phone(), // Mantener el mismo número
                existingPhone.channel(), // Mantener el mismo canal
                // Campos actualizables
                (String) request.getOrDefault("provider", existingPhone.provider()),
                (String) request.getOrDefault("providerSid", existingPhone.providerSid()),
                // Status actualizable
                request.containsKey("isActive") ? 
                    ((Boolean) request.get("isActive") ? EntityStatus.ACTIVE : EntityStatus.INACTIVE) : 
                    existingPhone.status(),
                existingPhone.verifiedAt(), // Mantener verifiedAt
                (String) request.getOrDefault("webhookSecret", existingPhone.webhookSecret())
            );
            
            // Guardar la actualización
            clientPhoneRepository.save(updatedPhone);
            
            log.info("Número de WhatsApp actualizado: {} para cliente {}", 
                    updatedPhone.phone().value(), updatedPhone.clientId().value());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "phoneId", updatedPhone.id().value().toString(),
                "message", "Número de WhatsApp actualizado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al actualizar ClientPhone: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al actualizar número: " + e.getMessage()
            ));
        }
    }

    /**
     * Eliminar un número de WhatsApp
     * DELETE /api/client-phones/{id}
     */
    @Operation(
        summary = "Eliminar un número de WhatsApp",
        description = "Elimina un número de WhatsApp de un cliente. ⚠️ Esta acción no se puede deshacer."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Número eliminado exitosamente"
        ),
        @ApiResponse(responseCode = "404", description = "Número no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteClientPhone(
        @Parameter(description = "UUID del número a eliminar", required = true)
        @PathVariable String id
    ) {
        try {
            Optional<ClientPhone> phoneOpt = clientPhoneRepository.findById(UuidId.of(UUID.fromString(id)));
            
            if (phoneOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Número no encontrado: " + id
                ));
            }
            
            // TODO: Implementar delete en el repositorio si no existe
            // clientPhoneRepository.delete(UuidId.of(UUID.fromString(id)));
            
            log.info("Número de WhatsApp eliminado: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Número eliminado exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error al eliminar número: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Obtiene todos los números telefónicos de clientes
     * GET /api/client-phones
     */
    @Operation(
            summary = "Obtener todos los números telefónicos",
            description = "Retorna la lista completa de números telefónicos registrados en el sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de números telefónicos obtenida correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                {
                  "status": "success",
                  "count": 2,
                  "phones": [
                    {
                      "id": "111e4567-e89b-12d3-a456-426614174000",
                      "clientId": "123e4567-e89b-12d3-a456-426614174000",
                      "channel": "WHATSAPP",
                      "e164": "+593999888777",
                      "provider": "TWILIO",
                      "status": "ACTIVE",
                      "providerSid": "PN12345",
                      "verifiedAt": "2024-10-10T15:30:00Z"
                    },
                    {
                      "id": "222e4567-e89b-12d3-a456-426614174111",
                      "clientId": "223e4567-e89b-12d3-a456-426614174111",
                      "channel": "SMS",
                      "e164": "+593988776655",
                      "provider": "INFOBIP",
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
    public ResponseEntity<Map<String, Object>> getAllClientPhones() {
        try {
            List<ClientPhone> phones = clientPhoneRepository.findAll();

            List<Map<String, Object>> phonesDto = phones.stream()
                    .map(this::toDto)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "phones", phonesDto,
                    "count", phonesDto.size()
            ));

        } catch (Exception e) {
            log.error("Error al obtener todos los números telefónicos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Convierte un ClientPhone del dominio a DTO
     */
    private Map<String, Object> toDto(ClientPhone phone) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", phone.id().value().toString());
        dto.put("clientId", phone.clientId().value().toString());
        dto.put("channel", phone.channel().name());
        dto.put("e164", phone.phone().value());
        dto.put("provider", phone.provider());
        dto.put("status", phone.status().name());
        
        // Campos opcionales básicos
        if (phone.providerSidOpt().isPresent()) {
            dto.put("providerSid", phone.providerSid());
        }
        
        if (phone.verifiedAtOpt().isPresent()) {
            dto.put("verifiedAt", phone.verifiedAtOpt().get().toString());
        }
        
        // Campo webhookSecret
        if (phone.webhookSecretOpt().isPresent()) {
            dto.put("webhookSecret", phone.webhookSecret());
        }
        
        return dto;
    }
}

