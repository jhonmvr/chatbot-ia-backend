# Capa de Aplicación - Chatbot IA

Esta capa contiene los **casos de uso** (use cases) que orquestan la lógica de negocio del chatbot, coordinando las entidades de dominio y los servicios externos.

## Estructura

```
application/
├── dto/                      # Data Transfer Objects
│   ├── MessageCommand.java   # Comando para recibir mensajes
│   ├── MessageResponse.java  # Respuesta de procesamiento
│   └── ConversationInfo.java # Info de conversación
│
├── ports/out/                # Puertos de salida (interfaces)
│   ├── EmbeddingsPort.java   # Servicio de embeddings
│   ├── VectorStore.java      # Almacén vectorial
│   ├── WhatsAppService.java  # Servicio de WhatsApp (Twilio, etc.)
│   └── AIService.java        # Servicio de IA (OpenAI, etc.)
│
└── usecases/                 # Casos de uso
    ├── ReceiveWhatsAppMessage.java  # 🎯 Flujo principal
    ├── SendMessage.java
    ├── ProcessMessageWithAI.java
    ├── StartConversation.java
    ├── CloseConversation.java
    ├── GetOrCreateContact.java
    ├── GetConversationHistory.java
    ├── SearchDocuments.java
    ├── IngestDocuments.java
    └── MigrateVectors.java
```

## Casos de Uso Principales

### 1. **ReceiveWhatsAppMessage** (Orquestador Principal)
Flujo completo de recepción y procesamiento de mensajes de WhatsApp:

1. Valida el cliente
2. Obtiene o crea el contacto
3. Obtiene o crea la conversación
4. Guarda el mensaje entrante
5. Genera respuesta con IA
6. Envía la respuesta

**Uso:**
```java
MessageCommand command = new MessageCommand(
    "client-code",
    "+593999999999",      // Número del bot
    "+593988888888",      // Número del usuario
    "Usuario Test",       // Nombre del usuario
    Channel.WHATSAPP,
    "¿Cuáles son tus horarios?",
    Instant.now(),
    "msg_external_id"
);

MessageResponse response = receiveWhatsAppMessage.handle(command);
```

### 2. **ProcessMessageWithAI**
Genera respuesta inteligente usando:
- **RAG (Retrieval-Augmented Generation)**: Busca contexto en knowledge base
- **Historial de conversación**: Mantiene contexto de la conversación
- **IA Generativa**: Genera respuesta natural

### 3. **SendMessage**
Envía mensajes a través del proveedor de WhatsApp (Twilio, Meta, etc.)

### 4. **StartConversation / CloseConversation**
Gestión del ciclo de vida de conversaciones

### 5. **GetOrCreateContact**
Manejo de contactos (usuarios de WhatsApp)

### 6. **Knowledge Base (KB)**
- **IngestDocuments**: Procesa e indexa documentos en el KB
- **SearchDocuments**: Búsqueda semántica en el KB
- **MigrateVectors**: Migración entre vector stores

## Puertos de Salida (Interfaces)

### EmbeddingsPort
Genera embeddings de texto para búsqueda semántica.

**Implementación sugerida**: OpenAI Embeddings, HuggingFace, etc.

### VectorStore
Almacenamiento y búsqueda de vectores.

**Implementación sugerida**: pgvector (PostgreSQL), Pinecone, Qdrant, Weaviate

### WhatsAppService
Envío de mensajes a través de WhatsApp.

**Implementación sugerida**: Twilio API, Meta WhatsApp Business API

### AIService
Generación de respuestas con IA.

**Implementación sugerida**: OpenAI GPT-4, Claude, Llama

## Flujo de Datos

```
Webhook WhatsApp
    ↓
WhatsAppController
    ↓
ReceiveWhatsAppMessage ──────┐
    ↓                        │
GetOrCreateContact           │ Orquestación
    ↓                        │
StartConversation            │
    ↓                        │
ProcessMessageWithAI ────────┘
    ├─→ SearchDocuments (KB)
    ├─→ GetConversationHistory
    └─→ AIService.generateResponse
    ↓
SendMessage
    └─→ WhatsAppService.sendMessage
```

## Dependencias

Esta capa depende de:
- **Dominio**: Entidades, Value Objects, Repositorios (interfaces)
- **Puertos de salida**: Servicios externos (interfaces)

Esta capa NO depende de:
- Infraestructura
- Frameworks específicos (excepto anotaciones de Spring para DI)
- Bases de datos específicas

## Patrones Aplicados

1. **Use Case / Command Pattern**: Cada caso de uso es una operación de negocio específica
2. **Dependency Inversion**: Depende de interfaces (puertos), no de implementaciones
3. **Single Responsibility**: Cada caso de uso tiene una responsabilidad única
4. **Transaction Script**: Los casos de uso orquestan transacciones
5. **DTO Pattern**: Separación entre modelos de dominio y de transferencia

## Próximos Pasos

### Implementaciones Faltantes

1. **ContactRepository** en `domain/ports/messaging/`
2. Implementaciones de puertos:
   - `WhatsAppServiceImpl` (Twilio o Meta)
   - `AIServiceImpl` (OpenAI)
   - `EmbeddingsPortImpl` (OpenAI)
   - `VectorStoreImpl` (pgvector)

3. **Búsqueda de conversaciones existentes** en `ReceiveWhatsAppMessage`
4. **Validación de token** en `WhatsAppController.verify()`
5. **Configuración externalizada** (application.yml)

### Mejoras Sugeridas

1. **Circuit Breaker** para servicios externos
2. **Retry Logic** en llamadas a APIs
3. **Rate Limiting** por cliente
4. **Caché** de embeddings
5. **Eventos de dominio** para auditoría
6. **Métricas** de uso y performance

