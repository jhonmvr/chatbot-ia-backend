#!/bin/bash

# Script para inicializar la base de datos con el esquema necesario
# Este script se ejecuta después de que PostgreSQL esté listo

echo "Esperando a que PostgreSQL esté listo..."
sleep 10

# Conectar a PostgreSQL y crear el esquema
PGPASSWORD=${POSTGRES_PASSWORD:-masterylas20} psql -h db -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-chatbotia} << EOF
-- Crear el esquema si no existe
CREATE SCHEMA IF NOT EXISTS chatbotia;

-- Crear la extensión pgvector si no existe
CREATE EXTENSION IF NOT EXISTS vector;

-- Otorgar permisos al usuario postgres
GRANT ALL PRIVILEGES ON SCHEMA chatbotia TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA chatbotia TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA chatbotia TO postgres;

-- Configurar el esquema por defecto
ALTER DATABASE ${POSTGRES_DB:-chatbotia} SET search_path TO chatbotia, public;

-- Verificar que el esquema se creó correctamente
\dn chatbotia
EOF

echo "Esquema chatbotia creado exitosamente"
