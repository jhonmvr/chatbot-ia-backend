# 🤖 OpenAI Embeddings Adapter

## ✅ Implementación Completa

Se ha creado un adapter completo para generar embeddings usando **OpenAI Embeddings API**.

---

## 📁 Archivos Creados

### 1. **OpenAIEmbeddingsAdapter.java**
**Ubicación:** `infrastructure/adapters/out/ai/openai/OpenAIEmbeddingsAdapter.java`

**Características:**
- ✅ Implementa `EmbeddingsPort`
- ✅ Soporta embeddings individuales y por lotes
- ✅ Manejo completo de errores de OpenAI API
- ✅ Logging detallado de requests y tokens usados
- ✅ Validación de inputs
- ✅ Activación condicional con `@ConditionalOnProperty`

**Modelos Soportados:**
- `text-embedding-3-large` - 3072 dimensiones (mejor calidad, $0.13/1M tokens)
- `text-embedding-3-small` - 1536 dimensiones (más económico, $0.02/1M tokens)
- `text-embedding-ada-002` - 1536 dimensiones (legacy, $0.10/1M tokens)

---

### 2. **OpenAIConfig.java**
**Ubicación:** `infrastructure/config/OpenAIConfig.java`

**Características:**
- ✅ WebClient configurado específicamente para OpenAI API
- ✅ Timeouts configurables (default: 60s)
- ✅ Connection pooling optimizado
- ✅ Logging de requests/responses
- ✅ Manejo robusto de conexiones

---

## ⚙️ Configuración

### Variables de Entorno

```bash
# API Key de OpenAI (REQUERIDO)
OPENAI_API_KEY=sk-proj-xxxxx...
```

### application.yml

```yaml
app:
  ai:
    embeddings:
      provider: openai  # openai | http | mock
    
    openai:
      api-url: https://api.openai.com
      api-key: ${OPENAI_API_KEY}
      timeout: 60
      
      embeddings:
        model: text-embedding-3-large
        dimensions: 3072
```

### Configuración por Ambiente

#### Desarrollo (`application-dev.yml`)
```yaml
app:
  ai:
    embeddings:
      provider: openai
    
    openai:
      embeddings:
        model: text-embedding-3-small  # Más económico
        dimensions: 1536
```

#### Producción (`application-prod.yml`)
```yaml
app:
  ai:
    embeddings:
      provider: openai
    
    openai:
      embeddings:
        model: text-embedding-3-large  # Mejor calidad
        dimensions: 3072
```

---

## 🚀 Uso

### 1. Embedding Individual

```java
@Autowired
private EmbeddingsPort embeddingsPort;

public void generateEmbedding() {
    String text = "¿Cuáles son tus horarios de atención?";
    float[] embedding = embeddingsPort.embedOne(text);
    
    // embedding es un array de 3072 floats (para text-embedding-3-large)
    System.out.println("Dimensiones: " + embedding.length);
}
```

### 2. Embeddings por Lotes (Batch)

```java
public void generateMultipleEmbeddings() {
    List<String> texts = List.of(
        "Horario: Lunes a Viernes de 9am a 6pm",
        "Envíos gratuitos en compras mayores a $50",
        "Aceptamos tarjetas de crédito y débito"
    );
    
    List<float[]> embeddings = embeddingsPort.embedMany(texts);
    
    // embeddings.size() == texts.size()
    System.out.println("Generados " + embeddings.size() + " embeddings");
}
```

### 3. Uso en Knowledge Base

El adapter se integra automáticamente con los casos de uso existentes:

```java
// IngestDocuments ya usa EmbeddingsPort
@Autowired
private IngestDocuments ingestDocuments;

public void ingestContent() {
    List<Document> documents = List.of(
        new Document("doc1", "Contenido del documento 1", metadata),
        new Document("doc2", "Contenido del documento 2", metadata)
    );
    
    // Genera embeddings automáticamente y los guarda en pgvector
    ingestDocuments.handle("kb_namespace", documents);
}
```

---

## 📊 Request/Response OpenAI API

### Request Example

```json
POST https://api.openai.com/v1/embeddings
Authorization: Bearer sk-proj-xxxxx
Content-Type: application/json

{
  "input": "¿Cuáles son tus horarios de atención?",
  "model": "text-embedding-3-large",
  "dimensions": 3072,
  "encoding_format": "float"
}
```

### Response Example

```json
{
  "object": "list",
  "data": [
    {
      "object": "embedding",
      "index": 0,
      "embedding": [0.0012, -0.0034, 0.0056, ...]  // 3072 floats
    }
  ],
  "model": "text-embedding-3-large",
  "usage": {
    "prompt_tokens": 12,
    "total_tokens": 12
  }
}
```

---

## 🔒 Manejo de Errores

El adapter maneja todos los errores de OpenAI API:

### Errores Comunes

**1. API Key inválida (401)**
```
OpenAI API error: Incorrect API key provided
```

**2. Rate limit excedido (429)**
```
OpenAI API error: Rate limit exceeded
```

**3. Texto demasiado largo (400)**
```
OpenAI API error: This model's maximum context length is 8191 tokens
```

**4. Timeout (Configurado en 60s)**
```
Error al generar embedding con OpenAI: Read timed out
```

### Logging

```
DEBUG - Generando embedding para texto de 42 caracteres con modelo text-embedding-3-large
DEBUG - OpenAI API Request: POST https://api.openai.com/v1/embeddings
DEBUG - OpenAI API Response: 200 OK
DEBUG - Embedding generado exitosamente. Tokens usados: 12
```

---

## 💰 Costos (Precios de OpenAI)

| Modelo | Dimensiones | Costo por 1M tokens |
|--------|-------------|---------------------|
| `text-embedding-3-large` | 3072 | $0.13 |
| `text-embedding-3-small` | 1536 | $0.02 |
| `text-embedding-ada-002` | 1536 | $0.10 |

**Ejemplo de cálculo:**
- 1000 documentos de ~500 palabras cada uno = ~750,000 tokens
- Con `text-embedding-3-small`: $0.015
- Con `text-embedding-3-large`: $0.0975

---

## 🔄 Comparación con el Adapter Anterior

| Característica | `HttpEmbeddingsClient` | `OpenAIEmbeddingsAdapter` |
|----------------|------------------------|---------------------------|
| Proveedor | Genérico HTTP | OpenAI específico |
| Modelos | Configuración manual | Modelos OpenAI nativos |
| Dimensiones | Configuración manual | Automático según modelo |
| Manejo de errores | Básico | Completo con detalles |
| Logging | Básico | Detallado con tokens |
| Retry | No | No (puede agregarse) |
| Rate limiting | No | No (puede agregarse) |

---

## 🧪 Pruebas

### Prueba Simple

```bash
# Crear un documento en el Knowledge Base
curl -X POST http://localhost:8080/api/knowledge-base/{kbId}/ingest \
  -H 'Content-Type: application/json' \
  -d '{
  "documents": [
    {
      "content": "Nuestro horario de atención es Lunes a Viernes de 9am a 6pm",
      "source": "FAQ",
      "category": "horarios"
    }
  ]
}'
```

### Buscar en Knowledge Base

```bash
curl -X POST http://localhost:8080/api/knowledge-base/{kbId}/search \
  -H 'Content-Type: application/json' \
  -d '{
  "query": "¿Cuáles son los horarios?",
  "topK": 3
}'
```

---

## 📝 Validaciones

El adapter valida:
- ✅ Texto no nulo ni vacío
- ✅ Lista de textos no vacía
- ✅ Ningún texto en la lista está vacío
- ✅ Número de embeddings coincide con número de textos
- ✅ API Key configurada

---

## 🔧 Configuración Avanzada

### Usar Diferentes Modelos por Caso de Uso

```yaml
app:
  ai:
    openai:
      embeddings:
        # Modelo por defecto
        model: text-embedding-3-large
        dimensions: 3072
        
        # Modelos específicos (requiere implementación custom)
        models:
          search: text-embedding-3-large  # Para búsqueda
          ingestion: text-embedding-3-small  # Para ingesta masiva
```

### Ajustar Timeouts

```yaml
app:
  ai:
    openai:
      timeout: 90  # Aumentar para documentos muy grandes
```

### Cambiar URL (para proxies o ambientes especiales)

```yaml
app:
  ai:
    openai:
      api-url: https://api.openai.com  # O tu proxy personalizado
```

---

## 🚀 Próximos Pasos

### Mejoras Opcionales

1. **Retry con Backoff Exponencial**
   ```java
   .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
   ```

2. **Rate Limiting**
   ```java
   @RateLimiter(name = "openai")
   ```

3. **Caching de Embeddings**
   ```java
   @Cacheable(value = "embeddings", key = "#text")
   ```

4. **Métricas de Uso**
   ```java
   meterRegistry.counter("openai.embeddings.requests").increment();
   ```

5. **Batch Automático**
   - Dividir automáticamente lotes grandes (>2048 textos)

---

## 🔗 Referencias

- **OpenAI Embeddings Guide:** https://platform.openai.com/docs/guides/embeddings
- **API Reference:** https://platform.openai.com/docs/api-reference/embeddings
- **Pricing:** https://openai.com/api/pricing/
- **Best Practices:** https://platform.openai.com/docs/guides/embeddings/use-cases

---

**¡El adapter de OpenAI Embeddings está listo para usar!** 🎉

Solo necesitas configurar tu `OPENAI_API_KEY` y el sistema empezará a generar embeddings automáticamente.

