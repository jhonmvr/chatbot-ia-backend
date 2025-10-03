# Integración con WhatsApp Business API (Meta)

## 🚀 Implementación Completa

He implementado la integración con **Meta WhatsApp Business API** (antes Facebook WhatsApp API).

---

## 📦 Componentes Creados

### 1. **MetaWhatsAppAdapter** ✅
**Ubicación**: `infrastructure/adapters/out/whatsapp/MetaWhatsAppAdapter.java`

**Características**:
- ✅ Envío de mensajes de texto
- ✅ Envío de plantillas (templates)
- ✅ Limpieza automática de números de teléfono
- ✅ Manejo robusto de errores
- ✅ Integración con Graph API de Meta
- ✅ Soporte para API v21.0 (configurable)

**Métodos**:
```java
// Enviar mensaje de texto
String sendMessage(String from, String to, String message);

// Enviar plantilla
String sendTemplate(String from, String to, String templateId, Map<String, String> parameters);
```

### 2. **MetaWhatsAppWebhookController** ✅
**Ubicación**: `infrastructure/adapters/in/web/MetaWhatsAppWebhookController.java`

**Características**:
- ✅ Verificación de webhook (GET)
- ✅ Recepción de mensajes (POST)
- ✅ Procesamiento de mensajes de texto
- ✅ Soporte para estructura completa de Meta
- ✅ Manejo de errores sin reintentos

**Endpoints**:
```
GET  /webhooks/whatsapp/meta  - Verificación
POST /webhooks/whatsapp/meta  - Recepción de mensajes
```

---

## 🔧 Configuración

### Variables de Entorno Necesarias

```bash
# Access Token de Meta (Obligatorio)
META_WHATSAPP_ACCESS_TOKEN=EAAxxxxxxxxxxxxxxx

# Phone Number ID (Obligatorio)
# Lo obtienes en Meta Business Manager > WhatsApp > API Setup
META_WHATSAPP_PHONE_NUMBER_ID=123456789012345

# Webhook Verify Token (Obligatorio)
# Token que configuras tú mismo para validar el webhook
META_WHATSAPP_WEBHOOK_VERIFY_TOKEN=tu_token_secreto_aqui

# Versión de la API (Opcional, por defecto v21.0)
META_WHATSAPP_API_VERSION=v21.0
```

### application-dev.yml
```yaml
app:
  whatsapp:
    provider: mock  # Cambiar a 'meta' para usar WhatsApp real
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN:}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID:}
      webhook-verify-token: ${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN:mi_token_secreto}
      api-version: v21.0
```

### application-prod.yml
```yaml
app:
  whatsapp:
    provider: meta
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
      webhook-verify-token: ${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN}
      api-version: ${META_WHATSAPP_API_VERSION:v21.0}
```

---

## 📋 Guía de Configuración en Meta

### Paso 1: Crear App de WhatsApp Business

1. Ve a https://developers.facebook.com/
2. **My Apps** > **Create App**
3. Selecciona **Business** como tipo
4. Añade **WhatsApp** como producto

### Paso 2: Obtener Credenciales

#### Access Token
1. En el panel de WhatsApp
2. **API Setup** > **Temporary access token** (desarrollo)
3. O crea un **System User** con token permanente (producción)

```bash
# Guardar en .env
META_WHATSAPP_ACCESS_TOKEN=EAAxxxxxxxxxxxxxxx
```

#### Phone Number ID
1. En **API Setup**
2. Copia el **Phone number ID** (no es el número de teléfono)

```bash
META_WHATSAPP_PHONE_NUMBER_ID=123456789012345
```

### Paso 3: Configurar Webhook

1. En el panel de WhatsApp > **Configuration**
2. **Webhook** > **Edit**

**Callback URL**:
```
https://tu-dominio.com/webhooks/whatsapp/meta
```

**Verify Token**:
```
tu_token_secreto_aqui
```
(El mismo que configuraste en `META_WHATSAPP_WEBHOOK_VERIFY_TOKEN`)

3. **Suscribirse a eventos**:
   - ✅ `messages` (obligatorio)
   - ✅ `message_status` (opcional, para delivery/read receipts)

4. Hacer clic en **Verify and Save**

---

## 🧪 Testing

### 1. Verificar Webhook (Local con ngrok)

```bash
# Instalar ngrok
choco install ngrok  # Windows
# o
brew install ngrok   # Mac

# Exponer puerto local
ngrok http 8080

# URL generada:
https://abc123.ngrok.io

# Configurar en Meta:
https://abc123.ngrok.io/webhooks/whatsapp/meta
```

### 2. Probar Verificación

Meta hará esta llamada automáticamente:
```bash
GET https://tu-dominio.com/webhooks/whatsapp/meta?
    hub.mode=subscribe&
    hub.verify_token=tu_token_secreto_aqui&
    hub.challenge=CHALLENGE_STRING
```

Debe responder con el `challenge` si el token es correcto.

### 3. Enviar Mensaje de Prueba

Desde tu número personal registrado en Meta, envía un mensaje al número de WhatsApp Business.

**Logs esperados**:
```
Webhook recibido: {...}
Mensaje recibido - ID: wamid.xxx, From: 593999999999, Type: text
Mensaje procesado exitosamente: wamid.xxx
```

### 4. Enviar Mensaje Programáticamente

```java
@Autowired
private WhatsAppService whatsAppService;

// Enviar mensaje simple
String messageId = whatsAppService.sendMessage(
    null,  // from no se usa en Meta (usa phoneNumberId)
    "593999999999",  // to
    "¡Hola! Este es un mensaje de prueba."
);

// Enviar plantilla
String templateMessageId = whatsAppService.sendTemplate(
    null,
    "593999999999",
    "hello_world",  // nombre de la plantilla
    Map.of("1", "Juan")  // parámetros
);
```

---

## 📊 Formato de Mensajes

### Mensaje Simple
```json
{
  "messaging_product": "whatsapp",
  "recipient_type": "individual",
  "to": "593999999999",
  "type": "text",
  "text": {
    "preview_url": false,
    "body": "Tu mensaje aquí"
  }
}
```

### Plantilla
```json
{
  "messaging_product": "whatsapp",
  "to": "593999999999",
  "type": "template",
  "template": {
    "name": "hello_world",
    "language": {
      "code": "es"
    },
    "components": [
      {
        "type": "body",
        "parameters": [
          {
            "type": "text",
            "text": "Juan"
          }
        ]
      }
    ]
  }
}
```

---

## 🎯 Plantillas (Templates)

### Crear Plantilla en Meta

1. **Business Manager** > **WhatsApp Manager**
2. **Message Templates** > **Create Template**
3. Crear plantilla con parámetros:

```
Nombre: bienvenida_bot
Categoría: UTILITY
Idioma: Spanish

Contenido:
Hola {{1}}, bienvenido a nuestro servicio de atención.
¿En qué podemos ayudarte hoy?
```

4. Esperar aprobación (24-48 horas)

### Usar Plantilla

```java
whatsAppService.sendTemplate(
    null,
    "593999999999",
    "bienvenida_bot",
    Map.of("1", "Juan")  // Reemplaza {{1}} con "Juan"
);
```

---

## 🔒 Seguridad

### Validación de Webhook

El webhook valida:
1. ✅ Token de verificación (GET)
2. ✅ Estructura del payload (POST)
3. ✅ Tipo de objeto (`whatsapp_business_account`)

### Recomendaciones

1. **HTTPS obligatorio** en producción
2. **Webhook Verify Token** fuerte y secreto
3. **Access Token** permanente con System User
4. **Rate Limiting** en endpoints públicos
5. **IP Whitelist** si es posible

---

## 📈 Límites de la API

### Modo Sandbox (Desarrollo)
- ✅ Hasta 5 números de prueba
- ✅ Plantillas predefinidas
- ✅ 1000 conversaciones gratuitas/mes

### Modo Producción
- 📊 Tier-based pricing
- 📊 Basado en conversaciones iniciadas
- 📊 Primeras 1000 conversaciones/mes gratis

**Documentación de precios**: https://developers.facebook.com/docs/whatsapp/pricing

---

## 🆚 Comparación de Proveedores

| Característica | Meta (Business API) | Twilio |
|----------------|---------------------|--------|
| **Costo** | 💰 Bajo | 💰💰 Medio-Alto |
| **Integración** | Directa con Meta | Abstracción de Twilio |
| **Plantillas** | Aprobación de Meta | Aprobación vía Twilio |
| **Webhook** | Configuración manual | Configurado por Twilio |
| **Complejidad** | Media | Baja |
| **Recomendación** | ✅ **Producción** | 🧪 Prototipado rápido |

---

## 🐛 Troubleshooting

### El webhook no recibe mensajes

1. **Verificar URL pública**:
   ```bash
   curl https://tu-dominio.com/webhooks/whatsapp/meta
   ```

2. **Revisar suscripciones**:
   - Meta Business Manager > WhatsApp > Configuration
   - Verificar que `messages` esté suscrito

3. **Logs del servidor**:
   ```bash
   logging:
     level:
       com.relative.chat.bot.ia.infrastructure.adapters.in.web: DEBUG
   ```

### Error 401 al enviar mensajes

- **Causa**: Access Token inválido o expirado
- **Solución**: Regenerar token en Meta Developer Console

### Error 400: Invalid phone number

- **Causa**: Número no en formato E.164
- **Solución**: El adapter limpia automáticamente, pero verifica que tenga código de país

### Plantilla no enviada

- **Causa**: Plantilla no aprobada o nombre incorrecto
- **Solución**: Verificar estado en Message Templates

---

## ✅ Checklist de Producción

- [ ] Access Token permanente (System User)
- [ ] Phone Number ID configurado
- [ ] Webhook con HTTPS válido
- [ ] Webhook Verify Token secreto y seguro
- [ ] Suscripciones de webhook activas
- [ ] Plantillas aprobadas por Meta
- [ ] Número de WhatsApp verificado
- [ ] Business Verification completada
- [ ] Logs y monitoring configurados
- [ ] Rate limiting implementado

---

## 📚 Recursos

- [Documentación oficial de Meta](https://developers.facebook.com/docs/whatsapp/cloud-api)
- [Getting Started Guide](https://developers.facebook.com/docs/whatsapp/cloud-api/get-started)
- [Webhooks Reference](https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks)
- [Message Templates](https://developers.facebook.com/docs/whatsapp/message-templates)
- [API Reference](https://developers.facebook.com/docs/whatsapp/cloud-api/reference)

---

## 🎉 ¡Listo para Producción!

El sistema está completamente configurado para usar **Meta WhatsApp Business API**. Solo necesitas:

1. ✅ Obtener credenciales de Meta
2. ✅ Configurar variables de entorno
3. ✅ Configurar webhook público
4. ✅ Cambiar `app.whatsapp.provider` a `meta`

**¡Tu chatbot está listo para escalar! 🚀**

