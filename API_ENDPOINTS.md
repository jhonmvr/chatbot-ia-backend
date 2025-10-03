# 📡 API Endpoints - Chatbot WhatsApp IA

## Resumen de APIs Disponibles

El sistema expone múltiples APIs REST para diferentes funcionalidades.

---

## 🔌 Webhooks de WhatsApp

### 1. Webhook Genérico (Multi-proveedor)
**Base URL**: `/webhook/whatsapp`

#### GET - Verificación
```http
GET /webhook/whatsapp?hub.mode=subscribe&hub.verify_token=TOKEN&hub.challenge=CHALLENGE
```

**Respuesta**:
```
200 OK
CHALLENGE
```

#### POST - Recibir Mensajes
```http
POST /webhook/whatsapp
Content-Type: application/json

{
  "from": "+593999999999",
  "text": "Hola, ¿cuáles son tus horarios?",
  "profileName": "Juan Pérez",
  "messageId": "msg_123"
}
```

**Respuesta**:
```json
{
  "success": true,
  "conversationId": "uuid",
  "response": "Nuestros horarios son..."
}
```

#### POST - Test
```http
POST /webhook/whatsapp/test
Content-Type: application/json

{
  "message": "¿Cuánto cuesta el servicio?"
}
```

---

### 2. Webhook Meta WhatsApp Business API
**Base URL**: `/webhooks/whatsapp/meta`

#### GET - Verificación
```http
GET /webhooks/whatsapp/meta?hub.mode=subscribe&hub.verify_token=TOKEN&hub.challenge=CHALLENGE
```

#### POST - Recibir Mensajes
Recibe el payload completo de Meta WhatsApp Business API.

```http
POST /webhooks/whatsapp/meta
Content-Type: application/json

{
  "object": "whatsapp_business_account",
  "entry": [...]
}
```

---

## 💬 API de Conversaciones

**Base URL**: `/api/conversations`

### Listar Conversaciones
```http
GET /api/conversations?clientId=uuid&from=2025-01-01&to=2025-12-31
```

**Respuesta**:
```json
[
  {
    "id": "uuid",
    "clientId": "uuid",
    "contactId": "uuid",
    "status": "ACTIVE",
    "startedAt": "2025-10-03T10:30:00Z",
    "closedAt": null
  }
]
```

### Obtener Conversación con Historial
```http
GET /api/conversations/{id}
```

**Respuesta**:
```json
{
  "id": "uuid",
  "clientId": "uuid",
  "contactId": "uuid",
  "status": "ACTIVE",
  "startedAt": "2025-10-03T10:30:00Z",
  "messageCount": 10,
  "messages": [
    {
      "id": "uuid",
      "direction": "IN",
      "body": "Hola",
      "createdAt": "2025-10-03T10:30:00Z",
      "status": "DELIVERED"
    }
  ]
}
```

### Cerrar Conversación
```http
POST /api/conversations/{id}/close
```

**Respuesta**:
```json
{
  "status": "success",
  "message": "Conversación cerrada exitosamente"
}
```

### Estadísticas
```http
GET /api/conversations/stats?clientId=uuid
```

**Respuesta**:
```json
{
  "total": 150,
  "active": 25,
  "closed": 125,
  "clientId": "uuid"
}
```

---

## 📚 API de Knowledge Base

**Base URL**: `/api/knowledge-base`

### Crear/Actualizar Knowledge Base
```http
POST /api/knowledge-base
Content-Type: application/json

{
  "clientId": "uuid",
  "name": "FAQ General",
  "description": "Preguntas frecuentes del negocio"
}
```

**Respuesta**:
```json
{
  "status": "success",
  "kbId": "uuid",
  "message": "KB creado"
}
```

### Ingestar Documentos
```http
POST /api/knowledge-base/{kbId}/ingest
Content-Type: application/json

{
  "documents": [
    {
      "content": "Nuestros horarios son de lunes a viernes de 9am a 6pm",
      "title": "Horarios",
      "category": "Info General"
    },
    {
      "content": "Aceptamos pagos en efectivo y tarjeta",
      "title": "Formas de Pago"
    }
  ]
}
```

**Respuesta**:
```json
{
  "status": "success",
  "message": "Documentos ingestados exitosamente",
  "count": 2
}
```

### Buscar en Knowledge Base
```http
POST /api/knowledge-base/{kbId}/search
Content-Type: application/json

{
  "query": "¿Cuáles son los horarios?",
  "topK": 5
}
```

**Respuesta**:
```json
{
  "status": "success",
  "query": "¿Cuáles son los horarios?",
  "results": [
    "Nuestros horarios son de lunes a viernes de 9am a 6pm",
    "Los sábados atendemos de 10am a 2pm"
  ],
  "count": 2
}
```

### Listar Knowledge Bases
```http
GET /api/knowledge-base?clientId=uuid
```

**Respuesta**:
```json
[
  {
    "id": "uuid",
    "name": "FAQ General",
    "description": "Preguntas frecuentes",
    "documentCount": 50,
    "createdAt": "2025-10-01T00:00:00Z"
  }
]
```

### Obtener Knowledge Base
```http
GET /api/knowledge-base/{id}
```

**Respuesta**:
```json
{
  "id": "uuid",
  "clientId": "uuid",
  "name": "FAQ General",
  "description": "Preguntas frecuentes del negocio",
  "documentCount": 50,
  "createdAt": "2025-10-01T00:00:00Z",
  "updatedAt": "2025-10-03T10:00:00Z"
}
```

---

## 🏥 API de Salud y Monitoreo

**Base URL**: `/api`

### Health Check Básico
```http
GET /api/health
```

**Respuesta**:
```json
{
  "status": "UP",
  "timestamp": "2025-10-03T10:30:00Z",
  "application": "chatbot-ia"
}
```

### Health Check Completo
```http
GET /api/health/full
```

**Respuesta**:
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

### Información de la Aplicación
```http
GET /api/info
```

**Respuesta**:
```json
{
  "name": "chatbot-ia",
  "version": "1.0.0",
  "description": "Chatbot con IA para WhatsApp",
  "whatsappProvider": "meta",
  "aiProvider": "openai",
  "endpoints": {
    "webhook": "/webhook/whatsapp",
    "webhookMeta": "/webhooks/whatsapp/meta",
    "test": "/webhook/whatsapp/test",
    "conversations": "/api/conversations",
    "knowledgeBase": "/api/knowledge-base",
    "health": "/api/health"
  }
}
```

---

## 🔐 Autenticación

> **Nota**: Actualmente las APIs no tienen autenticación implementada.
> Para producción, se recomienda implementar:
> - JWT tokens
> - API Keys
> - OAuth 2.0

**Ejemplo de implementación futura**:
```http
Authorization: Bearer {token}
```

---

## 📊 Códigos de Respuesta HTTP

| Código | Descripción |
|--------|-------------|
| 200 | Éxito |
| 400 | Solicitud incorrecta |
| 401 | No autenticado |
| 403 | Prohibido |
| 404 | No encontrado |
| 500 | Error del servidor |

---

## 🧪 Ejemplos con cURL

### Test del Chatbot
```bash
curl -X POST http://localhost:8080/webhook/whatsapp/test \
  -H "Content-Type: application/json" \
  -d '{"message": "¿Cuáles son tus horarios?"}'
```

### Crear Knowledge Base
```bash
curl -X POST http://localhost:8080/api/knowledge-base \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "FAQ General",
    "description": "Preguntas frecuentes"
  }'
```

### Ingestar Documentos
```bash
curl -X POST http://localhost:8080/api/knowledge-base/{kbId}/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "documents": [
      {
        "content": "Horarios: Lunes a Viernes 9am-6pm",
        "title": "Horarios"
      }
    ]
  }'
```

### Buscar en KB
```bash
curl -X POST http://localhost:8080/api/knowledge-base/{kbId}/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "horarios",
    "topK": 3
  }'
```

### Health Check
```bash
curl http://localhost:8080/api/health
```

---

## 🚀 Testing con Postman

### Collection de Postman

Puedes importar esta colección en Postman:

```json
{
  "info": {
    "name": "Chatbot WhatsApp IA",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Webhook Test",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/webhook/whatsapp/test",
        "body": {
          "mode": "raw",
          "raw": "{\"message\": \"Hola\"}"
        }
      }
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "{{base_url}}/api/health"
      }
    }
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    }
  ]
}
```

---

## 📚 Documentación Adicional

- **Swagger/OpenAPI**: Considerar agregar en el futuro
- **GraphQL**: Alternativa a REST (futuro)
- **WebSockets**: Para comunicación en tiempo real (futuro)

---

## ✅ Checklist de Endpoints

| Endpoint | Implementado | Documentado | Probado |
|----------|--------------|-------------|---------|
| Webhook WhatsApp | ✅ | ✅ | ⏳ |
| Webhook Meta | ✅ | ✅ | ⏳ |
| Conversaciones | ✅ | ✅ | ⏳ |
| Knowledge Base | ✅ | ✅ | ⏳ |
| Health | ✅ | ✅ | ⏳ |

---

**¡Todas las APIs están implementadas y listas para usar! 🎉**

