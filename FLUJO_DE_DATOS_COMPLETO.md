# 🔄 Flujo de Datos Completo - Chatbot WhatsApp IA

## 📋 Tabla de Contenidos
1. [Flujo Principal (Happy Path)](#flujo-principal)
2. [Flujo Detallado Paso a Paso](#flujo-detallado)
3. [Componentes y Responsabilidades](#componentes)
4. [Flujo de Datos RAG (IA)](#flujo-rag)
5. [Casos de Uso Alternativos](#casos-alternativos)

---

## 🎯 Flujo Principal (Happy Path)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    USUARIO DE WHATSAPP                              │
│                 Envía: "¿Cuáles son tus horarios?"                  │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                  META WHATSAPP BUSINESS API                         │
│         Recibe mensaje → Envía webhook a tu servidor               │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 1: RECEPCIÓN DEL WEBHOOK                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ MetaWhatsAppWebhookController                                 │ │
│  │ POST /webhooks/whatsapp/meta                                  │ │
│  │                                                                │ │
│  │ - Valida el webhook de Meta                                   │ │
│  │ - Extrae datos del payload:                                   │ │
│  │   • messageId: "wamid.xxx"                                    │ │
│  │   • from: "593999999999"                                      │ │
│  │   • body: "¿Cuáles son tus horarios?"                         │ │
│  │   • phoneNumberId: "123456..."                                │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 2: CREAR COMANDO                                             │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ MessageCommand                                                │ │
│  │ {                                                             │ │
│  │   clientCode: null,  // Se resuelve después                  │ │
│  │   phoneNumber: "123456...",  // phoneNumberId                │ │
│  │   contactPhone: "593999999999",                              │ │
│  │   contactName: "593999999999",                               │ │
│  │   channel: WHATSAPP,                                         │ │
│  │   content: "¿Cuáles son tus horarios?",                      │ │
│  │   receivedAt: "2025-10-03T14:30:00Z",                        │ │
│  │   externalId: "wamid.xxx"                                    │ │
│  │ }                                                             │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 3: ORQUESTACIÓN PRINCIPAL                                    │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ ReceiveWhatsAppMessage.handle(command)                       │ │
│  │                                                               │ │
│  │ Este es el ORQUESTADOR principal que coordina todo el flujo  │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 4: VALIDAR CLIENTE                                           │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ ClientRepository.findByCode("default-client")                │ │
│  │                      ↓                                        │ │
│  │         PostgreSQL: chatbotia.client                         │ │
│  │                      ↓                                        │ │
│  │ Cliente encontrado:                                          │ │
│  │   id: "550e8400-e29b-41d4-a716-446655440000"                │ │
│  │   code: "default-client"                                     │ │
│  │   name: "Mi Empresa"                                         │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 5: OBTENER O CREAR CONTACTO                                 │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ GetOrCreateContact.handle(clientId, phone, name, channel)   │ │
│  │                      ↓                                        │ │
│  │ ContactRepository.findByClientAndPhone(...)                  │ │
│  │                      ↓                                        │ │
│  │         PostgreSQL: chatbotia.contact                        │ │
│  │                      ↓                                        │ │
│  │ Si existe → retorna contacto existente                       │ │
│  │ Si NO existe:                                                │ │
│  │   - Crea nuevo Contact                                       │ │
│  │   - Asigna ID: UuidId.newId()                               │ │
│  │   - Guarda en BD                                            │ │
│  │   - Retorna nuevo contacto                                   │ │
│  │                                                               │ │
│  │ Contacto resultante:                                         │ │
│  │   id: "7f3e4d5c-..."                                         │ │
│  │   clientId: "550e8400-..."                                   │ │
│  │   fullName: "593999999999"                                   │ │
│  │   phoneE164: "593999999999"                                  │ │
│  │   status: ACTIVE                                             │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 6: INICIAR O CONTINUAR CONVERSACIÓN                         │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ StartConversation.handle(clientId, contactId, phoneId,      │ │
│  │                          channel, title)                     │ │
│  │                      ↓                                        │ │
│  │ ConversationRepository.findActiveByContact(...) [TODO]       │ │
│  │                      ↓                                        │ │
│  │ Si existe conversación ACTIVA → reutilizarla                 │ │
│  │ Si NO existe:                                                │ │
│  │   - Crea nueva Conversation                                  │ │
│  │   - Status: ACTIVE                                           │ │
│  │   - StartedAt: ahora                                         │ │
│  │   - Guarda en BD                                            │ │
│  │                                                               │ │
│  │ Conversación resultante:                                     │ │
│  │   id: "a1b2c3d4-..."                                         │ │
│  │   clientId: "550e8400-..."                                   │ │
│  │   contactId: "7f3e4d5c-..."                                  │ │
│  │   status: ACTIVE                                             │ │
│  │   channel: WHATSAPP                                          │ │
│  │   startedAt: "2025-10-03T14:30:00Z"                         │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 7: GUARDAR MENSAJE ENTRANTE                                 │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ Crear Message:                                               │ │
│  │   id: UuidId.newId()                                        │ │
│  │   conversationId: "a1b2c3d4-..."                            │ │
│  │   direction: IN                                              │ │
│  │   content: "¿Cuáles son tus horarios?"                      │ │
│  │   externalId: "wamid.xxx"                                   │ │
│  │   status: PENDING                                            │ │
│  │   createdAt: "2025-10-03T14:30:00Z"                         │ │
│  │                      ↓                                        │ │
│  │ MessageRepository.save(message)                              │ │
│  │                      ↓                                        │ │
│  │         PostgreSQL: chatbotia.message                        │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 8: PROCESAR CON IA (RAG) ⭐                                  │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ ProcessMessageWithAI.handle(message, conversationId, "kb")  │ │
│  │                                                               │ │
│  │ ┌─────────────────────────────────────────────────────────┐ │ │
│  │ │ SUB-PASO 8.1: GENERAR EMBEDDING DE LA PREGUNTA         │ │ │
│  │ │                                                          │ │ │
│  │ │ EmbeddingsPort.embedOne("¿Cuáles son tus horarios?")   │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ HttpEmbeddingsClient → OpenAI API                       │ │ │
│  │ │ POST https://api.openai.com/v1/embeddings               │ │ │
│  │ │ {                                                        │ │ │
│  │ │   model: "text-embedding-3-large",                      │ │ │
│  │ │   input: "¿Cuáles son tus horarios?"                    │ │ │
│  │ │ }                                                        │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ Respuesta: float[3072] (vector de embeddings)           │ │ │
│  │ └─────────────────────────────────────────────────────────┘ │ │
│  │                      ↓                                        │ │
│  │ ┌─────────────────────────────────────────────────────────┐ │ │
│  │ │ SUB-PASO 8.2: BUSCAR EN KNOWLEDGE BASE                  │ │ │
│  │ │                                                          │ │ │
│  │ │ VectorStore.query("kb", embedding, topK=5)             │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ PostgreSQL: chatbotia.kb_embedding_pgvector             │ │ │
│  │ │ SELECT * FROM kb_embedding_pgvector                     │ │ │
│  │ │ ORDER BY embedding <=> '[...]'::vector                  │ │ │
│  │ │ LIMIT 5;                                                │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ Documentos relevantes encontrados:                      │ │ │
│  │ │ 1. "Horarios: Lunes a Viernes 9am-6pm"                 │ │ │
│  │ │ 2. "Sábados 10am-2pm"                                   │ │ │
│  │ │ 3. "Domingos cerrado"                                   │ │ │
│  │ └─────────────────────────────────────────────────────────┘ │ │
│  │                      ↓                                        │ │
│  │ ┌─────────────────────────────────────────────────────────┐ │ │
│  │ │ SUB-PASO 8.3: OBTENER HISTORIAL                        │ │ │
│  │ │                                                          │ │ │
│  │ │ MessageRepository.findByConversation(id, limit=10)     │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ PostgreSQL: chatbotia.message                           │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ Historial de conversación:                              │ │ │
│  │ │ [                                                        │ │ │
│  │ │   { role: "IN", content: "Hola" },                     │ │ │
│  │ │   { role: "OUT", content: "¡Hola! ¿En qué puedo...?" },│ │ │
│  │ │   { role: "IN", content: "¿Cuáles son tus horarios?" } │ │ │
│  │ │ ]                                                        │ │ │
│  │ └─────────────────────────────────────────────────────────┘ │ │
│  │                      ↓                                        │ │
│  │ ┌─────────────────────────────────────────────────────────┐ │ │
│  │ │ SUB-PASO 8.4: GENERAR RESPUESTA CON IA                 │ │ │
│  │ │                                                          │ │ │
│  │ │ AIService.generateResponse(message, context, history)  │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ OpenAIServiceAdapter → OpenAI GPT-4o-mini              │ │ │
│  │ │                                                          │ │ │
│  │ │ Prompt construido:                                      │ │ │
│  │ │ ┌────────────────────────────────────────────────────┐ │ │ │
│  │ │ │ SYSTEM:                                            │ │ │ │
│  │ │ │ "Eres un asistente virtual inteligente..."        │ │ │ │
│  │ │ │                                                    │ │ │ │
│  │ │ │ USER:                                              │ │ │ │
│  │ │ │ Contexto relevante:                                │ │ │ │
│  │ │ │ - Horarios: Lunes a Viernes 9am-6pm               │ │ │ │
│  │ │ │ - Sábados 10am-2pm                                 │ │ │ │
│  │ │ │ - Domingos cerrado                                 │ │ │ │
│  │ │ │                                                    │ │ │ │
│  │ │ │ Historial de conversación:                         │ │ │ │
│  │ │ │ Usuario: Hola                                      │ │ │ │
│  │ │ │ Asistente: ¡Hola! ¿En qué puedo ayudarte?        │ │ │ │
│  │ │ │ Usuario: ¿Cuáles son tus horarios?                │ │ │ │
│  │ │ │                                                    │ │ │ │
│  │ │ │ Pregunta del usuario: ¿Cuáles son tus horarios?   │ │ │ │
│  │ │ └────────────────────────────────────────────────────┘ │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ POST https://api.openai.com/v1/chat/completions        │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ Respuesta de OpenAI:                                    │ │ │
│  │ │ "Nuestros horarios son de lunes a viernes de 9am a    │ │ │
│  │ │  6pm y los sábados de 10am a 2pm. Los domingos        │ │ │
│  │ │  permanecemos cerrados. ¿En qué más puedo ayudarte?"  │ │ │
│  │ └─────────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 9: ENVIAR RESPUESTA                                          │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ SendMessage.handle(clientId, conversationId, contactId,      │ │
│  │                   phoneId, channel, response, from, to)      │ │
│  │                      ↓                                        │ │
│  │ ┌─────────────────────────────────────────────────────────┐ │ │
│  │ │ SUB-PASO 9.1: CREAR MENSAJE DE SALIDA                  │ │ │
│  │ │                                                          │ │ │
│  │ │ Message:                                                │ │ │
│  │ │   id: UuidId.newId()                                   │ │ │
│  │ │   direction: OUT                                        │ │ │
│  │ │   content: "Nuestros horarios son..."                  │ │ │
│  │ │   status: PENDING                                       │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ MessageRepository.save(message)                         │ │ │
│  │ └─────────────────────────────────────────────────────────┘ │ │
│  │                      ↓                                        │ │
│  │ ┌─────────────────────────────────────────────────────────┐ │ │
│  │ │ SUB-PASO 9.2: ENVIAR VÍA WHATSAPP                      │ │ │
│  │ │                                                          │ │ │
│  │ │ WhatsAppService.sendMessage(from, to, message)         │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ MetaWhatsAppAdapter → Meta WhatsApp API                │ │ │
│  │ │                                                          │ │ │
│  │ │ POST https://graph.facebook.com/v21.0/               │ │ │
│  │ │      {phoneNumberId}/messages                           │ │ │
│  │ │ {                                                        │ │ │
│  │ │   messaging_product: "whatsapp",                        │ │ │
│  │ │   to: "593999999999",                                   │ │ │
│  │ │   type: "text",                                         │ │ │
│  │ │   text: {                                               │ │ │
│  │ │     body: "Nuestros horarios son..."                   │ │ │
│  │ │   }                                                      │ │ │
│  │ │ }                                                        │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ Respuesta de Meta:                                      │ │ │
│  │ │ {                                                        │ │ │
│  │ │   messages: [{                                          │ │ │
│  │ │     id: "wamid.HBgNNTkzOTk5OTk5OTk5FQIAERgSMkE..."    │ │ │
│  │ │   }]                                                     │ │ │
│  │ │ }                                                        │ │ │
│  │ └─────────────────────────────────────────────────────────┘ │ │
│  │                      ↓                                        │ │
│  │ ┌─────────────────────────────────────────────────────────┐ │ │
│  │ │ SUB-PASO 9.3: ACTUALIZAR MENSAJE                       │ │ │
│  │ │                                                          │ │ │
│  │ │ message.markSent(now, externalId)                       │ │ │
│  │ │ MessageRepository.save(message)                         │ │ │
│  │ │           ↓                                              │ │ │
│  │ │ Message actualizado:                                    │ │ │
│  │ │   status: SENT                                          │ │ │
│  │ │   sentAt: "2025-10-03T14:30:02Z"                       │ │ │
│  │ │   externalId: "wamid.HBgN..."                          │ │ │
│  │ └─────────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  PASO 10: RESPONDER AL WEBHOOK                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ MessageResponse:                                             │ │
│  │ {                                                             │ │
│  │   success: true,                                             │ │
│  │   conversationId: "a1b2c3d4-...",                           │ │
│  │   messageId: "e5f6g7h8-...",                                │ │
│  │   response: "Nuestros horarios son..."                      │ │
│  │ }                                                             │ │
│  │           ↓                                                   │ │
│  │ HTTP 200 OK                                                  │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                  META WHATSAPP BUSINESS API                         │
│              Envía mensaje al usuario de WhatsApp                   │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    USUARIO DE WHATSAPP                              │
│          Recibe: "Nuestros horarios son de lunes a viernes         │
│                   de 9am a 6pm y los sábados de 10am a 2pm..."    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Componentes y sus Responsabilidades

### Capa 1: Interfaces (Web)
```
MetaWhatsAppWebhookController
├─ Responsabilidad: Recibir webhooks de Meta WhatsApp
├─ Entrada: POST /webhooks/whatsapp/meta (JSON de Meta)
├─ Salida: MessageCommand
└─ Tecnología: Spring Web, @RestController
```

### Capa 2: Application (Casos de Uso)
```
ReceiveWhatsAppMessage (ORQUESTADOR)
├─ Responsabilidad: Coordinar todo el flujo
├─ Entrada: MessageCommand
├─ Salida: MessageResponse
├─ Dependencias:
│  ├─ ClientRepository (buscar cliente)
│  ├─ GetOrCreateContact (gestión de contactos)
│  ├─ StartConversation (gestión de conversaciones)
│  ├─ MessageRepository (persistencia de mensajes)
│  ├─ ProcessMessageWithAI (IA + RAG)
│  └─ SendMessage (envío de respuestas)
└─ Transaccional: @Transactional

ProcessMessageWithAI (RAG)
├─ Responsabilidad: Generar respuesta inteligente
├─ Entrada: mensaje del usuario, conversationId, namespace
├─ Salida: String (respuesta generada)
├─ Flujo RAG:
│  ├─ 1. EmbeddingsPort → generar embedding de la pregunta
│  ├─ 2. VectorStore → buscar contexto relevante
│  ├─ 3. MessageRepository → obtener historial
│  └─ 4. AIService → generar respuesta con contexto
└─ Tecnología: OpenAI, pgvector

GetOrCreateContact
├─ Responsabilidad: Gestión de contactos
├─ Entrada: clientId, phone, name, channel
├─ Salida: Contact (nuevo o existente)
├─ Lógica:
│  ├─ Buscar en ContactRepository
│  ├─ Si existe → retornar
│  └─ Si NO existe → crear y guardar
└─ Transaccional: @Transactional

StartConversation
├─ Responsabilidad: Gestión de conversaciones
├─ Entrada: clientId, contactId, phoneId, channel, title
├─ Salida: Conversation
├─ Lógica:
│  ├─ Buscar conversación ACTIVA
│  ├─ Si existe → retornar
│  └─ Si NO existe → crear nueva
└─ Transaccional: @Transactional

SendMessage
├─ Responsabilidad: Enviar mensajes por WhatsApp
├─ Entrada: clientId, conversationId, contactId, channel, message, from, to
├─ Salida: Message (mensaje enviado)
├─ Flujo:
│  ├─ 1. Crear Message (direction=OUT, status=PENDING)
│  ├─ 2. Guardar en MessageRepository
│  ├─ 3. WhatsAppService.sendMessage() → Meta API
│  ├─ 4. message.markSent(externalId)
│  └─ 5. Actualizar en MessageRepository
└─ Transaccional: @Transactional
```

### Capa 3: Domain (Dominio)
```
Entidades Principales:

Client
├─ id: UuidId<Client>
├─ code: String (único)
├─ name: String
├─ email: Email
├─ status: EntityStatus
└─ Relaciones: tiene muchos Conversations, Messages, Contacts

Contact
├─ id: UuidId<Contact>
├─ clientId: UuidId<Client>
├─ fullName: String
├─ email: Email (opcional)
├─ phoneE164: String
└─ status: EntityStatus

Conversation
├─ id: UuidId<Conversation>
├─ clientId: UuidId<Client>
├─ contactId: UuidId<Contact>
├─ channel: Channel (WHATSAPP)
├─ status: ConversationStatus (ACTIVE, CLOSED)
├─ startedAt: Instant
└─ closedAt: Optional<Instant>

Message
├─ id: UuidId<Message>
├─ conversationId: UuidId<Conversation>
├─ contactId: UuidId<Contact>
├─ direction: Direction (IN, OUT)
├─ content: String
├─ status: MessageStatus (PENDING, SENT, DELIVERED, READ)
├─ createdAt: Instant
├─ sentAt: Optional<Instant>
├─ externalId: Optional<String>
└─ Métodos:
   ├─ markSent(timestamp, externalId)
   ├─ markDelivered(timestamp)
   ├─ markRead(timestamp)
   └─ fail(error)
```

### Capa 4: Infrastructure (Adaptadores)
```
Adaptadores de Persistencia:

ConversationRepositoryAdapter
├─ Implementa: ConversationRepository
├─ Usa: ConversationJpa (Spring Data)
├─ Mapper: ConversationMapper (Domain ↔ Entity)
└─ BD: PostgreSQL chatbotia.conversation

MessageRepositoryAdapter
├─ Implementa: MessageRepository
├─ Usa: MessageJpa (Spring Data)
├─ Mapper: MessageMapper (Domain ↔ Entity)
└─ BD: PostgreSQL chatbotia.message

ContactRepositoryAdapter
├─ Implementa: ContactRepository
├─ Usa: ContactJpa (Spring Data)
├─ Mapper: ContactMapper (Domain ↔ Entity)
└─ BD: PostgreSQL chatbotia.contact

Adaptadores Externos:

OpenAIServiceAdapter
├─ Implementa: AIService
├─ API: OpenAI Chat Completions
├─ Modelo: gpt-4o-mini
├─ Función: Generar respuestas con RAG
└─ Configuración: application.yml

HttpEmbeddingsClient
├─ Implementa: EmbeddingsPort
├─ API: OpenAI Embeddings
├─ Modelo: text-embedding-3-large
├─ Dimensiones: 3072
└─ Función: Convertir texto a vectores

MetaWhatsAppAdapter
├─ Implementa: WhatsAppService
├─ API: Meta Graph API v21.0
├─ Función: Enviar mensajes de WhatsApp
└─ Configuración: ACCESS_TOKEN, PHONE_NUMBER_ID

SimplePgVectorStoreAdapter
├─ Implementa: VectorStore
├─ BD: PostgreSQL con extensión pgvector
├─ Función: Búsqueda por similitud vectorial
└─ Tabla: chatbotia.kb_embedding_pgvector
```

---

## 🗄️ Flujo de Datos en Base de Datos

```sql
-- 1. Cliente registrado
SELECT * FROM chatbotia.client WHERE code = 'default-client';
-- Resultado: id, code, name, email, status

-- 2. Buscar o crear contacto
SELECT * FROM chatbotia.contact 
WHERE client_id = ? AND phone_e164 = '593999999999';
-- Si no existe: INSERT INTO chatbotia.contact (...)

-- 3. Crear conversación
INSERT INTO chatbotia.conversation (
  id, client_id, contact_id, channel, status, started_at
) VALUES (?, ?, ?, 'WHATSAPP', 'ACTIVE', NOW());

-- 4. Guardar mensaje entrante
INSERT INTO chatbotia.message (
  id, conversation_id, contact_id, channel, direction,
  content, status, created_at
) VALUES (?, ?, ?, 'WHATSAPP', 'IN', '¿Cuáles son tus horarios?', 'PENDING', NOW());

-- 5. Buscar en Knowledge Base (pgvector)
SELECT id, chunk_text, metadata
FROM chatbotia.kb_embedding_pgvector
ORDER BY embedding <=> '[0.123, 0.456, ...]'::vector
LIMIT 5;

-- 6. Obtener historial de conversación
SELECT id, direction, content, created_at
FROM chatbotia.message
WHERE conversation_id = ?
ORDER BY created_at DESC
LIMIT 10;

-- 7. Guardar mensaje de respuesta
INSERT INTO chatbotia.message (
  id, conversation_id, direction, content, status, created_at
) VALUES (?, ?, 'OUT', 'Nuestros horarios son...', 'PENDING', NOW());

-- 8. Actualizar mensaje después de enviarlo
UPDATE chatbotia.message
SET status = 'SENT', sent_at = NOW(), external_id = 'wamid.xxx'
WHERE id = ?;
```

---

## ⚙️ Configuración del Sistema

```yaml
# application-prod.yml

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chatbotia
    username: postgres
    password: ${DATABASE_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway maneja migraciones
    properties:
      hibernate:
        default_schema: chatbotia

app:
  # WhatsApp Provider
  whatsapp:
    provider: meta  # meta, twilio, o mock
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
      webhook-verify-token: ${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN}
  
  # IA Configuration
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini
      embedding-model: text-embedding-3-large
  
  # Knowledge Base
  knowledge-base:
    default-namespace: kb
    top-k-results: 5
    max-conversation-history: 10
```

---

## 🔄 Casos de Uso Alternativos

### Caso 1: Error en la IA
```
Usuario envía mensaje
    ↓
[Pasos 1-7 normales]
    ↓
ProcessMessageWithAI
    ↓
OpenAI API → ERROR 500
    ↓
Catch exception
    ↓
Retornar respuesta por defecto:
"Lo siento, ocurrió un error al procesar tu mensaje. ¿Puedes intentar de nuevo?"
    ↓
SendMessage (envía mensaje de error)
    ↓
Usuario recibe disculpa
```

### Caso 2: Cliente no encontrado
```
Webhook recibido
    ↓
MessageCommand creado
    ↓
ReceiveWhatsAppMessage
    ↓
ClientRepository.findByCode() → NULL
    ↓
return MessageResponse.error("Cliente no encontrado")
    ↓
HTTP 200 OK (pero sin procesar mensaje)
```

### Caso 3: Sin contexto en Knowledge Base
```
ProcessMessageWithAI
    ↓
SearchRelevantContext → [] (vacío)
    ↓
AIService.generateResponse(
  message,
  [], // sin contexto
  history
)
    ↓
IA genera respuesta basada solo en el historial
    ↓
Respuesta genérica enviada
```

---

## 📈 Métricas y Monitoreo

```
Puntos de logging:

1. MetaWhatsAppWebhookController
   - INFO: "Webhook recibido"
   - DEBUG: Payload completo

2. ReceiveWhatsAppMessage
   - INFO: "Recibiendo mensaje de WhatsApp"
   - INFO: "Mensaje entrante guardado"
   - INFO: "Respuesta enviada"
   - ERROR: Cualquier excepción

3. ProcessMessageWithAI
   - INFO: "Respuesta generada"
   - WARN: "Error al buscar contexto"
   - ERROR: Excepciones de IA

4. SendMessage
   - INFO: "Enviando mensaje"
   - INFO: "Mensaje enviado exitosamente"
   - ERROR: Errores de WhatsApp API
```

---

## ✅ Resumen del Flujo

| Paso | Componente | Acción | Tiempo Aprox |
|------|------------|--------|--------------|
| 1 | Webhook | Recibir de Meta | < 10ms |
| 2 | Controller | Parsear payload | < 5ms |
| 3 | Use Case | Orquestar flujo | - |
| 4 | Repository | Buscar cliente | ~10ms |
| 5 | Use Case | Crear/buscar contacto | ~20ms |
| 6 | Use Case | Crear/buscar conversación | ~20ms |
| 7 | Repository | Guardar mensaje IN | ~15ms |
| 8a | Embeddings | Generar embedding | ~200ms |
| 8b | VectorStore | Buscar en KB | ~50ms |
| 8c | Repository | Obtener historial | ~20ms |
| 8d | OpenAI | Generar respuesta | ~1-3s |
| 9 | WhatsApp API | Enviar mensaje | ~300ms |
| 10 | Repository | Actualizar mensaje | ~10ms |
| **TOTAL** | | **End-to-End** | **~2-4 segundos** |

---

**¡Este es el flujo completo de datos de tu chatbot WhatsApp con IA! 🚀**

