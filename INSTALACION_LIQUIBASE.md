# 🚀 Instalación de Liquibase en el Proyecto

## ✅ Cambios Aplicados

### 1. **Dependencia Maven agregada** (`pom.xml`)

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

Spring Boot gestiona automáticamente la versión compatible.

---

### 2. **Configuración en `application.yml`**

Se reemplazó la configuración de Flyway por Liquibase:

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: chatbotia
```

**Anteriormente (Flyway):**
```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: false
```

---

### 3. **Estructura de Directorios Creada**

```
src/main/resources/
└── db/
    ├── changelog/
    │   └── db.changelog-master.yaml  ← Nuevo archivo maestro de Liquibase
    └── migration/
        ├── V1__create_tables.sql     ← Scripts SQL originales (reutilizados)
        └── V2__backend_pgvector.sql  ← Scripts SQL originales (reutilizados)
```

---

### 4. **Archivo Maestro de Changelog** (`db.changelog-master.yaml`)

```yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: system
      changes:
        - sqlFile:
            path: classpath:db/migration/V1__create_tables.sql
            splitStatements: false
            stripComments: false
      rollback:
        - sql:
            sql: DROP SCHEMA IF EXISTS chatbotia CASCADE;

  - changeSet:
      id: 2
      author: system
      changes:
        - sqlFile:
            path: classpath:db/migration/V2__backend_pgvector.sql
            splitStatements: false
            stripComments: false
      rollback:
        - sql:
            sql: DROP TABLE IF EXISTS public.vector_store CASCADE;
```

**Ventajas:**
- ✅ Reutiliza los scripts SQL existentes
- ✅ Define rollback para cada changeSet
- ✅ Control de versiones más robusto

---

## 🔍 Verificación de la Instalación

### Paso 1: Verificar que Liquibase se ejecute al iniciar

Al iniciar la aplicación, deberías ver en los logs:

```
INFO liquibase.database : Set default schema name to chatbotia
INFO liquibase.lockservice : Successfully acquired change log lock
INFO liquibase.changelog : Reading from chatbotia.DATABASECHANGELOG
INFO liquibase.changelog : classpath:db/changelog/db.changelog-master.yaml: ...
INFO liquibase.lockservice : Successfully released change log lock
```

### Paso 2: Verificar tablas de control de Liquibase

Liquibase crea dos tablas de control:

```sql
-- Tabla que registra los changeSets aplicados
SELECT * FROM chatbotia.databasechangelog;

-- Tabla de bloqueo para ejecución concurrente
SELECT * FROM chatbotia.databasechangeloglock;
```

---

## 📚 Comparación Flyway vs Liquibase

| Característica | Flyway | Liquibase |
|----------------|--------|-----------|
| **Formato** | SQL puro | SQL, YAML, XML, JSON |
| **Versionado** | Basado en nombre de archivo (`V1__`, `V2__`) | Basado en `id` del changeSet |
| **Rollback** | No soportado nativamente | Soportado con `rollback` |
| **Precondiciones** | No | Sí (validaciones antes de ejecutar) |
| **Contextos** | No | Sí (ejecutar según ambiente: dev, prod) |
| **Checksums** | MD5 del archivo | MD5 del changeSet |
| **Cambios pendientes** | `flyway info` | `liquibase status` |

---

## 🎯 Ventajas de Liquibase en este Proyecto

### 1. **Rollback Automático**
```yaml
rollback:
  - sql:
      sql: DROP SCHEMA IF EXISTS chatbotia CASCADE;
```
Puedes deshacer cambios con:
```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### 2. **Precondiciones**
Validar antes de ejecutar:
```yaml
- changeSet:
    id: 3
    author: system
    preConditions:
      - tableExists:
          tableName: client
    changes:
      - addColumn:
          tableName: client
          columns:
            - column:
                name: email
                type: VARCHAR(200)
```

### 3. **Contextos para Ambientes**
```yaml
- changeSet:
    id: 4
    author: system
    context: dev
    changes:
      - insert:
          tableName: client
          columns:
            - column: {name: id, value: uuid_generate_v4()}
            - column: {name: name, value: "Cliente de Prueba"}
```

Ejecutar solo en dev:
```yaml
spring:
  liquibase:
    contexts: dev
```

### 4. **Formato Declarativo**
En lugar de SQL puro, puedes usar YAML:
```yaml
- changeSet:
    id: 5
    author: system
    changes:
      - createTable:
          tableName: audit_log
          columns:
            - column:
                name: id
                type: UUID
                constraints:
                  primaryKey: true
            - column:
                name: action
                type: VARCHAR(100)
            - column:
                name: created_at
                type: TIMESTAMP
                defaultValueComputed: NOW()
```

---

## 🛠️ Comandos Útiles de Liquibase

### Maven Plugin (Opcional)

Puedes agregar el plugin de Liquibase al `pom.xml`:

```xml
<plugin>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-maven-plugin</artifactId>
    <version>4.29.2</version>
    <configuration>
        <propertyFile>src/main/resources/liquibase.properties</propertyFile>
    </configuration>
</plugin>
```

### Comandos disponibles:

```bash
# Ver el estado de las migraciones
mvn liquibase:status

# Generar changelog desde BD existente
mvn liquibase:generateChangeLog

# Ver SQL que se ejecutará sin aplicarlo
mvn liquibase:updateSQL

# Aplicar migraciones manualmente
mvn liquibase:update

# Rollback de N changeSets
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback hasta una fecha específica
mvn liquibase:rollback -Dliquibase.rollbackDate=2025-01-01

# Marcar todos los changeSets como ejecutados (sin ejecutarlos)
mvn liquibase:changelogSync

# Validar el formato del changelog
mvn liquibase:validate
```

---

## 📝 Agregar Nuevas Migraciones

### Opción 1: SQL Directo (Similar a Flyway)

1. Crear nuevo archivo SQL:
```sql
-- V3__add_email_to_client.sql
ALTER TABLE chatbotia.client 
ADD COLUMN IF NOT EXISTS email VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_client_email 
ON chatbotia.client(email);
```

2. Referenciar en `db.changelog-master.yaml`:
```yaml
- changeSet:
    id: 3
    author: tu_nombre
    changes:
      - sqlFile:
          path: classpath:db/migration/V3__add_email_to_client.sql
          splitStatements: true
    rollback:
      - sql:
          sql: ALTER TABLE chatbotia.client DROP COLUMN IF EXISTS email;
```

### Opción 2: YAML Declarativo

Agregar directamente al `db.changelog-master.yaml`:

```yaml
- changeSet:
    id: 3
    author: tu_nombre
    changes:
      - addColumn:
          tableName: client
          schemaName: chatbotia
          columns:
            - column:
                name: email
                type: VARCHAR(200)
                constraints:
                  nullable: true
      - createIndex:
          indexName: idx_client_email
          tableName: client
          schemaName: chatbotia
          columns:
            - column:
                name: email
    rollback:
      - dropIndex:
          indexName: idx_client_email
          schemaName: chatbotia
      - dropColumn:
          tableName: client
          schemaName: chatbotia
          columnName: email
```

---

## 🔒 Tablas de Control de Liquibase

### `databasechangelog`
Registra cada changeSet ejecutado:

```sql
SELECT 
    id,
    author,
    filename,
    dateexecuted,
    orderexecuted,
    exectype,
    md5sum,
    description
FROM chatbotia.databasechangelog
ORDER BY dateexecuted;
```

### `databasechangeloglock`
Evita ejecuciones concurrentes:

```sql
SELECT * FROM chatbotia.databasechangeloglock;
```

Si Liquibase queda bloqueado, liberar manualmente:
```sql
UPDATE chatbotia.databasechangeloglock 
SET locked = FALSE, 
    lockgranted = NULL, 
    lockedby = NULL 
WHERE id = 1;
```

---

## ⚙️ Configuraciones Adicionales Útiles

### En `application.yml`:

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: chatbotia
    # Contextos para ejecutar solo ciertos changeSets
    contexts: dev,common
    # Etiquetas para ejecutar hasta cierta versión
    tag: v1.0.0
    # Validar checksums (detecta cambios manuales en changeSets ejecutados)
    drop-first: false
    # Base de datos limpia en cada inicio (solo para dev!)
    # drop-first: true  # ⚠️ ¡PELIGROSO en producción!
    # Parámetros personalizados
    parameters:
      schema: chatbotia
      admin-user: postgres
```

### Por perfil (dev, prod):

**application-dev.yml:**
```yaml
spring:
  liquibase:
    contexts: dev
    drop-first: true  # OK para desarrollo
```

**application-prod.yml:**
```yaml
spring:
  liquibase:
    contexts: prod
    drop-first: false  # NUNCA en producción
```

---

## 🚨 Migración desde Flyway

Si ya tenías Flyway ejecutado:

### Opción 1: Sincronizar Liquibase con el estado actual

```bash
# Marca todos los changeSets como ejecutados sin ejecutarlos
mvn liquibase:changelogSync
```

### Opción 2: Migración limpia (solo si puedes recrear la BD)

1. Respaldar datos
2. Eliminar tablas de Flyway:
   ```sql
   DROP TABLE IF EXISTS chatbotia.flyway_schema_history;
   ```
3. Iniciar la aplicación (Liquibase ejecutará todo)

### Opción 3: Baseline híbrido

Si tienes datos en producción:

```yaml
- changeSet:
    id: 0-baseline
    author: system
    changes:
      - tagDatabase:
          tag: flyway-baseline
    rollback:
      - sql:
          sql: -- No rollback for baseline
```

Luego ejecutar:
```bash
mvn liquibase:changelogSync -Dliquibase.tag=flyway-baseline
```

---

## ✅ Checklist de Migración

- [x] Dependencia de Liquibase agregada al `pom.xml`
- [x] Configuración de Liquibase en `application.yml`
- [x] Archivo maestro `db.changelog-master.yaml` creado
- [x] Scripts SQL existentes referenciados desde el changelog
- [x] Rollback definido para cada changeSet
- [ ] Probar en entorno de desarrollo
- [ ] Validar que las migraciones se ejecuten correctamente
- [ ] Verificar tablas de control (`databasechangelog`, `databasechangeloglock`)
- [ ] Configurar contextos para diferentes ambientes (opcional)
- [ ] Agregar Maven plugin de Liquibase (opcional)

---

## 🎓 Recursos Adicionales

- **Documentación oficial:** https://docs.liquibase.com/
- **Integración con Spring Boot:** https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.liquibase
- **Changelog formats:** https://docs.liquibase.com/concepts/changelogs/home.html
- **Best practices:** https://www.liquibase.org/get-started/best-practices

---

## 🎯 Próximos Pasos Recomendados

1. **Iniciar la aplicación** y verificar que Liquibase ejecute sin errores
2. **Revisar los logs** para confirmar que ambos changeSets se aplicaron
3. **Consultar `databasechangelog`** para ver el estado
4. **Probar agregar una nueva migración** (ejemplo: agregar columna)
5. **Configurar perfiles** (dev, prod) si es necesario

---

**¡Liquibase instalado y configurado correctamente!** 🚀

