# 📋 Resumen: Estado de Liquibase

## ⚠️ **Situación Actual**

**Liquibase NO se está ejecutando** al iniciar la aplicación.

### Evidencia:
- ✅ Dependencia agregada correctamente en `pom.xml`
- ✅ Configuración correcta en `application.yml`
- ✅ Archivo `db.changelog-master.yaml` existe
- ❌ **NO aparecen logs de Liquibase en el inicio**

---

## 🔍 **Posibles Causas**

### 1. **Las tablas ya existen**
Si las tablas ya fueron creadas (manualmente o por Hibernate con `ddl-auto=create`), Liquibase puede estar silenciosamente marcándolas como ejecutadas.

### 2. **Archivo no encontrado**
El archivo `db.changelog-master.yaml` podría no estar en la ruta correcta o tener un error de sintaxis.

### 3. **Dependencia no cargada**
La dependencia de Liquibase podría no haberse descargado correctamente.

---

## ✅ **Correcciones Aplicadas**

### 1. Logging de DEBUG habilitado

Agregado en `application.yml`:

```yaml
logging:
  level:
    liquibase: DEBUG
    liquibase.lockservice: DEBUG
    liquibase.changelog: DEBUG
```

Esto nos mostrará **exactamente** qué está haciendo (o no haciendo) Liquibase.

---

## 🚀 **Próximos Pasos**

### 1. **Reiniciar la aplicación**

Con el logging de DEBUG, deberías ver MUCHOS más logs de Liquibase, como:

```
DEBUG liquibase.servicelocator : liquibase.hub found
DEBUG liquibase.lockservice : Acquiring database lock
DEBUG liquibase.database : Set default schema name to chatbotia
DEBUG liquibase.changelog : Reading changelog classpath:db/changelog/db.changelog-master.yaml
DEBUG liquibase.changelog : ChangeSet db/migration/V1__create_tables.sql::1::system read
DEBUG liquibase.changelog : ChangeSet db/migration/V2__backend_pgvector.sql::2::system read
DEBUG liquibase.changelog : Checking if table DATABASECHANGELOG exists
DEBUG liquibase.changelog : Table chatbotia.DATABASECHANGELOG already exists
DEBUG liquibase.changelog : Reading from chatbotia.DATABASECHANGELOG
INFO liquibase.changelog : ChangeSet db/migration/V1__create_tables.sql::1::system ran successfully in XXXms
```

### 2. **Si NO ves logs de Liquibase:**

Ejecuta este comando para forzar la recarga de dependencias:

```bash
./mvnw.cmd clean install -DskipTests
```

Luego reinicia.

### 3. **Si ves "Table already exists":**

Significa que las tablas ya están creadas. Opciones:

**Opción A - Sincronizar sin ejecutar (RECOMENDADO para dev):**

Agrega temporalmente en `application.yml`:

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: chatbotia
    liquibase-schema: chatbotia
```

**Opción B - Recrear la BD (solo si puedes perder datos):**

```sql
-- ⚠️ Solo en desarrollo
DROP SCHEMA chatbotia CASCADE;
CREATE SCHEMA chatbotia;
```

Luego reinicia la aplicación.

---

## 📊 **Verificación Manual**

Mientras la aplicación está corriendo, conecta a PostgreSQL y ejecuta:

```sql
-- Ver si las tablas de control de Liquibase existen
SELECT tablename 
FROM pg_tables 
WHERE schemaname = 'chatbotia' 
  AND tablename LIKE 'database%';

-- Deberías ver:
-- databasechangelog
-- databasechangeloglock
```

Si **NO existen**, Liquibase definitivamente no se está ejecutando.

Si **SÍ existen**, ejecuta:

```sql
-- Ver qué changeSets se han ejecutado
SELECT id, author, filename, dateexecuted, exectype
FROM chatbotia.databasechangelog
ORDER BY orderexecuted;
```

---

## 🐛 **Comandos de Diagnóstico**

### Verificar que la dependencia se descargó:

```bash
./mvnw.cmd dependency:tree | findstr liquibase
```

Debería mostrar:
```
[INFO] +- org.liquibase:liquibase-core:jar:4.x.x:compile
```

### Verificar que el archivo changelog existe:

```bash
dir src\main\resources\db\changelog\db.changelog-master.yaml
```

### Verificar la sintaxis del YAML:

Usa un validador online: https://www.yamllint.com/

Copia el contenido de `db.changelog-master.yaml` y valídalo.

---

## 📝 **Checklist de Diagnóstico**

Ejecuta esto paso a paso:

- [ ] Reiniciar la aplicación con logging DEBUG
- [ ] Buscar logs de Liquibase en la consola
- [ ] Verificar que `liquibase-core` esté en las dependencias
- [ ] Verificar que el archivo `db.changelog-master.yaml` exista
- [ ] Conectarse a PostgreSQL y ver si existen las tablas `databasechangelog*`
- [ ] Si las tablas existen, ver qué changeSets están registrados
- [ ] Si no hay logs, ejecutar `mvn clean install`

---

## 🎯 **Acción Inmediata**

**Reinicia la aplicación AHORA** y copia los primeros 30 segundos de logs.

Deberías ver uno de estos escenarios:

### Escenario A: Liquibase funciona
```
INFO liquibase.lockservice : Successfully acquired change log lock
INFO liquibase.changelog : Reading from chatbotia.DATABASECHANGELOG
INFO liquibase.changelog : ChangeSet ... ran successfully
INFO liquibase.lockservice : Successfully released change log lock
```

### Escenario B: Tablas ya existen
```
DEBUG liquibase.changelog : Table chatbotia.DATABASECHANGELOG already exists
DEBUG liquibase.changelog : Reading from chatbotia.DATABASECHANGELOG
DEBUG liquibase.changelog : ChangeSet ... has been run, skipping
```

### Escenario C: Error silencioso
```
DEBUG liquibase.changelog : Error reading changelog: ...
WARN liquibase : Liquibase disabled or failed to initialize
```

### Escenario D: NO aparece nada de Liquibase
→ La dependencia no está cargada o la configuración no se está aplicando.

---

**Reinicia ahora y observa los logs.** 🔍

