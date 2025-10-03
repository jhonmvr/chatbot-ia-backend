# 🧪 Pruebas: Cliente y Knowledge Base

## ✅ Cambios Aplicados

Se corrigió `ClientRepositoryAdapter` para establecer todos los campos requeridos:
- ✅ `id`
- ✅ `name`
- ✅ `taxId` (guarda el `code` del dominio)
- ✅ `status`
- ✅ `timezone` (default: "America/Guayaquil")
- ✅ `metadata` (objeto vacío)
- ✅ `createdAt` ⬅️ **Agregado**
- ✅ `updatedAt` ⬅️ **Agregado**

---

## 🚀 Pruebas a Realizar

### 1️⃣ Crear Cliente

```bash
curl -X 'POST' \
  'http://localhost:8080/api/clients' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "code": "CLI-001",
  "name": "Empresa Demo S.A.",
  "status": "ACTIVE"
}'
```

**Respuesta Esperada:**
```json
{
  "status": "success",
  "clientId": "123e4567-e89b-12d3-a456-426614174000",
  "code": "CLI-001",
  "message": "Cliente creado exitosamente"
}
```

---

### 2️⃣ Crear Knowledge Base

**Nota:** Reemplaza `CLIENT_ID` con el ID obtenido en el paso anterior.

```bash
curl -X 'POST' \
  'http://localhost:8080/api/knowledge-base' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "clientId": "CLIENT_ID",
  "name": "Productos y Servicios",
  "description": "Base de conocimiento sobre nuestros productos"
}'
```

**Respuesta Esperada:**
```json
{
  "status": "success",
  "kbId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "KB creado"
}
```

---

### 3️⃣ Obtener Cliente por ID

```bash
curl -X 'GET' \
  'http://localhost:8080/api/clients/CLIENT_ID' \
  -H 'accept: application/json'
```

---

### 4️⃣ Obtener Cliente por Código

```bash
curl -X 'GET' \
  'http://localhost:8080/api/clients/by-code/CLI-001' \
  -H 'accept: application/json'
```

---

### 5️⃣ Obtener Knowledge Base

```bash
curl -X 'GET' \
  'http://localhost:8080/api/knowledge-base/KB_ID' \
  -H 'accept: application/json'
```

---

## 📝 Mapeo de Campos

| Campo Dominio (`Client`) | Campo Entidad (`ClientEntity`) | Notas |
|--------------------------|--------------------------------|-------|
| `id` | `id` | UUID |
| `code` | `taxId` | Se guarda en `tax_id` |
| `name` | `name` | Nombre de la empresa |
| `status` | `status` | ACTIVE/INACTIVE |
| - | `timezone` | Default: "America/Guayaquil" |
| - | `metadata` | Objeto JSON vacío por defecto |
| - | `createdAt` | Timestamp automático |
| - | `updatedAt` | Timestamp automático |

---

## 🔍 Verificar en Base de Datos

```sql
-- Ver clientes creados
SELECT id, name, tax_id, status, created_at 
FROM chatbotia.client;

-- Ver knowledge bases
SELECT id, client_id, name, description, created_at 
FROM chatbotia.kb;
```

---

## ✅ Flujo Completo de Prueba

1. **Crear Cliente** → Obtiene `clientId`
2. **Verificar Cliente** → GET por ID o código
3. **Crear KB** → Usa el `clientId`
4. **Verificar KB** → GET por ID
5. **Ingestar Documentos** → POST a `/api/knowledge-base/{kbId}/ingest`
6. **Buscar en KB** → POST a `/api/knowledge-base/{kbId}/search`

---

**¡Ahora todos los endpoints deberían funcionar correctamente!** 🎉

