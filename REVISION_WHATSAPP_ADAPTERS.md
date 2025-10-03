# 🔍 Revisión de WhatsApp Adapters - Condiciones

## ✅ Estado Actual de las Condiciones

### 1. **MetaWhatsAppAdapter** ⚠️ CORREGIDO

**Antes:**
```java
@ConditionalOnProperty(
    name = "app.whatsapp.provider", 
    havingValue = "meta", 
    matchIfMissing = true  // ❌ Se activaba por defecto
)
```

**Después:**
```java
@ConditionalOnProperty(
    name = "app.whatsapp.provider", 
    havingValue = "meta", 
    matchIfMissing = false  // ✅ Solo se activa si está configurado
)
```

---

### 2. **TwilioWhatsAppAdapter** ✅ CORRECTO

```java
@ConditionalOnProperty(
    name = "app.whatsapp.provider", 
    havingValue = "twilio",
    matchIfMissing = false  // ✅ Correcto
)
```

---

### 3. **MockWhatsAppAdapter** ✅ CORRECTO

```java
@ConditionalOnProperty(
    name = "app.whatsapp.provider", 
    havingValue = "mock",
    matchIfMissing = false  // ✅ Correcto (implícito si no se especifica)
)
```

---

## 🎯 Cómo Funciona Ahora

### Configuración determina el adapter activo:

```yaml
app:
  whatsapp:
    provider: meta  # meta | twilio | mock
```

| Valor | Adapter Activo | Uso |
|-------|----------------|-----|
| `meta` | MetaWhatsAppAdapter | Producción (WhatsApp Business API) |
| `twilio` | TwilioWhatsAppAdapter | Alternativa con Twilio |
| `mock` | MockWhatsAppAdapter | Desarrollo/Testing |

---

## 📊 Comparación de Adapters

### MetaWhatsAppAdapter (Facebook/Meta)

**Ventajas:**
- ✅ API oficial de WhatsApp Business
- ✅ Sin costo de intermediario
- ✅ Mejor integración con funciones avanzadas
- ✅ Soporta plantillas aprobadas
- ✅ Webhooks nativos

**Configuración requerida:**
```yaml
app:
  whatsapp:
    provider: meta
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
      webhook-verify-token: ${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN}
      api-version: v21.0
```

---

### TwilioWhatsAppAdapter

**Ventajas:**
- ✅ Fácil configuración inicial
- ✅ Buen soporte técnico
- ✅ Integración con otros canales (SMS, Voice)
- ✅ Sandbox para testing

**Desventajas:**
- ⚠️ Costo adicional por mensaje
- ⚠️ Intermediario entre tú y WhatsApp

**Configuración requerida:**
```yaml
app:
  whatsapp:
    provider: twilio
    twilio:
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      whatsapp-from: whatsapp:+14155238886
```

---

### MockWhatsAppAdapter

**Ventajas:**
- ✅ Sin credenciales necesarias
- ✅ Perfecto para desarrollo local
- ✅ Logs detallados de mensajes simulados
- ✅ Sin costo

**Uso:**
```yaml
app:
  whatsapp:
    provider: mock
```

**Output ejemplo:**
```
📱 [MOCK] Mensaje enviado:
   De: +593987654321
   A: +593998765432
   Mensaje: Hola, ¿cómo estás?
   ID: mock_a3b4c5d6
```

---

## 🔄 Flujo de Selección

```
┌──────────────────────────────┐
│ Spring Boot lee config       │
│ app.whatsapp.provider: "meta"│
└──────────┬───────────────────┘
           ↓
┌──────────────────────────────┐
│ Evalúa @ConditionalOnProperty│
└──────────┬───────────────────┘
           ↓
    ┌──────┴──────┬──────────┐
    ↓             ↓          ↓
┌─────────┐  ┌─────────┐  ┌─────────┐
│  Meta   │  │ Twilio  │  │  Mock   │
│provider │  │provider │  │provider │
│=="meta" │  │=="twilio"│  │=="mock" │
│✅ ACTIVO │  │❌INACTIVO│  │❌INACTIVO│
└─────────┘  └─────────┘  └─────────┘
```

---

## 🧪 Verificar Adapter Activo

### Opción 1: Logs de Inicio

**Meta activo:**
```
INFO : MetaWhatsAppAdapter inicializado con Phone Number ID: 123456789
```

**Twilio activo:**
```
INFO : TwilioWhatsAppAdapter inicializado con Account SID: ACxxxx...
```

**Mock activo:**
```
INFO : Creating bean 'mockWhatsAppAdapter'
```

### Opción 2: Logs al Enviar Mensaje

**Meta:**
```
INFO : Enviando mensaje a 593999999999: Hola...
INFO : Mensaje enviado exitosamente. ID: wamid.HBgLN...
```

**Twilio:**
```
INFO : Enviando mensaje de whatsapp:+593... a whatsapp:+593...: Hola...
INFO : Mensaje enviado exitosamente. SID: SMxxxx...
```

**Mock:**
```
📱 [MOCK] Mensaje enviado:
   De: +593987654321
   A: +593998765432
   Mensaje: Hola, ¿cómo estás?
   ID: mock_a3b4c5d6
```

---

## ⚙️ Configuración por Ambiente

### Desarrollo (`application-dev.yml`)
```yaml
app:
  whatsapp:
    provider: mock  # Sin credenciales necesarias
```

### Staging (`application-staging.yml`)
```yaml
app:
  whatsapp:
    provider: meta  # O twilio para testing con credenciales reales
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
      webhook-verify-token: test-token-staging
```

### Producción (`application-prod.yml`)
```yaml
app:
  whatsapp:
    provider: meta  # Recomendado
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
      webhook-verify-token: ${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN}
      api-version: v21.0
```

---

## 🛡️ Validación de Credenciales

Todos los adapters validan credenciales al iniciar:

### Meta
```java
if (accessToken != null && phoneNumberId != null) {
    // Inicializa WebClient
} else {
    log.warn("Credenciales de Meta WhatsApp no configuradas");
    this.metaClient = null;
}
```

### Twilio
```java
if (accountSid != null && authToken != null) {
    // Inicializa WebClient
} else {
    log.warn("Credenciales de Twilio no configuradas");
    this.twilioClient = null;
}
```

**Si intentas enviar un mensaje sin credenciales:**
```
ERROR: No se puede enviar mensaje: Meta WhatsApp no está configurado
IllegalStateException: Meta WhatsApp no está configurado
```

---

## 🔧 Cambiar de Adapter

### Sin Reiniciar (usando Spring Cloud Config)

```bash
# Actualizar configuración
curl -X POST http://config-server/refresh

# O usando actuator/refresh
curl -X POST http://localhost:8080/actuator/refresh
```

### Con Reinicio (método simple)

1. Cambiar `application.yml`:
```yaml
app:
  whatsapp:
    provider: twilio  # Cambiar de "meta" a "twilio"
```

2. Reiniciar aplicación
3. Verificar logs de inicio

---

## ⚠️ Problemas Comunes

### Problema 1: Múltiples Adapters Activos

**Síntoma:**
```
NoUniqueBeanDefinitionException: 
expected single matching bean but found 2: 
metaWhatsAppAdapter, twilioWhatsAppAdapter
```

**Causa:** Ambos adapters tienen `matchIfMissing = true`

**Solución:** Todos deben tener `matchIfMissing = false` ✅

---

### Problema 2: Ningún Adapter Activo

**Síntoma:**
```
NoSuchBeanDefinitionException: 
No qualifying bean of type 'WhatsAppService' available
```

**Causa:** Configuración `app.whatsapp.provider` no coincide con ningún adapter

**Solución:** Verificar configuración:
```yaml
app:
  whatsapp:
    provider: meta  # Debe ser: meta, twilio, o mock
```

---

### Problema 3: Credenciales No Configuradas

**Síntoma:**
```
WARN : Credenciales de Meta WhatsApp no configuradas
ERROR: No se puede enviar mensaje: Meta WhatsApp no está configurado
```

**Solución:** Configurar variables de entorno:
```bash
export META_WHATSAPP_ACCESS_TOKEN=your_token
export META_WHATSAPP_PHONE_NUMBER_ID=your_phone_id
```

---

## 📝 Checklist de Verificación

Cuando configures un adapter:

- [ ] Configurar `app.whatsapp.provider` correctamente
- [ ] Configurar credenciales necesarias (access-token, account-sid, etc.)
- [ ] Verificar logs de inicio
- [ ] Probar envío de mensaje simple
- [ ] Verificar webhooks (para Meta)
- [ ] Validar que solo un adapter está activo

---

## 🎯 Recomendaciones

### Para Desarrollo
✅ Usar `mock` - Sin credenciales, logs detallados

### Para Staging/Testing
✅ Usar `meta` o `twilio` - Credenciales de prueba

### Para Producción
✅ **Usar `meta`** - API oficial, sin intermediarios, más económico

---

## 📊 Resumen de Correcciones

| Adapter | Antes | Después | Estado |
|---------|-------|---------|--------|
| MetaWhatsAppAdapter | `matchIfMissing = true` ❌ | `matchIfMissing = false` ✅ | CORREGIDO |
| TwilioWhatsAppAdapter | ✅ Correcto | ✅ Correcto | OK |
| MockWhatsAppAdapter | ✅ Correcto | ✅ Correcto | OK |

---

**¡Ahora todos los adapters de WhatsApp tienen las condiciones correctas!** 🎉

Solo uno se activará según tu configuración en `app.whatsapp.provider`.

