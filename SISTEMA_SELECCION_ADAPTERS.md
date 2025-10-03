# 🔀 Sistema de Selección de Adapters

## ❓ ¿Cómo sabe el sistema qué adapter usar?

El sistema utiliza **`@ConditionalOnProperty`** de Spring Boot para activar/desactivar adapters según la configuración en `application.yml`.

---

## 🎯 Mecanismo de Selección

### 1. **Anotación @ConditionalOnProperty**

Cada adapter tiene una anotación que le dice a Spring cuándo debe activarse:

```java
@Component
@ConditionalOnProperty(
    name = "app.ai.embeddings.provider",  // Propiedad a evaluar
    havingValue = "openai",                // Valor esperado
    matchIfMissing = false                 // Si no existe, NO activar
)
public class OpenAIEmbeddingsAdapter implements EmbeddingsPort {
    // ...
}
```

### 2. **Configuración en application.yml**

```yaml
app:
  ai:
    embeddings:
      provider: openai  # ← Este valor determina qué adapter se activa
```

---

## 📊 Adapters de Embeddings Disponibles

### ✅ Adapter 1: OpenAI (Recomendado)

**Archivo:** `OpenAIEmbeddingsAdapter.java`

**Se activa cuando:**
```yaml
app:
  ai:
    embeddings:
      provider: openai
```

**Anotación:**
```java
@ConditionalOnProperty(
    name = "app.ai.embeddings.provider", 
    havingValue = "openai", 
    matchIfMissing = false
)
```

**Características:**
- ✅ API oficial de OpenAI
- ✅ Modelos: text-embedding-3-large, text-embedding-3-small, ada-002
- ✅ Manejo completo de errores
- ✅ Logging detallado con tokens usados

---

### ✅ Adapter 2: HTTP Genérico

**Archivo:** `HttpEmbeddingsClient.java`

**Se activa cuando:**
```yaml
app:
  ai:
    embeddings:
      provider: http
```

**Anotación:**
```java
@ConditionalOnProperty(
    name = "app.ai.embeddings.provider", 
    havingValue = "http", 
    matchIfMissing = false
)
```

**Características:**
- ✅ Para servicios custom o locales
- ✅ Cualquier API compatible con formato OpenAI
- ✅ Más flexible pero menos validación

---

### ✅ Adapter 3: Mock (Desarrollo)

**Para crear un adapter Mock:**

```java
@Component
@ConditionalOnProperty(
    name = "app.ai.embeddings.provider", 
    havingValue = "mock", 
    matchIfMissing = false
)
public class MockEmbeddingsAdapter implements EmbeddingsPort {
    @Override
    public float[] embedOne(String text) {
        // Retornar vector aleatorio para testing
        return new float[1536];
    }
    
    @Override
    public List<float[]> embedMany(List<String> texts) {
        return texts.stream()
            .map(t -> new float[1536])
            .toList();
    }
    
    @Override
    public String model() {
        return "mock-model";
    }
}
```

**Se activa cuando:**
```yaml
app:
  ai:
    embeddings:
      provider: mock
```

---

## 🔄 Flujo de Selección

```
┌─────────────────────────────────────────┐
│ 1. Spring Boot inicia                   │
└───────────────┬─────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│ 2. Lee application.yml                  │
│    app.ai.embeddings.provider: "openai" │
└───────────────┬─────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│ 3. Evalúa @ConditionalOnProperty        │
│    en cada clase @Component             │
└───────────────┬─────────────────────────┘
                ↓
    ┌───────────┴───────────┐
    ↓                       ↓
┌──────────────┐    ┌──────────────┐
│ OpenAI       │    │ HTTP         │
│ provider==   │    │ provider==   │
│ "openai"? ✅ │    │ "http"? ❌   │
│ ACTIVADO     │    │ DESACTIVADO  │
└──────────────┘    └──────────────┘
```

---

## 🧪 Ejemplos de Configuración

### Ejemplo 1: Usar OpenAI (Producción)

**application-prod.yml:**
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

**Resultado:**
- ✅ `OpenAIEmbeddingsAdapter` → **ACTIVO**
- ❌ `HttpEmbeddingsClient` → **INACTIVO**

---

### Ejemplo 2: Usar Servidor Local (Desarrollo)

**application-dev.yml:**
```yaml
app:
  ai:
    embeddings:
      provider: http

ai:
  base-url: http://localhost:8000  # Tu servidor local
```

**Resultado:**
- ❌ `OpenAIEmbeddingsAdapter` → **INACTIVO**
- ✅ `HttpEmbeddingsClient` → **ACTIVO**

---

### Ejemplo 3: Mock para Testing

**application-test.yml:**
```yaml
app:
  ai:
    embeddings:
      provider: mock
```

**Resultado:**
- ❌ `OpenAIEmbeddingsAdapter` → **INACTIVO**
- ❌ `HttpEmbeddingsClient` → **INACTIVO**
- ✅ `MockEmbeddingsAdapter` → **ACTIVO** (si lo creas)

---

## 🔍 Verificar Qué Adapter Está Activo

### 1. **Logs de Inicio**

Cuando la aplicación inicia, Spring muestra qué beans se crean:

```
INFO  : Creating bean 'openAIEmbeddingsAdapter'
DEBUG : Skipping bean 'httpEmbeddingsClient' - @ConditionalOnProperty did not match
```

### 2. **Endpoint de Actuator**

```bash
curl http://localhost:8080/actuator/beans | jq '.contexts.application.beans | keys | .[] | select(contains("Embeddings"))'
```

### 3. **Logs de tu Aplicación**

El `OpenAIEmbeddingsAdapter` tiene logs:

```
DEBUG - Generando embedding para texto de 42 caracteres con modelo text-embedding-3-large
```

Si ves estos logs, sabes que OpenAI está activo.

---

## ⚠️ Problema: Múltiples Adapters Activos

### ❌ Error: Dos adapters sin condiciones

Si ambos adapters no tienen `@ConditionalOnProperty`:

```java
@Component
public class OpenAIEmbeddingsAdapter implements EmbeddingsPort { ... }

@Component
public class HttpEmbeddingsClient implements EmbeddingsPort { ... }
```

**Resultado:**
```
ERROR: NoUniqueBeanDefinitionException: 
No qualifying bean of type 'EmbeddingsPort' available: 
expected single matching bean but found 2: 
openAIEmbeddingsAdapter, httpEmbeddingsClient
```

### ✅ Solución: Usar @ConditionalOnProperty

```java
@Component
@ConditionalOnProperty(name = "app.ai.embeddings.provider", havingValue = "openai")
public class OpenAIEmbeddingsAdapter implements EmbeddingsPort { ... }

@Component
@ConditionalOnProperty(name = "app.ai.embeddings.provider", havingValue = "http")
public class HttpEmbeddingsClient implements EmbeddingsPort { ... }
```

---

## 🎛️ Cambiar de Adapter Sin Reiniciar (Avanzado)

### Opción 1: Spring Cloud Config (Recomendado)

Permite cambiar configuración en runtime:

```yaml
spring:
  cloud:
    config:
      enabled: true
```

### Opción 2: @RefreshScope

```java
@Component
@RefreshScope
@ConditionalOnProperty(...)
public class OpenAIEmbeddingsAdapter implements EmbeddingsPort { ... }
```

Luego:
```bash
curl -X POST http://localhost:8080/actuator/refresh
```

---

## 📝 Orden de Precedencia de Configuración

Spring Boot evalúa propiedades en este orden (de menor a mayor prioridad):

1. `application.yml` (base)
2. `application-{profile}.yml` (perfil activo)
3. Variables de entorno
4. Argumentos de línea de comando

**Ejemplo:**

```bash
# application.yml
app:
  ai:
    embeddings:
      provider: http

# application-prod.yml
app:
  ai:
    embeddings:
      provider: openai  # ← Gana en producción

# Variable de entorno (gana sobre todo)
export APP_AI_EMBEDDINGS_PROVIDER=mock

# Línea de comando (máxima prioridad)
java -jar app.jar --app.ai.embeddings.provider=openai
```

---

## 🧩 Patrón de Diseño: Strategy Pattern

Este sistema implementa el **Strategy Pattern**:

```
    EmbeddingsPort (Interface)
           ↑
           |
    ┌──────┼──────┐
    |      |      |
OpenAI   HTTP   Mock  ← Strategies
```

**Beneficios:**
- ✅ Fácil agregar nuevos providers
- ✅ Sin cambios en código de negocio
- ✅ Configuración declarativa
- ✅ Testing simplificado

---

## 🚀 Agregar un Nuevo Adapter

### Paso 1: Crear la Clase

```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.ai.embeddings.provider", 
    havingValue = "huggingface"
)
public class HuggingFaceEmbeddingsAdapter implements EmbeddingsPort {
    
    @Value("${app.ai.huggingface.api-key}")
    private String apiKey;
    
    @Override
    public float[] embedOne(String text) {
        // Implementación con HuggingFace API
    }
    
    @Override
    public List<float[]> embedMany(List<String> texts) {
        // Implementación batch
    }
    
    @Override
    public String model() {
        return "sentence-transformers/all-MiniLM-L6-v2";
    }
}
```

### Paso 2: Configurar en application.yml

```yaml
app:
  ai:
    embeddings:
      provider: huggingface
    
    huggingface:
      api-key: ${HUGGINGFACE_API_KEY}
```

### Paso 3: ¡Listo!

El nuevo adapter se activará automáticamente sin cambiar nada más.

---

## 📊 Comparación de Adapters

| Característica | OpenAI | HTTP | Mock |
|----------------|--------|------|------|
| Producción | ✅ Recomendado | ⚠️ Depende | ❌ No |
| Desarrollo | ✅ Sí | ✅ Sí | ✅ Ideal |
| Testing | ⚠️ Costoso | ⚠️ Requiere servidor | ✅ Perfecto |
| Validación | ✅ Completa | ⚠️ Básica | ❌ Ninguna |
| Logging | ✅ Detallado | ⚠️ Básico | ⚠️ Básico |
| Costo | 💰 Paga | 🆓 Gratis (si es local) | 🆓 Gratis |

---

## ✅ Checklist de Verificación

Cuando cambies de adapter, verifica:

- [ ] Configuración correcta en `application.yml`
- [ ] Variable de entorno configurada (si es necesaria)
- [ ] Logs de inicio muestran el bean correcto
- [ ] No hay errores de `NoUniqueBeanDefinitionException`
- [ ] Los embeddings se generan correctamente
- [ ] Las dimensiones son las esperadas

---

## 🔗 Referencias

- **Spring @Conditional:** https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html#features.developing-auto-configuration.condition-annotations
- **Strategy Pattern:** https://refactoring.guru/design-patterns/strategy

---

**¡El sistema de selección de adapters es completamente automático y configurable!** 🎉

Solo cambia el valor de `app.ai.embeddings.provider` y Spring Boot se encarga del resto.

