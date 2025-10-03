# ✅ OpenAI Embeddings Adapter - Implementación Completa

## 🎯 Resumen

Se ha implementado un **adapter completo para generar embeddings** usando la API de OpenAI, siguiendo las mejores prácticas y la documentación oficial.

---

## 📦 Archivos Creados/Modificados

### ✅ Archivos Nuevos

1. **`OpenAIEmbeddingsAdapter.java`**
   - Ubicación: `infrastructure/adapters/out/ai/openai/`
   - Implementa: `EmbeddingsPort`
   - Funcionalidad: Genera embeddings usando OpenAI API

2. **`OpenAIConfig.java`**
   - Ubicación: `infrastructure/config/`
   - Configura: WebClient para OpenAI API con timeouts y logging

3. **`OPENAI_EMBEDDINGS_ADAPTER.md`**
   - Documentación completa del adapter

4. **`.env.example`**
   - Plantilla de variables de entorno

### ✅ Archivos Modificados

5. **`pom.xml`**
   - Agregadas dependencias: `spring-boot-starter-webflux`, `reactor-netty`

6. **`application.yml`**
   - Agregada configuración de OpenAI

7. **`application-dev.yml`**
   - Configuración de desarrollo (modelo más económico)

8. **`application-prod.yml`**
   - Configuración de producción (mejor calidad)

---

## 🚀 Características Principales

### ✅ Funcionalidades

- **Embeddings individuales**: `embedOne(String text)`
- **Embeddings por lotes**: `embedMany(List<String> texts)`
- **Manejo completo de errores** de OpenAI API
- **Logging detallado** de requests y uso de tokens
- **Validación de inputs**
- **Activación condicional** con `@ConditionalOnProperty`

### ✅ Modelos Soportados

| Modelo | Dimensiones | Costo/1M tokens | Calidad |
|--------|-------------|-----------------|---------|
| `text-embedding-3-large` | 3072 | $0.13 | ⭐⭐⭐⭐⭐ |
| `text-embedding-3-small` | 1536 | $0.02 | ⭐⭐⭐⭐ |
| `text-embedding-ada-002` | 1536 | $0.10 | ⭐⭐⭐ (legacy) |

---

## ⚙️ Configuración Rápida

### 1. Variable de Entorno

```bash
export OPENAI_API_KEY=sk-proj-xxxxx...
```

### 2. application.yml (ya configurado)

```yaml
app:
  ai:
    embeddings:
      provider: openai
    
    openai:
      api-key: ${OPENAI_API_KEY}
      embeddings:
        model: text-embedding-3-large
        dimensions: 3072
```

---

## 🧪 Prueba Rápida

### 1. Crear Cliente

```bash
curl -X POST http://localhost:8080/api/clients \
  -H 'Content-Type: application/json' \
  -d '{
  "code": "CLI-001",
  "name": "Empresa Demo",
  "status": "ACTIVE"
}'
```

### 2. Crear Knowledge Base

```bash
curl -X POST http://localhost:8080/api/knowledge-base \
  -H 'Content-Type: application/json' \
  -d '{
  "clientId": "TU_CLIENT_ID",
  "name": "FAQ Empresa",
  "description": "Preguntas frecuentes"
}'
```

### 3. Ingestar Documentos (Usa OpenAI Embeddings)

```bash
curl -X POST http://localhost:8080/api/knowledge-base/TU_KB_ID/ingest \
  -H 'Content-Type: application/json' \
  -d '{
  "documents": [
    {
      "content": "Nuestro horario es Lunes a Viernes de 9am a 6pm",
      "source": "FAQ",
      "category": "horarios"
    }
  ]
}'
```

### 4. Buscar (Usa OpenAI Embeddings)

```bash
curl -X POST http://localhost:8080/api/knowledge-base/TU_KB_ID/search \
  -H 'Content-Type: application/json' \
  -d '{
  "query": "¿Cuáles son los horarios?",
  "topK": 3
}'
```

---

## 📊 Flujo de Datos

```
Usuario → POST /api/knowledge-base/{kbId}/ingest
    ↓
IngestDocuments (Use Case)
    ↓
EmbeddingsPort.embedMany(texts)
    ↓
OpenAIEmbeddingsAdapter
    ↓
POST https://api.openai.com/v1/embeddings
{
  "input": ["texto 1", "texto 2"],
  "model": "text-embedding-3-large",
  "dimensions": 3072
}
    ↓
Response: embeddings (float[3072] cada uno)
    ↓
VectorStore.upsert(namespace, vectors)
    ↓
PostgreSQL (pgvector)
```

---

## 🔒 Seguridad

### ✅ API Key
- Nunca incluir en el código
- Usar variables de entorno
- Rotar periódicamente

### ✅ Rate Limiting
- OpenAI tiene límites por minuto
- Implementar retry con backoff (opcional)

### ✅ Validación
- Textos no vacíos
- Longitud máxima: 8191 tokens por texto
- Batch máximo: 2048 textos

---

## 💰 Costos Estimados

### Ejemplo: 1000 documentos de ~500 palabras

**Desarrollo (text-embedding-3-small):**
- Tokens: ~750,000
- Costo: **$0.015**

**Producción (text-embedding-3-large):**
- Tokens: ~750,000
- Costo: **$0.10**

---

## 🔄 Integración con Casos de Uso Existentes

El adapter se integra **automáticamente** con:

- ✅ `IngestDocuments` - Ingesta de documentos al KB
- ✅ `SearchDocuments` - Búsqueda semántica
- ✅ `ProcessMessageWithAI` - RAG para respuestas

**No requiere cambios en el código existente**, solo configurar el provider:

```yaml
app:
  ai:
    embeddings:
      provider: openai  # ← Cambiar de "http" a "openai"
```

---

## 📝 Logs Ejemplo

```
DEBUG - Generando embedding para texto de 42 caracteres con modelo text-embedding-3-large
DEBUG - OpenAI API Request: POST https://api.openai.com/v1/embeddings
DEBUG - OpenAI API Response: 200 OK
DEBUG - Embedding generado exitosamente. Tokens usados: 12

DEBUG - Generando embeddings para 5 textos con modelo text-embedding-3-large
DEBUG - OpenAI API Request: POST https://api.openai.com/v1/embeddings
DEBUG - OpenAI API Response: 200 OK
DEBUG - Embeddings generados exitosamente. Tokens usados: 87
```

---

## ⚡ Ventajas vs. Adapter HTTP Genérico

| Característica | HTTP Genérico | OpenAI Adapter |
|----------------|---------------|----------------|
| API | Cualquier servidor | OpenAI oficial |
| Modelos | Manual | Automático |
| Dimensiones | Configuración | Por modelo |
| Errores | Genéricos | Específicos de OpenAI |
| Logs | Básicos | Detallados + tokens |
| Validación | Básica | Completa |
| Documentación | Mínima | Completa |

---

## 🎯 Próximos Pasos Opcionales

### 1. Agregar Caching
```java
@Cacheable(value = "embeddings", key = "#text")
public float[] embedOne(String text) { ... }
```

### 2. Agregar Métricas
```java
meterRegistry.counter("openai.embeddings.requests").increment();
meterRegistry.counter("openai.embeddings.tokens", "type", "total")
    .increment(response.usage().total_tokens());
```

### 3. Agregar Retry
```java
.retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
    .filter(throwable -> throwable instanceof WebClientResponseException))
```

### 4. Implementar Rate Limiting Local
```java
@RateLimiter(name = "openai", fallbackMethod = "fallback")
```

---

## 🔗 Referencias

- **Documentación:** https://platform.openai.com/docs/guides/embeddings
- **API Reference:** https://platform.openai.com/docs/api-reference/embeddings
- **Pricing:** https://openai.com/api/pricing/
- **Modelos:** https://platform.openai.com/docs/models

---

## ✅ Checklist de Implementación

- [x] Adapter creado con implementación completa
- [x] Configuración de WebClient
- [x] Manejo de errores
- [x] Validación de inputs
- [x] Logging detallado
- [x] Configuración por ambiente (dev/prod)
- [x] Documentación completa
- [x] Dependencias agregadas al pom.xml
- [x] Variables de entorno documentadas
- [x] Activación condicional configurada

---

**¡El adapter de OpenAI Embeddings está 100% listo para producción!** 🎉

Solo necesitas:
1. Configurar tu `OPENAI_API_KEY`
2. Reiniciar la aplicación
3. Los embeddings se generarán automáticamente al ingestar documentos

