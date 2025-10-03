# 📱 Configuración de WhatsApp - Guía Completa

## ✅ Configuración Agregada

Se ha agregado la configuración completa de WhatsApp en todos los archivos de configuración.

---

## 📋 Archivos Configurados

### 1. **application.yml** (Base)
```yaml
app:
  whatsapp:
    provider: mock  # Por defecto usa mock para desarrollo
    
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN:}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID:}
      webhook-verify-token: ${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN:my-secret-token}
      api-version: ${META_WHATSAPP_API_VERSION:v21.0}
    
    twilio:
      account-sid: ${TWILIO_ACCOUNT_SID:}
      auth-token: ${TWILIO_AUTH_TOKEN:}
      whatsapp-from: ${TWILIO_WHATSAPP_FROM:whatsapp:+14155238886}
```

### 2. **application-dev.yml** (Desarrollo)
```yaml
app:
  whatsapp:
    provider: mock  # Sin credenciales necesarias
```

### 3. **application-prod.yml** (Producción)
```yaml
app:
  whatsapp:
    provider: meta  # Meta WhatsApp Business API (recomendado)
    
    meta:
      access-token: ${META_WHATSAPP_ACCESS_TOKEN}
      phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
      webhook-verify-token: ${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN}
      api-version: v21.0
```

---

## 🔑 Variables de Entorno Necesarias

### Para Meta WhatsApp Business API (Producción)

```bash
# Meta WhatsApp Business API
export META_WHATSAPP_ACCESS_TOKEN="EAAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
export META_WHATSAPP_PHONE_NUMBER_ID="123456789012345"
export META_WHATSAPP_WEBHOOK_VERIFY_TOKEN="mi-token-secreto-123"
export META_WHATSAPP_API_VERSION="v21.0"
```

#### ¿Dónde obtener estas credenciales?

1. **Access Token:**
   - Ve a: https://developers.facebook.com/apps
   - Selecciona tu app
   - WhatsApp > API Setup
   - Copia el "Temporary access token" (o genera uno permanente)

2. **Phone Number ID:**
   - En la misma página de API Setup
   - Busca "Phone number ID" bajo tu número de WhatsApp Business

3. **Webhook Verify Token:**
   - Es un token que TÚ creas
   - Debe ser el mismo que configuras en el webhook de Meta
   - Ejemplo: `my-super-secret-token-2024`

4. **API Version:**
   - Usa `v21.0` (última versión estable)
   - O la versión que prefieras: https://developers.facebook.com/docs/graph-api/changelog

---

### Para Twilio (Alternativa)

```bash
# Twilio API
export TWILIO_ACCOUNT_SID="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
export TWILIO_AUTH_TOKEN="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
export TWILIO_WHATSAPP_FROM="whatsapp:+14155238886"
```

#### ¿Dónde obtener estas credenciales?

1. **Account SID y Auth Token:**
   - Ve a: https://console.twilio.com/
   - En el Dashboard verás tu Account SID y Auth Token

2. **WhatsApp From:**
   - Si usas sandbox: `whatsapp:+14155238886`
   - Si tienes número aprobado: `whatsapp:+TU_NUMERO`

---

## 🚀 Configurar Meta WhatsApp Business API

### Paso 1: Crear App en Facebook Developers

1. Ve a https://developers.facebook.com/apps
2. Click en "Create App"
3. Selecciona "Business" como tipo
4. Completa los datos de tu app

### Paso 2: Agregar WhatsApp a tu App

1. En tu app, click en "Add Product"
2. Selecciona "WhatsApp"
3. Click en "Set up"

### Paso 3: Configurar Número de WhatsApp

1. En "API Setup", selecciona o agrega un número de teléfono
2. Verifica el número siguiendo las instrucciones
3. Copia el "Phone number ID"

### Paso 4: Generar Access Token

**Opción A: Token Temporal (para pruebas)**
1. En "API Setup", copia el "Temporary access token"
2. Válido por 24 horas
3. Úsalo para desarrollo

**Opción B: Token Permanente (para producción)**
1. Ve a "Settings" > "Basic"
2. Genera un "System User Token"
3. Otorga permisos: `whatsapp_business_messaging`, `whatsapp_business_management`
4. Guarda el token de forma segura

### Paso 5: Configurar Webhook

1. En "Configuration" > "Webhook"
2. Click en "Edit"
3. Configura:
   - **Callback URL:** `https://tu-dominio.com/webhooks/whatsapp/meta`
   - **Verify Token:** El mismo que configuraste en `META_WHATSAPP_WEBHOOK_VERIFY_TOKEN`
4. Suscríbete a los siguientes eventos:
   - `messages` ✅
   - `message_status` (opcional)

### Paso 6: Variables de Entorno

```bash
export META_WHATSAPP_ACCESS_TOKEN="tu_access_token_aqui"
export META_WHATSAPP_PHONE_NUMBER_ID="tu_phone_number_id"
export META_WHATSAPP_WEBHOOK_VERIFY_TOKEN="tu_token_verificacion"
```

### Paso 7: Reiniciar Aplicación

```bash
# Activar perfil de producción
export SPRING_PROFILES_ACTIVE=prod

# Iniciar aplicación
java -jar chatbot-ia-backend.jar
```

---

## 🧪 Probar la Configuración

### 1. Verificar que el adapter correcto está activo

**Logs esperados:**
```
INFO : MetaWhatsAppAdapter inicializado con Phone Number ID: 123456789012345
```

### 2. Verificar webhook

```bash
curl -X GET "https://tu-dominio.com/webhooks/whatsapp/meta?hub.mode=subscribe&hub.verify_token=tu_token&hub.challenge=12345"
```

**Respuesta esperada:**
```
12345
```

### 3. Enviar mensaje de prueba

Envía un mensaje desde la consola de Meta:
1. Ve a "API Setup" en Facebook Developers
2. Envía un mensaje de prueba a tu número
3. Verifica que tu aplicación lo recibe

---

## 🔄 Cambiar de Provider

### De Mock a Meta (para producción)

**1. Configurar variables de entorno:**
```bash
export META_WHATSAPP_ACCESS_TOKEN="..."
export META_WHATSAPP_PHONE_NUMBER_ID="..."
export META_WHATSAPP_WEBHOOK_VERIFY_TOKEN="..."
```

**2. Activar perfil de producción:**
```bash
export SPRING_PROFILES_ACTIVE=prod
```

**3. Reiniciar aplicación**

### De Meta a Twilio

**1. Cambiar configuración:**
```yaml
app:
  whatsapp:
    provider: twilio
```

**2. Configurar variables:**
```bash
export TWILIO_ACCOUNT_SID="..."
export TWILIO_AUTH_TOKEN="..."
export TWILIO_WHATSAPP_FROM="whatsapp:+14155238886"
```

**3. Reiniciar aplicación**

---

## 📊 Comparación de Providers

### Meta WhatsApp Business API ⭐ (Recomendado)

**Ventajas:**
- ✅ API oficial de WhatsApp
- ✅ Sin intermediarios (más económico)
- ✅ Acceso a todas las funcionalidades
- ✅ Mejor integración con funciones avanzadas
- ✅ Webhooks nativos

**Desventajas:**
- ⚠️ Configuración inicial más compleja
- ⚠️ Requiere aprobación de Meta
- ⚠️ Límites de mensajería por nivel de cuenta

**Costo:**
- Conversaciones iniciadas por el usuario: GRATIS (primeras 1000/mes)
- Conversaciones iniciadas por el negocio: Variables según país

---

### Twilio WhatsApp

**Ventajas:**
- ✅ Configuración más simple
- ✅ Buen soporte técnico
- ✅ Sandbox para desarrollo
- ✅ Integración con otros canales

**Desventajas:**
- ⚠️ Costo adicional por mensaje
- ⚠️ Intermediario entre tú y WhatsApp
- ⚠️ Menos funcionalidades avanzadas

**Costo:**
- ~$0.005 - $0.01 por mensaje (varía por país)

---

### Mock (Solo Desarrollo)

**Ventajas:**
- ✅ Sin credenciales necesarias
- ✅ Logs detallados
- ✅ Sin costo
- ✅ Ideal para desarrollo local

**Desventajas:**
- ❌ No envía mensajes reales
- ❌ Solo para testing

---

## 🔒 Seguridad

### 1. **Nunca commitear credenciales**

❌ **MAL:**
```yaml
meta:
  access-token: "EAAxxxxxxxxxxxx"  # ❌ NO HACER ESTO
```

✅ **BIEN:**
```yaml
meta:
  access-token: ${META_WHATSAPP_ACCESS_TOKEN}  # ✅ Usar variables de entorno
```

### 2. **Usar variables de entorno**

```bash
# .env (gitignored)
META_WHATSAPP_ACCESS_TOKEN=EAAxxxx...
META_WHATSAPP_PHONE_NUMBER_ID=12345...
```

### 3. **Rotar tokens regularmente**

- Genera nuevos tokens cada 3-6 meses
- Revoca tokens comprometidos inmediatamente

### 4. **Webhook Verify Token fuerte**

❌ Débil: `token123`  
✅ Fuerte: `whatsapp-prod-2024-a8b7c6d5e4f3-secure`

### 5. **HTTPS obligatorio en producción**

Meta requiere HTTPS para webhooks:
```
https://tu-dominio.com/webhooks/whatsapp/meta
```

---

## 🐛 Troubleshooting

### Error: "Meta WhatsApp no está configurado"

**Causa:** Variables de entorno no configuradas

**Solución:**
```bash
export META_WHATSAPP_ACCESS_TOKEN="..."
export META_WHATSAPP_PHONE_NUMBER_ID="..."
```

---

### Error: "Webhook verification failed"

**Causa:** Token de verificación no coincide

**Solución:**
1. Verificar que `META_WHATSAPP_WEBHOOK_VERIFY_TOKEN` sea el mismo en:
   - Variables de entorno
   - Configuración del webhook en Meta

---

### Error: "(#100) The parameter recipient_phone_number is required"

**Causa:** Número de teléfono mal formateado

**Solución:**
- Usar formato internacional sin espacios: `593999999999`
- Sin el prefijo `+`
- Solo dígitos

---

### Error: "Message failed to send"

**Causas posibles:**
1. Access token expirado
2. Número no válido
3. Límites de mensajería alcanzados
4. Número no registrado como de prueba (en modo desarrollo)

**Solución:**
- Verificar logs detallados
- Regenerar access token
- Verificar estado de la cuenta en Facebook Business

---

## 📝 Checklist de Configuración

### Desarrollo
- [ ] Configurar `provider: mock` en `application-dev.yml`
- [ ] Verificar que la app inicia sin errores
- [ ] Logs muestran `[MOCK]` al enviar mensajes

### Producción
- [ ] Crear app en Facebook Developers
- [ ] Configurar número de WhatsApp Business
- [ ] Generar access token permanente
- [ ] Configurar webhook con HTTPS
- [ ] Establecer variables de entorno
- [ ] Configurar `provider: meta` en `application-prod.yml`
- [ ] Verificar logs de inicio (MetaWhatsAppAdapter inicializado)
- [ ] Probar envío de mensaje
- [ ] Probar recepción de mensaje (webhook)

---

## 🔗 Referencias

- **Meta WhatsApp Business API:** https://developers.facebook.com/docs/whatsapp/cloud-api
- **Twilio WhatsApp API:** https://www.twilio.com/docs/whatsapp
- **Facebook App Dashboard:** https://developers.facebook.com/apps
- **Twilio Console:** https://console.twilio.com/

---

**¡La configuración de WhatsApp está completa y lista para todos los ambientes!** 🎉

