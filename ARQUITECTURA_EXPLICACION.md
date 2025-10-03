# 🏗️ Arquitectura Hexagonal - Explicación de Capas

## ❌ PROBLEMA: Violación de Arquitectura

### Lo que estaba mal:

```java
// ❌ INCORRECTO
@RestController
public class KnowledgeBaseController {
    private final KbRepository kbRepository;  // ← Repositorio del DOMINIO
    
    @PostMapping
    public void create() {
        kbRepository.save(...);  // ← Controller haciendo lógica de BD
    }
}
```

**Problemas:**
1. Controller accede directamente al repositorio (Domain)
2. Lógica de negocio en el Controller (viola SRP)
3. Dificulta testing y cambios
4. Rompe la arquitectura hexagonal

---

## ✅ SOLUCIÓN: Arquitectura Correcta

### Flujo de capas:

```
┌─────────────────────────────────────────────────────────────┐
│                  CAPA DE INTERFACES                         │
│                                                              │
│  Controller → Solo coordina HTTP requests/responses         │
│  - Valida entrada                                           │
│  - Llama a Use Cases                                        │
│  - Formatea respuesta                                       │
│  - NO tiene lógica de negocio                              │
└─────────────────────────────────────────────────────────────┘
                          ↓ llama a
┌─────────────────────────────────────────────────────────────┐
│                CAPA DE APLICACIÓN (Use Cases)               │
│                                                              │
│  Use Case → Orquesta la lógica de negocio                  │
│  - Coordina entidades del dominio                           │
│  - Llama a repositorios                                     │
│  - Llama a servicios externos (ports)                       │
│  - Maneja transacciones                                     │
│  - Contiene reglas de negocio                              │
└─────────────────────────────────────────────────────────────┘
                          ↓ usa
┌─────────────────────────────────────────────────────────────┐
│                   CAPA DE DOMINIO                           │
│                                                              │
│  - Entidades (Client, Kb, Message, etc.)                   │
│  - Value Objects (Email, PhoneE164, etc.)                  │
│  - Ports/Interfaces (KbRepository, etc.)                   │
│  - Lógica de negocio pura                                  │
└─────────────────────────────────────────────────────────────┘
                          ↑ implementado por
┌─────────────────────────────────────────────────────────────┐
│              CAPA DE INFRAESTRUCTURA                        │
│                                                              │
│  Adapters → Implementaciones concretas                     │
│  - KbRepositoryJpaAdapter (PostgreSQL)                     │
│  - OpenAIServiceAdapter (OpenAI API)                       │
│  - MetaWhatsAppAdapter (Meta API)                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 Comparación Antes vs Después

### ❌ ANTES (Incorrecto)

```java
@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {
    
    private final KbRepository kbRepository;  // ← Acceso directo al dominio
    
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> request) {
        // ❌ Controller tiene lógica de negocio
        String name = (String) request.get("name");
        UuidId<Client> clientId = UuidId.of(...);
        
        // ❌ Controller crea entidades
        Kb kb = new Kb(UuidId.newId(), clientId, name, "");
        
        // ❌ Controller llama directamente al repositorio
        kbRepository.save(kb);
        
        return ResponseEntity.ok(...);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        // ❌ Controller accede al repositorio
        Optional<Kb> kb = kbRepository.findById(UuidId.of(...));
        return kb.isPresent() ? ResponseEntity.ok(kb) : ResponseEntity.notFound();
    }
}
```

**Problemas:**
- 🚫 Mezcla responsabilidades HTTP con lógica de negocio
- 🚫 Difícil de testear (necesitas BD para test)
- 🚫 No se puede reutilizar lógica en otros contextos
- 🚫 Viola principio de separación de capas

---

### ✅ DESPUÉS (Correcto)

```java
// ═══════════════════════════════════════════════════════════
// CAPA DE INTERFACES
// ═══════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {
    
    // ✅ Solo inyectamos Use Cases
    private final CreateKnowledgeBase createKnowledgeBase;
    private final GetKnowledgeBase getKnowledgeBase;
    
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> request) {
        // ✅ Controller solo coordina
        String name = (String) request.get("name");
        UuidId<Client> clientId = UuidId.of(UUID.fromString(...));
        
        // ✅ Delega al Use Case
        Kb kb = createKnowledgeBase.handle(clientId, name, description);
        
        // ✅ Solo formatea respuesta
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "kbId", kb.id().value().toString()
        ));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        // ✅ Delega al Use Case
        Optional<Kb> kb = getKnowledgeBase.handle(UuidId.of(UUID.fromString(id)));
        
        // ✅ Solo formatea respuesta
        return kb.isPresent() 
            ? ResponseEntity.ok(mapToResponse(kb.get())) 
            : ResponseEntity.notFound().build();
    }
}

// ═══════════════════════════════════════════════════════════
// CAPA DE APLICACIÓN (Use Cases)
// ═══════════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
public class CreateKnowledgeBase {
    
    private final KbRepository kbRepository;  // ✅ Aquí SÍ es correcto
    
    @Transactional
    public Kb handle(UuidId<Client> clientId, String name, String description) {
        // ✅ Validaciones de negocio
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del KB es requerido");
        }
        
        // ✅ Lógica de negocio
        Kb kb = new Kb(UuidId.newId(), clientId, name, description);
        
        // ✅ Persistencia
        kbRepository.save(kb);
        
        log.info("Knowledge Base creado: {}", kb.id());
        
        return kb;
    }
}

@Service
@RequiredArgsConstructor
public class GetKnowledgeBase {
    
    private final KbRepository kbRepository;
    
    public Optional<Kb> handle(UuidId<Kb> kbId) {
        return kbRepository.findById(kbId);
    }
}
```

**Ventajas:**
- ✅ Separación clara de responsabilidades
- ✅ Lógica de negocio reutilizable
- ✅ Fácil de testear (mock del Use Case)
- ✅ Respeta arquitectura hexagonal
- ✅ Cambios en BD no afectan Controller

---

## 🎯 Reglas de Arquitectura

### Controllers (Interfaces) SOLO pueden:
1. ✅ Validar entrada HTTP (formato, tipos)
2. ✅ Llamar Use Cases
3. ✅ Formatear respuestas HTTP
4. ✅ Manejar códigos de estado HTTP

### Controllers (Interfaces) NO pueden:
1. ❌ Acceder directamente a Repositories
2. ❌ Crear entidades de dominio
3. ❌ Contener lógica de negocio
4. ❌ Llamar servicios externos directamente

### Use Cases (Application) SOLO pueden:
1. ✅ Orquestar lógica de negocio
2. ✅ Llamar Repositories (ports del dominio)
3. ✅ Llamar servicios externos (ports)
4. ✅ Coordinar múltiples entidades
5. ✅ Manejar transacciones

### Use Cases (Application) NO pueden:
1. ❌ Manejar HTTP directamente
2. ❌ Depender de frameworks web
3. ❌ Formatear respuestas HTTP

---

## 📊 Ejemplo Completo de Flujo

```
HTTP POST /api/knowledge-base
{
  "clientId": "550e8400-...",
  "name": "FAQ General",
  "description": "Preguntas frecuentes"
}
         ↓
┌────────────────────────────────────────────┐
│  KnowledgeBaseController                   │
│  (Capa de Interfaces)                      │
│                                             │
│  1. Recibe request HTTP                    │
│  2. Extrae parámetros                      │
│  3. Valida formato básico                  │
│  4. Convierte String a UuidId              │
└────────────────────────────────────────────┘
         ↓ llama a
┌────────────────────────────────────────────┐
│  CreateKnowledgeBase.handle(...)           │
│  (Capa de Aplicación - Use Case)           │
│                                             │
│  1. Valida reglas de negocio               │
│  2. Crea entidad Kb                        │
│  3. Llama KbRepository.save()              │
│  4. Log de auditoría                       │
│  5. Retorna Kb creado                      │
└────────────────────────────────────────────┘
         ↓ usa
┌────────────────────────────────────────────┐
│  KbRepository (interface)                  │
│  (Capa de Dominio - Port)                  │
│                                             │
│  save(Kb kb)                               │
└────────────────────────────────────────────┘
         ↓ implementado por
┌────────────────────────────────────────────┐
│  KbRepositoryJpaAdapter                    │
│  (Capa de Infraestructura - Adapter)       │
│                                             │
│  1. Convierte Kb → KbEntity (mapper)       │
│  2. Llama JPA repository                   │
│  3. Persiste en PostgreSQL                 │
└────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────┐
│  PostgreSQL Database                       │
│  chatbotia.kb table                        │
└────────────────────────────────────────────┘
         ↑ respuesta
┌────────────────────────────────────────────┐
│  KnowledgeBaseController                   │
│                                             │
│  1. Recibe Kb del Use Case                │
│  2. Convierte a Map<String, Object>        │
│  3. Retorna ResponseEntity.ok(...)         │
└────────────────────────────────────────────┘
         ↓
HTTP 200 OK
{
  "status": "success",
  "kbId": "7f3e4d5c-...",
  "message": "KB creado"
}
```

---

## 🧪 Testing

### ❌ Antes (Difícil de testear)
```java
@Test
void testCreate() {
    // ❌ Necesitas levantar BD, Spring Context, etc.
    mockMvc.perform(post("/api/knowledge-base")...)
        .andExpect(status().isOk());
}
```

### ✅ Después (Fácil de testear)
```java
// Test del Use Case (sin BD, sin HTTP)
@Test
void testCreateKnowledgeBase() {
    // ✅ Mock simple del repository
    KbRepository mockRepo = mock(KbRepository.class);
    CreateKnowledgeBase useCase = new CreateKnowledgeBase(mockRepo);
    
    // ✅ Test puro de lógica de negocio
    Kb kb = useCase.handle(clientId, "FAQ", "Descripción");
    
    assertNotNull(kb);
    assertEquals("FAQ", kb.name());
    verify(mockRepo).save(any(Kb.class));
}

// Test del Controller (mock del Use Case)
@Test
void testCreateEndpoint() {
    CreateKnowledgeBase mockUseCase = mock(CreateKnowledgeBase.class);
    when(mockUseCase.handle(...)).thenReturn(kb);
    
    // ✅ Test solo de la capa HTTP
    mockMvc.perform(post("/api/knowledge-base")...)
        .andExpect(status().isOk());
}
```

---

## 🎓 Conclusión

### Principios Clave:

1. **Separación de Responsabilidades**
   - Controllers → HTTP
   - Use Cases → Lógica de Negocio
   - Domain → Reglas de Dominio
   - Infrastructure → Implementaciones

2. **Dependency Rule**
   - Interfaces → depende de → Application
   - Application → depende de → Domain
   - Infrastructure → implementa → Domain Ports

3. **Testabilidad**
   - Cada capa se testea independientemente
   - Mock de dependencias fácil

4. **Flexibilidad**
   - Cambiar BD sin tocar Controllers
   - Cambiar Controllers sin tocar Use Cases
   - Agregar nuevos canales (REST, GraphQL, CLI) reutilizando Use Cases

**¡La arquitectura correcta hace el código más mantenible, testeable y escalable! 🚀**

