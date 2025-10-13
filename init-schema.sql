-- Script de inicialización para crear el esquema chatbotia
-- Este archivo se ejecuta automáticamente cuando se crea el contenedor de PostgreSQL

-- Crear el esquema si no existe
CREATE SCHEMA IF NOT EXISTS chatbotia;

-- Crear la extensión pgvector si no existe
CREATE EXTENSION IF NOT EXISTS vector;

-- Otorgar permisos al usuario postgres
GRANT ALL PRIVILEGES ON SCHEMA chatbotia TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA chatbotia TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA chatbotia TO postgres;

-- Configurar el esquema por defecto
ALTER DATABASE chatbotia SET search_path TO chatbotia, public;
