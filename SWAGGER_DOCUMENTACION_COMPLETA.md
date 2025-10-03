# 📚 Documentación Swagger Completa

## ✅ Todos los Controladores Documentados

Se han documentado **5 controladores** con anotaciones completas de Swagger/OpenAPI:

---

## 📋 Resumen de Controllers Documentados

### 1. **ConversationController** (`@interfaces/web/`)
**Tag:** `Conversaciones`  
**Base Path:** `/api/conversations`

#### Endpoints:
- **GET** `/api/conversations/{id}` - Obtener conversación con historial completo
- **POST** `/api/conversations/{id}/close` - Cerrar una conversación

---

### 2. **KnowledgeBaseController** (`@interfaces/web/`)
**Tag:** `Knowledge Base`  
**Base Path:** `/api/knowledge-base`

#### Endpoints:
- **POST** `/api/knowledge-base` - Crear una nueva base de conocimiento
- **GET** `/api/knowledge-base/{id}` - Obtener un Knowledge Base
- **POST** `/api/knowledge-base/{kbId}/ingest` - Ingestar documentos en el KB
- **POST** `/api/knowledge-base/{kbId}/search` - Buscar en el Knowledge Base

---

### 3. **HealthController** (`@interfaces/web/`)
**Tag:** `Sistema`  
**Base Path:** `/api`

#### Endpoints:
- **GET** `/api/health` - Health check básico
- **GET** `/api/health/full` - Health check detallado con dependencias
- **GET** `/api/info` - Información de la aplicación

---

### 4. **MetaWhatsAppWebhookController** (`@infrastructure/adapters/in/web/`)
**Tag:** `Webhooks`  
**Base Path:** `/webhooks/whatsapp/meta`

#### Endpoints:
- **GET** `/webhooks/whatsapp/meta` - Verificación del webhook de Meta WhatsApp
- **POST** `/webhooks/whatsapp/meta` - Recibir notificaciones de Meta WhatsApp

---

## 🎨 Anotaciones Utilizadas

### A Nivel de Clase
```java
@Tag(name = "Nombre", description = "Descripción del grupo")
```

### A Nivel de Método
```java
@Operation(
    summary = "Resumen corto",
    description = "Descripción detallada"
)
```

### Respuestas
```java
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Éxito",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(value = "{ ... }")
        )
    ),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "404", description = "No encontrado")
})
```

### Parámetros
```java
@Parameter(
    description = "Descripción del parámetro",
    required = true,
    example = "valor-ejemplo"
)
@PathVariable String id
```

### Request Body
```java
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "Descripción del body",
    required = true,
    content = @Content(
        mediaType = "application/json",
        examples = @ExampleObject(value = "{ ... }")
    )
)
@RequestBody Map<String, Object> request
```

---

## 📊 Ejemplos de Responses Documentados

### ConversationController - GET /api/conversations/{id}
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "123e4567-e89b-12d3-a456-426614174000",
  "contactId": "987fbc97-4bed-5078-9f07-9141ba07c9f3",
  "status": "OPEN",
  "startedAt": "2025-10-03T10:30:00Z",
  "closedAt": null,
  "messageCount": 5,
  "messages": [
    {
      "id": "msg-001",
      "direction": "INBOUND",
      "content": "Hola, tengo una consulta",
      "createdAt": "2025-10-03T10:30:00Z"
    }
  ]
}
```

### KnowledgeBaseController - POST /api/knowledge-base
**Request:**
```json
{
  "clientId": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Productos y Servicios",
  "description": "Base de conocimiento sobre nuestros productos"
}
```

**Response:**
```json
{
  "status": "success",
  "kbId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "KB creado"
}
```

### KnowledgeBaseController - POST /api/knowledge-base/{kbId}/ingest
**Request:**
```json
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
```

**Response:**
```json
{
  "status": "success",
  "message": "Documentos ingestados exitosamente",
  "count": 2
}
```

### KnowledgeBaseController - POST /api/knowledge-base/{kbId}/search
**Request:**
```json
{
  "query": "¿Cuál es el horario de atención?",
  "topK": 5
}
```

**Response:**
```json
{
  "status": "success",
  "query": "¿Cuál es el horario de atención?",
  "results": [
    "Nuestro horario de atención es de Lunes a Viernes de 9am a 6pm",
    "Los sábados atendemos de 10am a 2pm"
  ],
  "count": 2
}
```

### HealthController - GET /api/health/full
```json
{
  "status": "UP",
  "timestamp": "2025-10-03T10:30:00Z",
  "application": "chatbot-ia",
  "database": {
    "status": "UP",
    "driver": "PostgreSQL",
    "version": "16.0"
  },
  "services": {
    "whatsapp": {
      "provider": "meta",
      "status": "CONFIGURED"
    },
    "ai": {
      "provider": "openai",
      "status": "CONFIGURED"
    }
  }
}
```

### MetaWhatsAppWebhookController - POST /webhooks/whatsapp/meta
**Request (Ejemplo de payload de Meta):**
```json
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
      "changes": [
        {
          "field": "messages",
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "display_phone_number": "593987654321",
              "phone_number_id": "PHONE_NUMBER_ID"
            },
            "messages": [
              {
                "from": "593998765432",
                "id": "wamid.HBgLNTkzOTg3NjU0MzIyFQIAERgSOTkzOTE0NTY3ODkwMTIzNDUA",
                "timestamp": "1698765432",
                "type": "text",
                "text": {
                  "body": "Hola, tengo una pregunta"
                }
              }
            ]
          }
        }
      ]
    }
  ]
}
```

---

## 🚀 Cómo Usar

### 1. Iniciar la aplicación
```bash
./mvnw.cmd spring-boot:run
```

### 2. Acceder a Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 3. Ver documentación JSON
```
http://localhost:8080/api-docs
```

---

## 📦 Agrupación de Endpoints en Swagger

Los endpoints se agrupan por **Tags**:

1. **Conversaciones** - Gestión de conversaciones del chatbot
2. **Knowledge Base** - Gestión de la base de conocimiento (RAG)
3. **Sistema** - Monitoreo y estado del sistema
4. **Webhooks** - Endpoints para servicios externos

---

## 🎯 Características Implementadas

### ✅ Documentación Completa
- Descripción detallada de cada endpoint
- Ejemplos de request y response
- Códigos de estado HTTP
- Tipos de contenido (Content-Type)

### ✅ Ejemplos Realistas
- JSON válidos y funcionales
- UUIDs de ejemplo consistentes
- Datos de ejemplo con contexto del negocio

### ✅ Parámetros Documentados
- Descripción de cada parámetro
- Indicación de campos requeridos
- Ejemplos de valores válidos

### ✅ Respuestas de Error
- Documentación de errores 400, 404, 403
- Ejemplos de mensajes de error
- Estructura consistente de respuestas

---

## 💡 Tips de Uso

### Probar Endpoints desde Swagger
1. Abre Swagger UI en http://localhost:8080/swagger-ui.html
2. Expande el endpoint que deseas probar
3. Click en "Try it out"
4. Completa los parámetros
5. Click en "Execute"
6. Revisa la respuesta

### Ver Esquemas
- Swagger UI muestra automáticamente los esquemas de request/response
- Los ejemplos están incluidos en cada endpoint

### Filtrar Endpoints
- Usa el campo de búsqueda en Swagger UI
- Filtra por tag para ver solo un grupo de endpoints

---

## 📝 Notas Técnicas

### Métodos Privados Ocultos
Los métodos privados en `MetaWhatsAppWebhookController` están marcados con `@Hidden` para que no aparezcan en la documentación:
```java
@Hidden
private void processMessages(...) { ... }
```

### Webhook de Meta
El endpoint de verificación (GET) y el de notificaciones (POST) están completamente documentados con ejemplos del payload real de Meta WhatsApp Business API.

### Health Checks
Los endpoints de health check retornan información útil para monitoreo:
- Estado de la aplicación
- Estado de la base de datos
- Configuración de servicios externos (WhatsApp, AI)

---

## 🔗 Referencias

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI Docs (JSON):** http://localhost:8080/api-docs
- **OpenAPI Docs (YAML):** http://localhost:8080/api-docs.yaml

---

**¡Toda la API está completamente documentada y lista para usar!** 🎉

Ahora cualquier desarrollador puede:
1. Ver todos los endpoints disponibles
2. Entender qué hace cada uno
3. Probarlos directamente desde el navegador
4. Ver ejemplos de request y response
5. Integrar la API en otras aplicaciones

