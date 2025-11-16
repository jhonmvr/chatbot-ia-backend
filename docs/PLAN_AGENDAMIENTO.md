# Plan de Implementación: Sistema de Agendamiento con Integración de Calendarios

## 📋 Resumen Ejecutivo

Este documento describe el plan completo para implementar un sistema de agendamiento conversacional que permite a los usuarios reservar citas a través de WhatsApp, con integración dinámica a múltiples proveedores de calendario (Google Calendar, Outlook, etc.).

## 🎯 Objetivos

1. Permitir a los usuarios agendar citas mediante conversación natural en WhatsApp
2. Integrar con múltiples proveedores de calendario (Google Calendar, Outlook)
3. Configurar dinámicamente el proveedor de calendario por cliente
4. Validar disponibilidad en tiempo real
5. Sincronizar agendamientos con calendarios externos

## 🔄 Flujo de Usuario

```
Usuario: "quiero hacer un agendamiento"
    ↓
Agente IA: Consulta calendario → Muestra horarios disponibles de la semana
    ↓
Usuario: "quiero el día viernes a las 8 am"
    ↓
Agente IA: Valida disponibilidad → Crea agendamiento → Confirma
    ↓
Respuesta: "¡Gracias por tu agendamiento! Se agregó el día viernes a las 8 am"
```

## 📐 Arquitectura

### Componentes Principales

1. **Detección de Intención**: Identifica cuando el usuario quiere agendar
2. **Servicio de Disponibilidad**: Consulta horarios disponibles del calendario
3. **Parser de Fecha/Hora**: Extrae fecha y hora del mensaje natural
4. **Validación**: Verifica que el horario esté disponible
5. **Creación de Agendamiento**: Crea la cita y sincroniza con calendario externo
6. **Configuración Dinámica**: Gestiona proveedores de calendario por cliente

## 📦 Fase 1: Modelo de Dominio y Persistencia

### 1.1 Entidades de Dominio

#### Appointment (Cita)
```java
- id: UuidId<Appointment>
- clientId: UuidId<Client>
- contactId: UuidId<Contact>
- title: String
- description: String
- startTime: LocalDateTime
- endTime: LocalDateTime
- timezone: String
- location: String (opcional)
- status: AppointmentStatus
- externalCalendarId: String (opcional)
- externalEventId: String (opcional)
- provider: CalendarProvider (opcional)
- createdAt: Instant
- updatedAt: Instant
```

#### Enums
- `AppointmentStatus`: PENDING, CONFIRMED, CANCELLED, COMPLETED, RESCHEDULED
- `CalendarProvider`: GOOGLE_CALENDAR, OUTLOOK, NONE

### 1.2 Repositorio

```java
// domain/ports/scheduling/AppointmentRepository.java
- save(Appointment appointment)
- findById(UuidId<Appointment> id)
- findByClient(UuidId<Client> clientId)
- findByContact(UuidId<Contact> contactId)
- findByDateRange(LocalDateTime start, LocalDateTime end)
- findByStatus(AppointmentStatus status)
```

### 1.3 Migración de Base de Datos

```sql
-- V2__create_appointment_tables.sql
CREATE TABLE appointment (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    contact_id UUID NOT NULL REFERENCES contact(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/Guayaquil',
    location VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_calendar_id VARCHAR(200),
    external_event_id VARCHAR(200),
    provider VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appointment_client ON appointment(client_id);
CREATE INDEX idx_appointment_contact ON appointment(contact_id);
CREATE INDEX idx_appointment_start_time ON appointment(start_time);
CREATE INDEX idx_appointment_status ON appointment(status);
CREATE INDEX idx_appointment_date_range ON appointment(start_time, end_time);
```

### 1.4 Configuración de Calendario en Cliente

Agregar campos en la tabla `client` o usar `metadata` JSONB:

```sql
-- Opción 1: Campos directos (recomendado)
ALTER TABLE client ADD COLUMN calendar_provider VARCHAR(20);
ALTER TABLE client ADD COLUMN calendar_id VARCHAR(200);
ALTER TABLE client ADD COLUMN calendar_timezone VARCHAR(50) DEFAULT 'America/Guayaquil';

-- Opción 2: Usar metadata existente (ya implementado)
-- Se almacena en client.metadata JSONB:
-- {
--   "calendarProvider": "GOOGLE_CALENDAR",
--   "calendarId": "primary",
--   "calendarTimezone": "America/Guayaquil"
-- }
```

## 📦 Fase 2: Puertos y Adaptadores de Calendario

### 2.1 Puerto de Calendario

```java
// application/ports/out/CalendarService.java
public interface CalendarService {
    String createEvent(Appointment appointment, String calendarId);
    String updateEvent(Appointment appointment, String externalEventId);
    void deleteEvent(String externalEventId);
    List<TimeSlot> getAvailableSlots(LocalDateTime start, LocalDateTime end);
    boolean hasConflict(LocalDateTime start, LocalDateTime end);
    CalendarProvider getProvider();
}
```

### 2.2 Adaptador Google Calendar

```java
// infrastructure/adapters/out/calendar/GoogleCalendarAdapter.java
- Implementa CalendarService
- Usa Google Calendar API v3
- Autenticación con Service Account o OAuth2
- Configuración desde application.yml
```

**Dependencias Maven:**
```xml
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-calendar</artifactId>
    <version>v3-rev20231130-2.0.0</version>
</dependency>
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.19.0</version>
</dependency>
```

### 2.3 Adaptador Outlook/Microsoft Graph

```java
// infrastructure/adapters/out/calendar/OutlookCalendarAdapter.java
- Implementa CalendarService
- Usa Microsoft Graph API
- Autenticación con Client Credentials
- Configuración desde application.yml
```

**Dependencias Maven:**
```xml
<dependency>
    <groupId>com.microsoft.graph</groupId>
    <artifactId>microsoft-graph</artifactId>
    <version>5.45.0</version>
</dependency>
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.10.0</version>
</dependency>
```

### 2.4 Router de Calendarios

```java
// infrastructure/adapters/out/calendar/CalendarServiceRouter.java
- Mapea CalendarProvider → CalendarService
- Inyecta todos los adaptadores disponibles
- Permite selección dinámica del proveedor
```

## 📦 Fase 3: Servicios de Aplicación

### 3.1 Servicio de Detección de Intención

```java
// application/services/AppointmentIntentService.java
- isAppointmentIntent(String message): boolean
- isDateTimeSelection(String message): boolean
- extractDateTime(String message): DateTimeSelection
```

**Patrones de detección:**
- Intención: "agendar", "agendamiento", "reservar", "cita", "turno"
- Fecha/Hora: "viernes a las 8 am", "mañana a las 2pm", etc.

### 3.2 Servicio de Disponibilidad

```java
// application/services/AppointmentAvailabilityService.java
- getAvailableSlotsForWeek(...): String (formateado para usuario)
- isSlotAvailable(...): boolean
- generateDefaultSlots(...): List<TimeSlot>
- formatSlotsForUser(...): String
```

**Características:**
- Consulta calendario externo para obtener disponibilidad real
- Genera slots por defecto si no hay calendario configurado
- Formatea horarios de forma amigable para WhatsApp
- Considera horarios laborables (configurables)

### 3.3 Servicio de Parser de Fecha/Hora

```java
// application/services/DateTimeParserService.java
- parseDateTime(String message): LocalDateTime
- parseDate(String message): LocalDate
- parseTime(String message): LocalTime
```

**Soporta:**
- Días de la semana: "lunes", "martes", "viernes", etc.
- Referencias temporales: "hoy", "mañana", "pasado mañana"
- Horas: "8 am", "2pm", "14:00", etc.

### 3.4 Servicio de Configuración de Cliente

```java
// application/usecases/GetClientCalendarConfig.java
- handle(UuidId<Client> clientId): ClientCalendarConfig
- Lee configuración desde client.metadata o campos directos
- Retorna: provider, calendarId, timezone
```

## 📦 Fase 4: Casos de Uso

### 4.1 CreateAppointment

```java
// application/usecases/CreateAppointment.java
Flujo:
1. Validar parámetros de entrada
2. Crear entidad Appointment en dominio
3. Si hay proveedor configurado:
   a. Validar disponibilidad
   b. Crear evento en calendario externo
   c. Vincular appointment con evento externo
4. Guardar appointment en repositorio
5. Retornar appointment creado
```

### 4.2 GetAvailableSlots

```java
// application/usecases/GetAvailableSlots.java
Flujo:
1. Obtener configuración de calendario del cliente
2. Consultar disponibilidad del calendario externo
3. Generar slots disponibles (o usar defaults)
4. Formatear para mostrar al usuario
5. Retornar mensaje formateado
```

### 4.3 CancelAppointment

```java
// application/usecases/CancelAppointment.java
Flujo:
1. Buscar appointment por ID
2. Validar que puede ser cancelado
3. Si tiene evento externo, eliminarlo del calendario
4. Actualizar status a CANCELLED
5. Guardar cambios
```

### 4.4 RescheduleAppointment

```java
// application/usecases/RescheduleAppointment.java
Flujo:
1. Buscar appointment existente
2. Validar nueva fecha/hora disponible
3. Actualizar appointment
4. Si tiene evento externo, actualizarlo en calendario
5. Guardar cambios
```

## 📦 Fase 5: Integración con ProcessMessageWithAI

### 5.1 Modificar ProcessMessageWithAI

```java
// Modificar: application/usecases/ProcessMessageWithAI.java

Cambios:
1. Agregar dependencias:
   - AppointmentIntentService
   - AppointmentAvailabilityService
   - DateTimeParserService
   - CreateAppointment
   - GetClientCalendarConfig

2. Modificar método handle():
   - Agregar parámetros: clientId, contactId
   - Interceptar intención de agendamiento
   - Interceptar selección de fecha/hora
   - Flujo normal si no es agendamiento

3. Nuevos métodos privados:
   - handleAppointmentIntent()
   - handleDateTimeSelection()
```

### 5.2 Actualizar ReceiveWhatsAppMessage

```java
// Modificar: application/usecases/ReceiveWhatsAppMessage.java

Cambio en llamada a ProcessMessageWithAI:
- Agregar client.id() y contact.id() como parámetros
```

## 📦 Fase 6: Configuración y Propiedades

### 6.1 application.yml

```yaml
# Configuración de Google Calendar
google:
  calendar:
    enabled: ${GOOGLE_CALENDAR_ENABLED:false}
    credentials-path: ${GOOGLE_CALENDAR_CREDENTIALS_PATH:/credentials.json}
    default-calendar-id: ${GOOGLE_CALENDAR_ID:primary}
    work-hours:
      start: "08:00"
      end: "18:00"
    slot-duration-minutes: 30

# Configuración de Outlook
outlook:
  enabled: ${OUTLOOK_ENABLED:false}
  tenant-id: ${OUTLOOK_TENANT_ID:}
  client-id: ${OUTLOOK_CLIENT_ID:}
  client-secret: ${OUTLOOK_CLIENT_SECRET:}
  default-calendar-id: ${OUTLOOK_CALENDAR_ID:calendar}
  work-hours:
    start: "08:00"
    end: "18:00"
  slot-duration-minutes: 30

# Configuración general de agendamiento
appointment:
  default-duration-minutes: 30
  default-timezone: "America/Guayaquil"
  max-days-ahead: 30
```

### 6.2 Variables de Entorno

```bash
# Google Calendar
GOOGLE_CALENDAR_ENABLED=true
GOOGLE_CALENDAR_CREDENTIALS_PATH=/path/to/credentials.json
GOOGLE_CALENDAR_ID=primary

# Outlook
OUTLOOK_ENABLED=true
OUTLOOK_TENANT_ID=your-tenant-id
OUTLOOK_CLIENT_ID=your-client-id
OUTLOOK_CLIENT_SECRET=your-client-secret
OUTLOOK_CALENDAR_ID=calendar
```

## 📦 Fase 7: Configuración Dinámica por Cliente

### 7.1 Estructura de Metadata

```json
{
  "calendarProvider": "GOOGLE_CALENDAR",
  "calendarId": "primary",
  "calendarTimezone": "America/Guayaquil",
  "workHours": {
    "start": "08:00",
    "end": "18:00"
  },
  "slotDurationMinutes": 30,
  "appointmentDurationMinutes": 30
}
```

### 7.2 API para Configurar Calendario

```java
// interfaces/web/CalendarConfigController.java
@RestController
@RequestMapping("/api/clients/{clientId}/calendar-config")
public class CalendarConfigController {
    
    @PostMapping
    public ResponseEntity<?> configureCalendar(
        @PathVariable UUID clientId,
        @RequestBody CalendarConfigRequest request
    ) {
        // Actualizar metadata del cliente con configuración de calendario
    }
    
    @GetMapping
    public ResponseEntity<CalendarConfigResponse> getConfig(
        @PathVariable UUID clientId
    ) {
        // Retornar configuración actual del cliente
    }
}
```

### 7.3 Caso de Uso: UpdateClientCalendarConfig

```java
// application/usecases/UpdateClientCalendarConfig.java
Flujo:
1. Buscar cliente por ID
2. Validar proveedor de calendario
3. Validar credenciales (opcional, según proveedor)
4. Actualizar metadata del cliente
5. Guardar cambios
```

## 📋 Checklist de Implementación

### Fase 1: Modelo de Dominio
- [ ] Crear entidad `Appointment` en dominio
- [ ] Crear enums `AppointmentStatus` y `CalendarProvider`
- [ ] Crear interfaz `AppointmentRepository`
- [ ] Crear migración SQL para tabla `appointment`
- [ ] Implementar `AppointmentRepositoryAdapter` con JPA
- [ ] Crear entidad JPA `AppointmentEntity`
- [ ] Crear mapper `AppointmentMapper`

### Fase 2: Puertos y Adaptadores
- [ ] Crear puerto `CalendarService`
- [ ] Implementar `GoogleCalendarAdapter`
- [ ] Implementar `OutlookCalendarAdapter`
- [ ] Crear `CalendarServiceRouter`
- [ ] Configurar beans de Spring para adaptadores
- [ ] Agregar dependencias Maven

### Fase 3: Servicios de Aplicación
- [ ] Crear `AppointmentIntentService`
- [ ] Crear `AppointmentAvailabilityService`
- [ ] Crear `DateTimeParserService`
- [ ] Crear `GetClientCalendarConfig`
- [ ] Implementar lógica de detección de intención
- [ ] Implementar parser de fecha/hora
- [ ] Implementar formateo de disponibilidad

### Fase 4: Casos de Uso
- [ ] Crear `CreateAppointment`
- [ ] Crear `GetAvailableSlots`
- [ ] Crear `CancelAppointment`
- [ ] Crear `RescheduleAppointment`
- [ ] Implementar validaciones de negocio
- [ ] Implementar sincronización con calendarios externos

### Fase 5: Integración
- [ ] Modificar `ProcessMessageWithAI` para interceptar agendamientos
- [ ] Actualizar `ReceiveWhatsAppMessage` para pasar parámetros
- [ ] Probar flujo completo de conversación
- [ ] Manejar errores y casos edge

### Fase 6: Configuración
- [ ] Agregar propiedades en `application.yml`
- [ ] Crear clases de configuración (`@ConfigurationProperties`)
- [ ] Documentar variables de entorno
- [ ] Configurar credenciales de Google Calendar
- [ ] Configurar credenciales de Outlook

### Fase 7: Configuración Dinámica
- [ ] Crear `UpdateClientCalendarConfig`
- [ ] Crear `CalendarConfigController`
- [ ] Crear DTOs para configuración
- [ ] Implementar validación de proveedores
- [ ] Documentar API de configuración

### Fase 8: Testing
- [ ] Tests unitarios para servicios
- [ ] Tests unitarios para casos de uso
- [ ] Tests de integración con Google Calendar
- [ ] Tests de integración con Outlook
- [ ] Tests end-to-end del flujo completo

### Fase 9: Documentación
- [ ] Documentar API de configuración
- [ ] Documentar flujo de agendamiento
- [ ] Crear guía de configuración de proveedores
- [ ] Documentar variables de entorno
- [ ] Crear ejemplos de uso

## 🔐 Consideraciones de Seguridad

1. **Credenciales de Calendario**
   - Almacenar credenciales de forma segura (secrets manager)
   - No exponer credenciales en logs
   - Rotar credenciales periódicamente

2. **Validación de Acceso**
   - Verificar que el cliente tenga permisos para agendar
   - Validar que el contacto pertenezca al cliente
   - Implementar rate limiting por cliente

3. **Datos Personales**
   - No exponer información sensible en logs
   - Cumplir con GDPR/LGPD si aplica
   - Encriptar datos sensibles en base de datos

## 🚀 Orden de Implementación Recomendado

1. **Semana 1**: Fase 1 (Modelo de Dominio)
2. **Semana 2**: Fase 2 (Puertos y Adaptadores - Google Calendar primero)
3. **Semana 3**: Fase 3 (Servicios de Aplicación)
4. **Semana 4**: Fase 4 (Casos de Uso)
5. **Semana 5**: Fase 5 (Integración) + Fase 6 (Configuración)
6. **Semana 6**: Fase 7 (Configuración Dinámica) + Testing
7. **Semana 7**: Ajustes, documentación y deploy

## 📝 Notas Adicionales

### Manejo de Errores
- Si falla la integración con calendario externo, continuar sin sincronización
- Loggear errores pero no exponer detalles técnicos al usuario
- Implementar retry logic para llamadas a APIs externas

### Mejoras Futuras
- Soporte para múltiples calendarios por cliente
- Recordatorios automáticos antes de la cita
- Cancelación y reprogramación desde WhatsApp
- Integración con más proveedores (iCal, CalDAV)
- Dashboard para gestión de agendamientos
- Notificaciones push

### Métricas a Implementar
- Tasa de éxito de agendamientos
- Tiempo promedio de respuesta
- Errores de integración con calendarios
- Horarios más solicitados
- Tasa de cancelación

## 🔗 Referencias

- [Google Calendar API Documentation](https://developers.google.com/calendar/api/v3/overview)
- [Microsoft Graph Calendar API](https://learn.microsoft.com/en-us/graph/api/resources/calendar)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [WhatsApp Business API](https://developers.facebook.com/docs/whatsapp)

---

**Versión**: 1.0  
**Última actualización**: 2024  
**Autor**: Equipo de Desarrollo

