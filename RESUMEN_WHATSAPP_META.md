# ✅ Integración Meta WhatsApp Business API - Completa

## 🎉 ¡Todo Implementado!

He completado la integración completa con **Meta WhatsApp Business API**.

---

## 📦 Componentes Implementados

### 1. **MetaWhatsAppAdapter** ✅
- **Archivo**: `infrastructure/adapters/out/whatsapp/MetaWhatsAppAdapter.java`
- **Función**: Enviar mensajes y plantillas a través de WhatsApp
- **API**: Graph API de Meta v21.0

### 2. **MetaWhatsAppWebhookController** ✅
- **Archivo**: `infrastructure/adapters/in/web/MetaWhatsAppWebhookController.java`
- **Endpoints**:
  - `GET /webhooks/whatsapp/meta` - Verificación de webhook
  - `POST /webhooks/whatsapp/meta` - Recepción de mensajes
- **Función**: Recibir y procesar mensajes entrantes

### 3. **Configuración** ✅
- **application-dev.yml**: Modo desarrollo con mock
- **application-prod.yml**: Modo producción con Meta API

---

## 🔧 Configuración Rápida

### Variables de Entorno

```bash
# Meta WhatsApp Business API
META_WHATSAPP_ACCESS_TOKEN=EAAxxxxxxxxxxxxxxx
META_WHATSAPP_PHONE_NUMBER_ID=123456789012345
META_WHATSAPP_WEBHOOK_VERIFY_TOKEN=mi_token_secreto_aqui
META_WHATSAPP_API_VERSION=v21.0  # Opcional
```

### application.yml

```yaml
app:
  whatsapp:
    provider: meta  # Cambiar de 'mock' a 'meta' para activar
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
      webhook-verify-token: ${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN}
      api-version: v21.0
```

---

## 🚀 Cómo Usar

### 1. Obtener Credenciales de Meta

1. Ve a https://developers.facebook.com/
2. **Create App** > Tipo: **Business**
3. Añade producto **WhatsApp**
4. En **API Setup**:
   - Copia el **Access Token** temporal (o crea uno permanente)
   - Copia el **Phone Number ID**

### 2. Configurar Webhook

**URL del Webhook**:
```
https://tu-dominio.com/webhooks/whatsapp/meta
```

**Verify Token**:
```
mi_token_secreto_aqui
```
(El mismo que configuraste en la variable de entorno)

**Suscripciones**:
- ✅ `messages`

### 3. Cambiar Provider en Configuración

```yaml
app:
  whatsapp:
    provider: meta  # Cambiar de 'mock' a 'meta'
```

### 4. Ejecutar la Aplicación

```bash
./mvnw spring-boot:run
```

---

## 📱 Funcionalidades

### Enviar Mensajes

```java
@Autowired
private WhatsAppService whatsAppService;

// Enviar mensaje simple
String messageId = whatsAppService.sendMessage(
    null,  // from (no se usa en Meta API)
    "593999999999",  // to (número del destinatario)
    "¡Hola! Este es un mensaje del chatbot."
);
```

### Enviar Plantillas

```java
// Enviar plantilla con parámetros
String messageId = whatsAppService.sendTemplate(
    null,
    "593999999999",
    "bienvenida_bot",  // nombre de la plantilla aprobada
    Map.of("1", "Juan", "2", "Premium")  // parámetros {{1}}, {{2}}
);
```

### Recibir Mensajes

El webhook automáticamente:
1. ✅ Recibe el mensaje de Meta
2. ✅ Extrae el contenido y datos del usuario
3. ✅ Llama a `ReceiveWhatsAppMessage`
4. ✅ Procesa con IA
5. ✅ Envía respuesta automática

---

## 🔄 Flujo Completo End-to-End

```
Usuario WhatsApp
    ↓ [Envía mensaje]
Meta WhatsApp API
    ↓ [POST /webhooks/whatsapp/meta]
MetaWhatsAppWebhookController
    ↓ [Crea MessageCommand]
ReceiveWhatsAppMessage
    ├─ GetOrCreateContact
    ├─ StartConversation
    ├─ Guardar mensaje
    ├─ ProcessMessageWithAI
    │   ├─ Buscar en Knowledge Base
    │   └─ Generar respuesta con OpenAI
    └─ SendMessage
        ↓ [Llama a WhatsAppService]
MetaWhatsAppAdapter
    ↓ [POST /messages]
Meta WhatsApp API
    ↓ [Envía mensaje]
Usuario WhatsApp
```

---

## 🎯 Providers Disponibles

### 1. **Meta (Recomendado para Producción)** ✅
```yaml
provider: meta
```
- ✅ Costo más bajo
- ✅ API directa de Meta
- ✅ Más control y flexibilidad

### 2. **Twilio (Alternativa)** ✅
```yaml
provider: twilio
```
- ✅ Fácil de configurar
- ✅ Abstracción de Twilio
- 💰 Costo más alto

### 3. **Mock (Desarrollo)** ✅
```yaml
provider: mock
```
- ✅ Sin credenciales
- ✅ Logs en consola
- ✅ Desarrollo rápido

---

## 📊 Comparación de Implementaciones

| Característica | Meta | Twilio | Mock |
|----------------|------|--------|------|
| **Costo** | 💰 Bajo | 💰💰 Alto | 🆓 Gratis |
| **Setup** | ⚙️ Medio | ⚙️ Fácil | ⚙️ Inmediato |
| **Webhook** | Manual | Automático | No requiere |
| **Plantillas** | Meta Manager | Twilio Console | Simuladas |
| **Producción** | ✅ Sí | ✅ Sí | ❌ No |

---

## 🐛 Troubleshooting

### Webhook no recibe mensajes

```bash
# Verificar endpoint público
curl https://tu-dominio.com/webhooks/whatsapp/meta

# Revisar logs
logging:
  level:
    com.relative.chat.bot.ia.infrastructure.adapters.in.web: DEBUG
```

### Error 401 al enviar

- **Causa**: Access Token inválido
- **Solución**: Regenerar token en Meta Developer Console

### Número inválido

- **Causa**: Formato incorrecto
- **Solución**: Usar formato E.164 sin espacios: `593999999999`

---

## 📚 Documentación

- [Meta WhatsApp Cloud API](https://developers.facebook.com/docs/whatsapp/cloud-api)
- [Getting Started](https://developers.facebook.com/docs/whatsapp/cloud-api/get-started)
- [Webhooks](https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks)
- [Templates](https://developers.facebook.com/docs/whatsapp/message-templates)

---

## ✅ Estado del Proyecto

| Componente | Estado | Proveedor |
|------------|--------|-----------|
| **Envío de mensajes** | ✅ | Meta, Twilio, Mock |
| **Recepción de mensajes** | ✅ | Meta, Mock |
| **Plantillas** | ✅ | Meta, Twilio |
| **IA (OpenAI)** | ✅ | OpenAI GPT-4o-mini |
| **Knowledge Base** | ✅ | pgvector + embeddings |
| **Base de Datos** | ✅ | PostgreSQL |
| **Webhooks** | ✅ | Meta WhatsApp |

---

## 🎉 ¡Listo para Producción!

El chatbot está completamente funcional con:

1. ✅ Recepción de mensajes de WhatsApp (Meta API)
2. ✅ Procesamiento con IA (OpenAI + RAG)
3. ✅ Búsqueda en Knowledge Base
4. ✅ Envío de respuestas automáticas
5. ✅ Gestión de conversaciones y contactos
6. ✅ Multi-proveedor (Meta, Twilio, Mock)
7. ✅ Configuración flexible por entorno

**¡Solo configura las credenciales y despliega! 🚀**

