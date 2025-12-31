-- V10: Agregar campos adicionales para información de rechazo de templates
-- Autor: Sistema de Plantillas WhatsApp
-- Fecha: 2024-12-30
-- Descripción: Agrega campos para almacenar información detallada sobre el rechazo de plantillas de Meta

-- Agregar columna para código de error de rechazo
ALTER TABLE chatbotia.whatsapp_templates 
ADD COLUMN IF NOT EXISTS rejection_code VARCHAR(50);

-- Agregar columna para detalles estructurados del rechazo en formato JSONB
ALTER TABLE chatbotia.whatsapp_templates 
ADD COLUMN IF NOT EXISTS rejection_details JSONB;

-- Agregar columna para fecha y hora del rechazo
ALTER TABLE chatbotia.whatsapp_templates 
ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE;

-- Crear índice para búsquedas por código de rechazo
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_rejection_code 
ON chatbotia.whatsapp_templates(rejection_code) 
WHERE rejection_code IS NOT NULL;

-- Crear índice GIN para búsquedas en JSONB rejection_details
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_rejection_details 
ON chatbotia.whatsapp_templates USING GIN (rejection_details) 
WHERE rejection_details IS NOT NULL;

-- Crear índice para búsquedas por fecha de rechazo
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_rejected_at 
ON chatbotia.whatsapp_templates(rejected_at) 
WHERE rejected_at IS NOT NULL;

-- Índice compuesto para templates rechazados (útil para reportes)
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_rejected_status 
ON chatbotia.whatsapp_templates(status, rejected_at) 
WHERE status = 'REJECTED' AND rejected_at IS NOT NULL;

-- Comentarios en las nuevas columnas
COMMENT ON COLUMN chatbotia.whatsapp_templates.rejection_code IS 'Código de error de rechazo de Meta (si está disponible). Ejemplo: INVALID_FORMAT, POLICY_VIOLATION';
COMMENT ON COLUMN chatbotia.whatsapp_templates.rejection_details IS 'Detalles adicionales del rechazo en formato JSONB. Puede contener información estructurada sobre el motivo del rechazo, descripción, timestamp, etc.';
COMMENT ON COLUMN chatbotia.whatsapp_templates.rejected_at IS 'Fecha y hora en que se produjo el rechazo de la plantilla por parte de Meta';

