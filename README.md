# Chatbot IA Backend - Docker Setup

Este proyecto incluye toda la configuración necesaria para ejecutar la aplicación Spring Boot con Docker.

## Archivos Creados

- `docker-compose-db.yml` - Configuración completa de Docker Compose con base de datos y aplicación
- `Dockerfile` - Imagen optimizada para la aplicación Spring Boot
- `env.docker` - Variables de entorno para Docker
- `application-docker.yml` - Configuración específica para Docker
- `.dockerignore` - Archivos a ignorar durante el build
- `start-docker.sh` - Script de inicio para Linux/Mac
- `start-docker.ps1` - Script de inicio para Windows PowerShell

## Configuración Actualizada

- `application.yml` - Actualizado para usar variables de entorno
- `docker-compose-db.yml` - Expandido para incluir la aplicación y configuración completa

## Uso Rápido

### Windows (PowerShell)
```powershell
# Iniciar servicios
.\start-docker.ps1

# Reconstruir e iniciar
.\start-docker.ps1 -Build

# Ver logs
.\start-docker.ps1 -Logs

# Detener servicios
.\start-docker.ps1 -Down
```

### Linux/Mac (Bash)
```bash
# Iniciar servicios
./start-docker.sh

# Reconstruir e iniciar
./start-docker.sh --build

# Ver logs
./start-docker.sh --logs

# Detener servicios
./start-docker.sh --down
```

### Docker Compose Directo
```bash
# Iniciar servicios
docker-compose -f docker-compose-db.yml up -d

# Reconstruir e iniciar
docker-compose -f docker-compose-db.yml up -d --build

# Ver logs
docker-compose -f docker-compose-db.yml logs -f

# Detener servicios
docker-compose -f docker-compose-db.yml down
```

## Servicios Disponibles

Una vez iniciados los servicios, tendrás acceso a:

- **Aplicación**: http://localhost:8180/agent-ai-backend
- **Swagger UI**: http://localhost:8180/agent-ai-backend/swagger-ui.html
- **Health Check**: http://localhost:8180/agent-ai-backend/actuator/health
- **Base de datos PostgreSQL**: localhost:25432

## Variables de Entorno

El archivo `env.docker` contiene todas las variables necesarias. Puedes modificarlo según tus necesidades:

```env
# Base de datos
POSTGRES_DB=chatbotia
POSTGRES_USER=postgres
POSTGRES_PASSWORD=masterylas20
POSTGRES_PORT=25432

# Aplicación
APP_PORT=8180
SPRING_PROFILES_ACTIVE=docker

# OpenAI
OPENAI_API_KEY=tu-api-key-aqui

# WhatsApp (opcional)
META_WHATSAPP_ACCESS_TOKEN=
META_WHATSAPP_PHONE_NUMBER_ID=
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=

# wwebjs
WWEBJS_BASE_URL=http://localhost:8085
```

## Características del Setup

### Dockerfile Optimizado
- Multi-stage build para reducir tamaño de imagen
- Usuario no-root para seguridad
- Health check integrado
- Configuración JVM optimizada para contenedores

### Docker Compose Completo
- Base de datos PostgreSQL con pgvector
- Health checks para dependencias
- Variables de entorno configurables
- Volúmenes persistentes para datos

### Configuración Flexible
- Variables de entorno con valores por defecto
- Perfil Docker específico
- Configuración separada para desarrollo y producción

## Troubleshooting

### Problemas Comunes

1. **Puerto ocupado**: Cambia `APP_PORT` en `env.docker`
2. **Base de datos no conecta**: Verifica `POSTGRES_HOST` y `POSTGRES_PORT`
3. **API Key inválida**: Actualiza `OPENAI_API_KEY` en `env.docker`

### Logs y Debugging

```bash
# Ver logs de todos los servicios
docker-compose -f docker-compose-db.yml logs -f

# Ver logs de un servicio específico
docker-compose -f docker-compose-db.yml logs -f app
docker-compose -f docker-compose-db.yml logs -f db

# Entrar al contenedor de la aplicación
docker-compose -f docker-compose-db.yml exec app sh
```

### Limpieza Completa

```bash
# Detener y eliminar contenedores, redes y volúmenes
docker-compose -f docker-compose-db.yml down -v

# Eliminar imágenes
docker-compose -f docker-compose-db.yml down --rmi all
```
