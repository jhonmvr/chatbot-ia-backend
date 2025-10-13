# Multi-stage build para optimizar el tamaño de la imagen
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

# Descargar dependencias (esto se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Imagen de runtime
FROM eclipse-temurin:21-jre-alpine

# Instalar dependencias necesarias
RUN apk add --no-cache \
    curl \
    tzdata

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el JAR compilado desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Cambiar ownership al usuario no-root
RUN chown -R appuser:appgroup /app

# Cambiar al usuario no-root
USER appuser

# Exponer puerto
EXPOSE 8180

# Configurar JVM para contenedores
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Argumentos de build (se pasan desde docker-compose)
ARG SPRING_PROFILES_ACTIVE=docker
ARG POSTGRES_DB=chatbotia
ARG POSTGRES_USER=postgres
ARG POSTGRES_PASSWORD=masterylas20
ARG POSTGRES_HOST=db
ARG POSTGRES_PORT=5432
ARG OPENAI_API_KEY=""
ARG META_WHATSAPP_ACCESS_TOKEN=""
ARG META_WHATSAPP_PHONE_NUMBER_ID=""
ARG META_WHATSAPP_WEBHOOK_VERIFY_TOKEN=my-secret-token
ARG TWILIO_ACCOUNT_SID=""
ARG TWILIO_AUTH_TOKEN=""
ARG TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
ARG WWEBJS_BASE_URL=http://localhost:8085

# Convertir ARGs a ENVs para runtime
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV POSTGRES_DB=${POSTGRES_DB}
ENV POSTGRES_USER=${POSTGRES_USER}
ENV POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
ENV POSTGRES_HOST=${POSTGRES_HOST}
ENV POSTGRES_PORT=${POSTGRES_PORT}
ENV OPENAI_API_KEY=${OPENAI_API_KEY}
ENV META_WHATSAPP_ACCESS_TOKEN=${META_WHATSAPP_ACCESS_TOKEN}
ENV META_WHATSAPP_PHONE_NUMBER_ID=${META_WHATSAPP_PHONE_NUMBER_ID}
ENV META_WHATSAPP_WEBHOOK_VERIFY_TOKEN=${META_WHATSAPP_WEBHOOK_VERIFY_TOKEN}
ENV TWILIO_ACCOUNT_SID=${TWILIO_ACCOUNT_SID}
ENV TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN}
ENV TWILIO_WHATSAPP_FROM=${TWILIO_WHATSAPP_FROM}
ENV WWEBJS_BASE_URL=${WWEBJS_BASE_URL}

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8180/agent-ai-backend/actuator/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
