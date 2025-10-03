# 🔍 Cómo Verificar que Liquibase se Ejecutó Correctamente

## 📋 Problema Detectado

En los logs **NO aparecen mensajes de Liquibase**, lo que indica que no se está ejecutando.

### Causa
La configuración de `liquibase` en `application.yml` estaba mal indentada (estaba dentro de `ai` en lugar de `spring`).

### ✅ Corrección Aplicada

**ANTES (Incorrecto):**
```yaml
ai:
  base-url: http://localhost:8000

  liquibase:  # ❌ Mal indentado
    enabled: true
```

**DESPUÉS (Correcto):**
```yaml
spring:
  liquibase:  # ✅ Correcto
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: chatbotia

ai:
  base-url: http://localhost:8000
```

---

## 🚀 Pasos para Verificar

### 1. Reiniciar la Aplicación

Después de corregir `application.yml`, reinicia la aplicación y busca estos logs:

```
INFO liquibase.database : Set default schema name to chatbotia
INFO liquibase.lockservice : Successfully acquired change log lock
INFO liquibase.changelog : Reading from chatbotia.DATABASECHANGELOG
INFO liquibase.changelog : classpath:db/changelog/db.changelog-master.yaml: ...
INFO liquibase.changelog : ChangeSet db/migration/V1__create_tables.sql::1::system ran successfully in XXXms
INFO liquibase.changelog : ChangeSet db/migration/V2__backend_pgvector.sql::2::system ran successfully in XXXms
INFO liquibase.lockservice : Successfully released change log lock
```

### 2. Verificar Tablas de Control en la Base de Datos

Conéctate a PostgreSQL y ejecuta:

```sql
-- Ver si existen las tablas de control de Liquibase
SELECT tablename 
FROM pg_tables 
WHERE schemaname = 'chatbotia' 
  AND tablename LIKE 'database%';

-- Resultado esperado:
-- databasechangelog
-- databasechangeloglock
```

### 3. Ver los ChangeSets Ejecutados

```sql
SELECT 
    id,
    author,
    filename,
    dateexecuted,
    orderexecuted,
    exectype,
    md5sum
FROM chatbotia.databasechangelog
ORDER BY orderexecuted;

-- Deberías ver:
-- id=1, filename=db/migration/V1__create_tables.sql
-- id=2, filename=db/migration/V2__backend_pgvector.sql
```

### 4. Verificar el Estado del Lock

```sql
SELECT * FROM chatbotia.databasechangeloglock;

-- Debería mostrar:
-- id=1, locked=false, lockgranted=NULL, lockedby=NULL
```

---

## 🐛 Solución de Problemas

### Problema 1: Liquibase no aparece en los logs

**Causa:** Configuración mal indentada o `enabled: false`

**Solución:**
```yaml
spring:
  liquibase:
    enabled: true  # ✅ Verificar que sea true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

### Problema 2: "Table already exists"

**Causa:** Las tablas ya fueron creadas manualmente o por Hibernate

**Solución A - Sincronizar sin ejecutar:**
```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    liquibase-schema: chatbotia
```

Luego ejecutar:
```bash
mvn liquibase:changelogSync
```

**Solución B - Recrear la BD (solo desarrollo):**
```sql
-- ⚠️ Solo en desarrollo
DROP SCHEMA chatbotia CASCADE;
CREATE SCHEMA chatbotia;
```

### Problema 3: "liquibase.lockservice : Waiting for changelog lock"

**Causa:** Liquibase quedó bloqueado por una ejecución anterior interrumpida

**Solución:**
```sql
-- Liberar el lock manualmente
UPDATE chatbotia.databasechangeloglock 
SET locked = FALSE, 
    lockgranted = NULL, 
    lockedby = NULL 
WHERE id = 1;
```

### Problema 4: Archivo changelog no encontrado

**Error:**
```
liquibase.exception.ChangeLogParseException: 
  Error parsing classpath:db/changelog/db.changelog-master.yaml
```

**Solución:** Verificar la estructura de directorios:
```
src/main/resources/
└── db/
    └── changelog/
        └── db.changelog-master.yaml  ← Debe existir aquí
```

---

## 📝 Habilitar Logs Detallados de Liquibase

Para debugging, agregar en `application.yml`:

```yaml
logging:
  level:
    liquibase: DEBUG
    liquibase.changelog: DEBUG
    liquibase.executor: DEBUG
```

---

## ✅ Checklist de Verificación

- [ ] `spring.liquibase.enabled=true` en `application.yml`
- [ ] Configuración correctamente indentada (dentro de `spring`)
- [ ] Archivo `db.changelog-master.yaml` existe en `src/main/resources/db/changelog/`
- [ ] Scripts SQL existen en `src/main/resources/db/migration/`
- [ ] Logs de Liquibase aparecen al iniciar la aplicación
- [ ] Tabla `databasechangelog` existe en el schema `chatbotia`
- [ ] Tabla `databasechangeloglock` existe en el schema `chatbotia`
- [ ] Los 2 changeSets aparecen registrados en `databasechangelog`

---

## 🎯 Comando de Verificación Rápida

Si tienes acceso a psql:

```bash
# Windows (si PostgreSQL está instalado localmente)
psql -h localhost -p 25432 -U postgres -d chatbotia

# Una vez conectado:
\dt chatbotia.database*

# Ver changeSets:
SELECT id, author, filename, dateexecuted 
FROM chatbotia.databasechangelog;
```

Si usas Docker:
```bash
docker exec -it <postgres-container> psql -U postgres -d chatbotia

# Dentro del contenedor:
\dt chatbotia.database*
```

---

## 🚀 Próximos Pasos

1. ✅ **Reinicia la aplicación** después de la corrección del YAML
2. 🔍 **Busca los logs de Liquibase** en la consola
3. ✔️ **Verifica las tablas** `databasechangelog` y `databasechangeloglock`
4. 📊 **Consulta los changeSets ejecutados**

Si todo está correcto, deberías ver en los logs algo como:

```
INFO liquibase.lockservice : Successfully acquired change log lock
INFO liquibase.changelog : Reading from chatbotia.DATABASECHANGELOG
INFO liquibase.changelog : ChangeSet db/migration/V1__create_tables.sql::1::system ran successfully
INFO liquibase.changelog : ChangeSet db/migration/V2__backend_pgvector.sql::2::system ran successfully
INFO liquibase.lockservice : Successfully released change log lock
```

---

**¡Reinicia la aplicación y verifica los logs!** 🚀

