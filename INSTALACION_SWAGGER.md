# 📚 Swagger/OpenAPI Instalado

## ✅ Instalación Completa

### 1. **Dependencia Agregada** (`pom.xml`)

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.4</version>
</dependency>
```

**Nota:** Esta es la versión más reciente compatible con Spring Boot 3.x

---

### 2. **Configuración Personalizada** (`OpenApiConfig.java`)

Creado en: `src/main/java/com/relative/chat/bot/ia/infrastructure/config/OpenApiConfig.java`

Incluye:
- ✅ Información de la API (título, versión, descripción)
- ✅ Contacto y licencia
- ✅ Servidores (desarrollo y producción)

---

### 3. **Configuración en `application.yml`**

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
    try-it-out-enabled: true
  show-actuator: true
```

---

## 🚀 Cómo Usar Swagger

### 1. **Iniciar la Aplicación**

```bash
./mvnw.cmd spring-boot:run
```

### 2. **Acceder a Swagger UI**

Abre tu navegador en:

```
http://localhost:8080/swagger-ui.html
```

### 3. **Ver la Documentación JSON**

```
http://localhost:8080/api-docs
```

---

## 📝 Documentar tus Controllers

### Ejemplo Básico

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "API para gestionar conversaciones")
public class ConversationController {

    @Operation(
        summary = "Obtener historial de conversación",
        description = "Retorna el historial completo de mensajes de una conversación"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversación encontrada"),
        @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(
        @Parameter(description = "ID de la conversación", required = true)
        @PathVariable String id
    ) {
        // tu código aquí
    }
}
```

---

## 🎨 Anotaciones Útiles

### A nivel de Clase (Controller)

```java
@Tag(name = "Nombre", description = "Descripción del grupo de endpoints")
```

### A nivel de Método

```java
@Operation(
    summary = "Resumen corto",
    description = "Descripción detallada de lo que hace el endpoint"
)
```

### Respuestas

```java
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Éxito"),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "404", description = "No encontrado"),
    @ApiResponse(responseCode = "500", description = "Error del servidor")
})
```

### Parámetros

```java
@Parameter(
    description = "Descripción del parámetro",
    required = true,
    example = "12345"
)
@PathVariable String id
```

### Request Body

```java
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "Datos del cliente",
    required = true,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ClientDTO.class)
    )
)
@RequestBody ClientDTO client
```

### Ocultar Endpoints

```java
@Hidden  // No aparecerá en Swagger
@GetMapping("/internal")
public String internalEndpoint() {
    return "Este endpoint no se muestra en Swagger";
}
```

---

## 📋 Ejemplo Completo: ConversationController

```java
package com.relative.chat.bot.ia.interfaces.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversaciones", description = "Endpoints para gestionar conversaciones del chatbot")
public class ConversationController {

    private final GetConversationHistory getConversationHistory;
    private final CloseConversation closeConversation;

    @Operation(
        summary = "Obtener historial de conversación",
        description = "Retorna todos los mensajes de una conversación ordenados por fecha"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Historial obtenido exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Message.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conversación no encontrada"
        )
    })
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<Message>> getHistory(
        @Parameter(description = "ID de la conversación", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String conversationId
    ) {
        // implementación
    }

    @Operation(
        summary = "Cerrar conversación",
        description = "Marca una conversación como cerrada"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversación cerrada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Conversación no encontrada"),
        @ApiResponse(responseCode = "400", description = "La conversación ya está cerrada")
    })
    @PostMapping("/{conversationId}/close")
    public ResponseEntity<Void> close(
        @Parameter(description = "ID de la conversación", required = true)
        @PathVariable String conversationId
    ) {
        // implementación
    }
}
```

---

## 🔧 Configuración Avanzada

### Personalizar Swagger UI

En `application.yml`:

```yaml
springdoc:
  swagger-ui:
    # Ordenar operaciones por método HTTP
    operations-sorter: method
    # Ordenar tags alfabéticamente
    tags-sorter: alpha
    # Expandir operaciones por defecto
    doc-expansion: none  # none, list, full
    # Mostrar duración de las peticiones
    display-request-duration: true
    # Habilitar filtro de búsqueda
    filter: true
    # Tema oscuro
    # syntaxHighlight.theme: monokai
```

### Agrupar por Paquetes

```yaml
springdoc:
  packages-to-scan:
    - com.relative.chat.bot.ia.interfaces.web
    - com.relative.chat.bot.ia.infrastructure.adapters.in.web
```

### Excluir Paths

```yaml
springdoc:
  paths-to-exclude:
    - /actuator/**
    - /error
```

### Configurar Seguridad (JWT)

En `OpenApiConfig.java`:

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(...)
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(new Components()
            .addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Ingresa tu token JWT")
            )
        );
}
```

---

## 📊 Schemas y Validaciones

### Validaciones en DTOs

```java
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record ClientDTO(
    @Schema(description = "Nombre del cliente", example = "Juan Pérez", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 200)
    String name,

    @Schema(description = "Email del cliente", example = "juan@example.com")
    @Email
    String email,

    @Schema(description = "Teléfono en formato E164", example = "+593987654321")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$")
    String phone
) {}
```

---

## 🎯 URLs Importantes

| Descripción | URL |
|-------------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **API Docs (JSON)** | http://localhost:8080/api-docs |
| **API Docs (YAML)** | http://localhost:8080/api-docs.yaml |

---

## 🚀 Próximos Pasos

1. **Reiniciar la aplicación**
2. **Abrir** http://localhost:8080/swagger-ui.html
3. **Documentar tus controllers** con las anotaciones de Swagger
4. **Probar los endpoints** directamente desde Swagger UI

---

## 💡 Tips

### 1. Agregar Ejemplos

```java
@Schema(example = """
    {
        "name": "Juan Pérez",
        "email": "juan@example.com",
        "phone": "+593987654321"
    }
    """)
public record ClientDTO(...) {}
```

### 2. Ocultar Propiedades

```java
@Schema(hidden = true)
private String internalField;
```

### 3. Agrupar Endpoints

Usa `@Tag` con el mismo nombre en múltiples controllers para agruparlos.

### 4. Deprecar Endpoints

```java
@Deprecated
@Operation(deprecated = true)
@GetMapping("/old-endpoint")
public String oldEndpoint() {
    return "Usa /new-endpoint en su lugar";
}
```

---

## 📚 Recursos Adicionales

- **Documentación oficial:** https://springdoc.org/
- **Anotaciones:** https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations
- **Ejemplos:** https://github.com/springdoc/springdoc-openapi-demos

---

**¡Swagger instalado y listo para usar!** 🎉

Ahora puedes documentar tus APIs de forma interactiva y profesional.

