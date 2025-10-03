# 🔍 Revisión de Mappers - Campos Requeridos

## ❌ Problemas Encontrados

### 1. **ConversationRepositoryAdapter** ⚠️ CRÍTICO
**Archivo:** `ConversationRepositoryAdapter.java`

**Campos faltantes en `save()`:**
- ✅ `id` - OK
- ✅ `clientEntity` - OK
- ✅ `contactEntity` - OK (opcional)
- ✅ `phone` - OK (opcional)
- ✅ `channel` - OK
- ✅ `title` - OK
- ✅ `status` - OK
- ✅ `startedAt` - OK
- ✅ `closedAt` - OK (opcional)
- ❌ **`createdAt`** - **FALTA** (nullable = false)
- ❌ **`updatedAt`** - **FALTA** (nullable = false)

**Impacto:** Error al crear conversaciones.

---

### 2. **MessageRepositoryAdapter** ⚠️ CRÍTICO
**Archivo:** `MessageRepositoryAdapter.java`

**Campos faltantes en `save()`:**
- ✅ `id` - OK
- ✅ `clientEntity` - OK
- ✅ `conversationEntity` - OK
- ✅ `contactEntity` - OK (nullable = false en DB, pero opcional en domain)
- ✅ `phone` - OK (opcional)
- ✅ `direction` - OK
- ✅ `channel` - OK
- ✅ `body` - OK
- ✅ `createdAt` - OK
- ❌ **`messageType`** - **FALTA** (nullable = false, default='TEXT')
- ❌ **`media`** - **FALTA** (nullable = false, default='{}'::jsonb)
- ❌ **`status`** - **FALTA** (nullable = false)

**Impacto:** Error al crear mensajes.

---

### 3. **ContactMapper/ContactRepositoryAdapter** ⚠️ REVISAR
**Archivo:** `ContactMapper.java`

**Campos a verificar:**
- `attributes` (nullable = false, default='{}'::jsonb)
- `createdAt` (nullable = false)
- `updatedAt` (nullable = false)

**Estado:** Delega al mapper, necesita revisión.

---

### 4. **OutboundQueueRepositoryAdapter** ⚠️ CRÍTICO
**Archivo:** `OutboundQueueRepositoryAdapter.java`

**Campos faltantes en `save()`:**
- ✅ `id` - OK (opcional en domain)
- ❌ **`clientEntity`** - **FALTA** (nullable = false)
- ❌ **`contactEntity`** - No se guarda (debería?)
- ❌ **`conversationEntity`** - No se guarda (debería?)
- ❌ **`phone`** - No se guarda (debería?)
- ✅ `channel` - OK
- ✅ `status` - OK
- ✅ `body` - OK
- ✅ `retries` - OK
- ✅ `scheduleAt` - OK
- ✅ `lastError` - OK
- ❌ **`media`** - **FALTA** (nullable = false, default='{}'::jsonb)
- ❌ **`createdAt`** - **FALTA** (nullable = false)
- ❌ **`updatedAt`** - **FALTA** (nullable = false)

**Impacto:** Error al encolar mensajes salientes.

---

### 5. **SubscriptionRepositoryAdapter** ⚠️ CRÍTICO
**Archivo:** `SubscriptionRepositoryAdapter.java`

**Campos faltantes en `save()`:**
- ✅ `id` - OK
- ✅ `clientEntity` - OK
- ❌ **`planEntity`** - **FALTA** (nullable = false) - Comentado en código
- ✅ `startDate` - OK
- ✅ `status` - OK
- ❌ **`createdAt`** - **FALTA** (nullable = false)
- ❌ **`updatedAt`** - **FALTA** (nullable = false)

**Impacto:** Error al crear suscripciones.

---

### 6. **KbRepositoryJpaAdapter** ✅ CORREGIDO
**Estado:** Ya se corrigió en revisión anterior.

---

### 7. **ClientRepositoryAdapter** ✅ CORREGIDO
**Estado:** Ya se corrigió en revisión anterior.

---

## 🔧 Correcciones Necesarias

### Prioridad 1: Crítico (Bloquean funcionalidad)

1. **ConversationRepositoryAdapter** - Agregar `createdAt` y `updatedAt`
2. **MessageRepositoryAdapter** - Agregar `messageType`, `media`, `status`
3. **OutboundQueueRepositoryAdapter** - Agregar todos los campos faltantes
4. **SubscriptionRepositoryAdapter** - Agregar `planEntity`, `createdAt`, `updatedAt`

### Prioridad 2: Revisar

5. **ContactMapper** - Verificar campos requeridos

---

## 📋 Patrón de Corrección

Todos los adapters deben seguir este patrón para campos `nullable = false`:

```java
@Override
public void save(DomainObject d) {
    Entity e = new Entity();
    
    // 1. ID
    e.setId(d.id().value());
    
    // 2. Relaciones requeridas
    e.setRelatedEntity(em.getReference(RelatedEntity.class, d.relatedId().value()));
    
    // 3. Campos simples requeridos
    e.setRequiredField(d.requiredField());
    
    // 4. Campos con defaults
    e.setFieldWithDefault(d.field() != null ? d.field() : "DEFAULT_VALUE");
    
    // 5. Campos JSONB requeridos
    e.setMetadata(new HashMap<>()); // o d.metadata() si existe
    
    // 6. Timestamps requeridos
    e.setCreatedAt(OffsetDateTime.now());
    e.setUpdatedAt(OffsetDateTime.now());
    
    // 7. Campos opcionales
    d.optionalField().ifPresent(e::setOptionalField);
    
    repo.save(e);
}
```

---

## 🎯 Campos Críticos por Tipo

### Timestamps (casi siempre requeridos)
- `createdAt` - nullable = false
- `updatedAt` - nullable = false

### JSONB (casi siempre requeridos con default)
- `metadata` - nullable = false, default = '{}'::jsonb
- `attributes` - nullable = false, default = '{}'::jsonb
- `media` - nullable = false, default = '{}'::jsonb

### Relaciones
- Verificar si son `nullable = false` en la entidad
- Usar `em.getReference()` para lazy loading

### Enums con Defaults
- `status` - Verificar default en @ColumnDefault
- `channel` - Verificar default en @ColumnDefault
- `messageType` - Verificar default en @ColumnDefault

---

## ✅ Checklist de Validación

Para cada adapter, verificar:

- [ ] Todos los campos `nullable = false` están siendo seteados
- [ ] Los campos con `@ColumnDefault` se setean con el default si no hay valor
- [ ] `createdAt` y `updatedAt` se setean con `OffsetDateTime.now()`
- [ ] Los campos JSONB requeridos se inicializan con `{}` o valor apropiado
- [ ] Las relaciones `@ManyToOne(optional = false)` se establecen
- [ ] Los campos opcionales usan `.ifPresent()` o validación de null
- [ ] No se dejan campos requeridos sin valor

---

**Siguiente paso:** Aplicar correcciones en orden de prioridad.

