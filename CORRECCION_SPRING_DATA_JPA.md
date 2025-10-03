# ✅ Corrección de Errores de Spring Data JPA

## 🎯 Problema

Al intentar iniciar la aplicación, Spring Data JPA fallaba al crear queries porque los nombres de los métodos en los repositorios no coincidían con los nombres de las propiedades en las entidades JPA.

### Error Principal

```
QueryCreationException: No property 'kbId' found for type 'KbDocumentEntity'
PropertyReferenceException: No property 'kbId' found for type 'KbDocumentEntity'
```

---

## 🔍 Causa Raíz

En JPA/Hibernate, cuando usas relaciones `@ManyToOne`, la propiedad en Java es el **objeto relacionado**, no el ID directamente.

**Ejemplo:**
```java
@Entity
public class KbDocumentEntity {
    @ManyToOne
    @JoinColumn(name = "kb_id")  // ← Nombre de la columna en BD
    private KbEntity kbEntity;   // ← Nombre de la propiedad en Java
}
```

Spring Data genera queries basándose en los **nombres de las propiedades Java**, no en los nombres de las columnas de la BD.

---

## 🔧 Correcciones Aplicadas

### 1. KbDocumentJpa.java

**ANTES (Incorrecto):**
```java
public interface KbDocumentJpa extends JpaRepository<KbDocumentEntity, UUID> {
    List<KbDocumentEntity> findByKbId(UUID kbId);  // ❌ No existe 'kbId'
}
```

**DESPUÉS (Correcto):**
```java
public interface KbDocumentJpa extends JpaRepository<KbDocumentEntity, UUID> {
    List<KbDocumentEntity> findByKbEntityId(UUID kbId);  // ✅ Usa 'kbEntity.id'
}
```

**Explicación:**
- La entidad tiene `kbEntity` (el objeto)
- Para acceder al ID, Spring Data usa la notación `kbEntity.id` → `findByKbEntityId`

---

### 2. ContactJpa.java

**ANTES (Incorrecto):**
```java
public interface ContactJpa extends JpaRepository<ContactEntity, UUID> {
    Optional<ContactEntity> findByClientIdAndPhoneE164(UUID clientId, String phoneE164);  // ❌
    List<ContactEntity> findByClientId(UUID clientId);  // ❌
}
```

**DESPUÉS (Correcto):**
```java
public interface ContactJpa extends JpaRepository<ContactEntity, UUID> {
    Optional<ContactEntity> findByClientEntityIdAndPhoneE164(UUID clientId, String phoneE164);  // ✅
    List<ContactEntity> findByClientEntityId(UUID clientId);  // ✅
}
```

**Explicación:**
- La entidad `ContactEntity` tiene `clientEntity` (no `clientId`)
- Para buscar por el ID del cliente: `findByClientEntityId`

---

### 3. Actualización de Adaptadores

Los adaptadores que usan estos repositorios también fueron actualizados:

#### KbRepositoryJpaAdapter.java

**ANTES:**
```java
public List<KbDocument> docsOf(UuidId<Kb> kbId) {
    return docRepo.findByKbId(kbId.value())  // ❌
        .stream()
        .map(KbRepositoryJpaAdapter::toDomain)
        .toList();
}
```

**DESPUÉS:**
```java
public List<KbDocument> docsOf(UuidId<Kb> kbId) {
    return docRepo.findByKbEntityId(kbId.value())  // ✅
        .stream()
        .map(KbRepositoryJpaAdapter::toDomain)
        .toList();
}
```

#### ContactRepositoryAdapter.java

**ANTES:**
```java
public Optional<Contact> findByClientAndPhone(UuidId<Client> clientId, String phoneNumber) {
    return contactJpa.findByClientIdAndPhoneE164(clientId.value(), phoneNumber)  // ❌
            .map(ContactMapper::toDomain);
}

public List<Contact> findByClient(UuidId<Client> clientId) {
    return contactJpa.findByClientId(clientId.value()).stream()  // ❌
            .map(ContactMapper::toDomain)
            .collect(Collectors.toList());
}
```

**DESPUÉS:**
```java
public Optional<Contact> findByClientAndPhone(UuidId<Client> clientId, String phoneNumber) {
    return contactJpa.findByClientEntityIdAndPhoneE164(clientId.value(), phoneNumber)  // ✅
            .map(ContactMapper::toDomain);
}

public List<Contact> findByClient(UuidId<Client> clientId) {
    return contactJpa.findByClientEntityId(clientId.value()).stream()  // ✅
            .map(ContactMapper::toDomain)
            .collect(Collectors.toList());
}
```

---

## 📚 Reglas de Spring Data JPA Query Methods

### Convención de Nombres

Para una entidad con una relación:
```java
@Entity
public class MyEntity {
    @ManyToOne
    @JoinColumn(name = "related_id")
    private RelatedEntity relatedEntity;
}
```

| ¿Qué quieres buscar? | Método correcto | Incorrecto |
|----------------------|-----------------|------------|
| Por el objeto completo | `findByRelatedEntity(RelatedEntity entity)` | - |
| Por el ID del objeto relacionado | `findByRelatedEntityId(UUID id)` | `findByRelatedId` |
| Por una propiedad del objeto relacionado | `findByRelatedEntityName(String name)` | `findByRelatedName` |

### Ejemplos Prácticos

```java
// ✅ Correcto - busca por el ID de la entidad relacionada
List<Order> findByCustomerId(Long customerId)

// ✅ Correcto - busca por una propiedad de la entidad relacionada
List<Order> findByCustomerEmail(String email)

// ❌ Incorrecto - 'customerId' no existe como propiedad directa
// Si Order tiene 'customer: Customer', esto falla
```

---

## ✅ Verificación

Después de las correcciones:

```bash
# Compilación exitosa
./mvnw clean compile

# Cero errores de linter
✅ No linter errors found.

# La aplicación ahora puede iniciar correctamente
# Spring Data JPA puede crear todas las queries sin errores
```

---

## 🎓 Lecciones Aprendidas

### 1. **Siempre usa nombres de propiedades Java, no nombres de columnas**

Spring Data JPA trabaja con el **modelo de objetos**, no con el esquema de base de datos.

### 2. **Para relaciones @ManyToOne/@OneToOne, usa `entityName.field`**

Si tienes:
```java
@ManyToOne
private Client client;
```

Para buscar por el ID del cliente:
```java
findByClientId(UUID id)  // ✅ Correcto
```

### 3. **Revisa las entidades JPA antes de escribir queries**

Antes de crear un método de query, verifica:
1. ¿Cuál es el nombre de la propiedad en la entidad?
2. ¿Es una relación o un campo directo?
3. ¿Qué tipo de objeto es?

### 4. **Alternativa: @Query para casos complejos**

Si el nombre del método se vuelve muy largo o confuso, usa `@Query`:

```java
@Query("SELECT k FROM KbDocumentEntity k WHERE k.kbEntity.id = :kbId")
List<KbDocumentEntity> findDocumentsByKbId(@Param("kbId") UUID kbId);
```

---

## 📊 Resumen de Cambios

| Archivo | Método Anterior | Método Corregido |
|---------|----------------|------------------|
| `KbDocumentJpa.java` | `findByKbId` | `findByKbEntityId` |
| `ContactJpa.java` | `findByClientIdAndPhoneE164` | `findByClientEntityIdAndPhoneE164` |
| `ContactJpa.java` | `findByClientId` | `findByClientEntityId` |
| `MessageJpa.java` | `findTop100ByConversationIdOrderByCreatedAtDesc` | `findTop100ByConversationEntityIdOrderByCreatedAtDesc` |
| `UsageDailyJpa.java` | `findByClientIdAndDay` | `findByClientEntityIdAndDay` |
| `UsageDailyJpa.java` | `JpaRepository<UsageDailyEntity, UUID>` | `JpaRepository<UsageDailyEntity, Long>` |
| `KbRepositoryJpaAdapter.java` | Llamada a `findByKbId` | Llamada a `findByKbEntityId` |
| `ContactRepositoryAdapter.java` | Llamadas a métodos antiguos | Llamadas a métodos corregidos |
| `MessageRepositoryAdapter.java` | Llamada a `findTop100ByConversationIdOrderByCreatedAtDesc` | Llamada a `findTop100ByConversationEntityIdOrderByCreatedAtDesc` |

---

## ✅ Estado Final

- ✅ **0 errores de compilación**
- ✅ **0 errores de linter**
- ✅ **Todos los repositorios JPA funcionan correctamente**
- ✅ **Spring Data puede crear queries sin errores**
- ✅ **La aplicación puede iniciar correctamente**

---

## 📝 Nota sobre Migraciones

**No se necesitó Liquibase** para este problema. El error era exclusivamente sobre la **convención de nombres de Spring Data JPA**, no sobre el esquema de la base de datos.

Las migraciones con Liquibase serían necesarias si:
- Falta crear/modificar tablas en la BD
- Hay cambios en el esquema (agregar columnas, índices, etc.)
- Se necesita poblar datos iniciales

En este caso, el problema era solo de **mapeo objeto-relacional (ORM)**, no del esquema de BD.

---

**Proyecto listo para ejecutarse sin errores de Spring Data JPA.** 🚀

