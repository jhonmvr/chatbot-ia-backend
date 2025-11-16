# Integración de Google Calendar y Outlook (Microsoft 365) para agendamiento de citas

## 1. Arquitectura general de integración de calendarios

### 1.1. Componentes principales en tu backend

1. **Módulo de identidades de calendario**
   - Entidad `CalendarProviderAccount` con:
     - `userId`
     - `provider` = "GOOGLE" | "OUTLOOK"
     - `accessToken`
     - `refreshToken`
     - `expiresAt`
     - Config extra (ej. `impersonatedEmail`)

2. **Módulo de agendamiento**
   - Entidades: `Appointment`, `AppointmentExternalMapping`
   - Servicios: `AvailabilityService`, `BookingService`

3. **Módulo de webhooks**
   - Google push notifications
   - Microsoft Graph change notifications

4. **Normalización de datos**
   - Conversión a un modelo común de evento.

---

## 2. Autenticación y permisos

### 2.1. Google Calendar

Overview:  
https://developers.google.com/workspace/calendar/api/guides/overview

Referencia API v3:  
https://developers.google.com/workspace/calendar/api/v3/reference

Scopes OAuth2:  
https://developers.google.com/identity/protocols/oauth2/scopes

#### OAuth User-Based
Flujo Authorization Code.

#### Service Account + Domain-Wide Delegation  
Guía:  
https://support.google.com/a/answer/162106

---

### 2.2. Outlook / Microsoft 365 – Microsoft Graph

Calendar overview:  
https://learn.microsoft.com/en-us/graph/api/resources/calendar-overview?view=graph-rest-1.0

Outlook Calendar Concepts:  
https://learn.microsoft.com/en-us/graph/outlook-calendar-concept-overview

Permisos:
- Calendars.Read
- Calendars.ReadWrite

---

## 3. Google Calendar – Operaciones esenciales

Events reference:  
https://developers.google.com/workspace/calendar/api/v3/reference/events

Events list:  
https://developers.google.com/workspace/calendar/api/v3/reference/events/list

FreeBusy:  
https://developers.google.com/workspace/calendar/api/v3/reference/freebusy

### Crear evento
POST https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events

JSON:
{
  "summary": "Reunión de prueba",
  "description": "Detalles",
  "start": { "dateTime": "...", "timeZone": "America/Guayaquil" },
  "end": { "dateTime": "...", "timeZone": "America/Guayaquil" },
  "attendees": [{ "email": "x@example.com" }]
}

### FreeBusy Query
POST https://www.googleapis.com/calendar/v3/freeBusy

---

## 4. Microsoft Graph – Calendario de Outlook

Crear evento:  
https://learn.microsoft.com/en-us/graph/api/calendar-post-events?view=graph-rest-1.0

Listar eventos:  
https://learn.microsoft.com/en-us/graph/api/calendar-list-events?view=graph-rest-1.0

### Crear evento
POST https://graph.microsoft.com/v1.0/me/events

JSON:
{
  "subject": "Reunión de prueba",
  "start": { "dateTime": "2025-11-20T15:00:00", "timeZone": "America/Guayaquil" },
  "end": { "dateTime": "2025-11-20T16:00:00", "timeZone": "America/Guayaquil" },
  "attendees": [{ "emailAddress": { "address": "x@example.com" } }],
  "isOnlineMeeting": true,
  "onlineMeetingProvider": "teamsForBusiness"
}

### Free/Busy – getSchedule

Docs:  
https://learn.microsoft.com/en-us/graph/outlook-get-free-busy-schedule  
https://learn.microsoft.com/en-us/graph/api/calendar-getschedule?view=graph-rest-1.0

---

## 5. Webhooks y Notificaciones

### Google Push Notifications

Guía:  
https://developers.google.com/workspace/calendar/api/guides/push

events.watch:  
https://developers.google.com/workspace/calendar/api/v3/reference/events/watch

### Microsoft Graph Webhooks

Overview:  
https://learn.microsoft.com/en-us/graph/api/resources/change-notifications-api-overview?view=graph-rest-1.0

Crear subscription:  
https://learn.microsoft.com/en-us/graph/api/subscription-post-subscriptions?view=graph-rest-1.0

---

## 6. Flujo típico de agendamiento

1. Usuario pide cita.
2. Backend consulta disponibilidad.
3. Usuario elige slot.
4. Backend crea evento en Google/Outlook.
5. Backend guarda mapping.
6. Webhooks sincronizan cambios externos.

---

## 7. Consideraciones importantes

- Timezones
- Refresh tokens
- Rate limits
- Seguridad en webhooks
- Multi-tenant

---

## 8. Lista final de enlaces

### Google
- https://developers.google.com/workspace/calendar/api/guides/overview
- https://developers.google.com/workspace/calendar/api/v3/reference
- https://developers.google.com/workspace/calendar/api/v3/reference/events
- https://developers.google.com/workspace/calendar/api/v3/reference/events/list
- https://developers.google.com/workspace/calendar/api/v3/reference/freebusy
- https://developers.google.com/workspace/calendar/api/guides/push
- https://developers.google.com/workspace/calendar/api/v3/reference/events/watch
- https://developers.google.com/identity/protocols/oauth2/scopes
- https://support.google.com/a/answer/162106

### Microsoft Graph
- https://learn.microsoft.com/en-us/graph/api/resources/calendar-overview?view=graph-rest-1.0
- https://learn.microsoft.com/en-us/graph/outlook-calendar-concept-overview
- https://learn.microsoft.com/en-us/graph/api/calendar-list-events?view=graph-rest-1.0
- https://learn.microsoft.com/en-us/graph/api/calendar-post-events?view=graph-rest-1.0
- https://learn.microsoft.com/en-us/graph/outlook-get-free-busy-schedule
- https://learn.microsoft.com/en-us/graph/api/calendar-getschedule?view=graph-rest-1.0
- https://learn.microsoft.com/en-us/graph/api/resources/change-notifications-api-overview?view=graph-rest-1.0
- https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks
- https://learn.microsoft.com/en-us/graph/outlook-change-notifications-overview
- https://learn.microsoft.com/en-us/graph/api/subscription-post-subscriptions?view=graph-rest-1.0
