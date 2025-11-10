-- V5: Crear tabla de plantillas de WhatsApp
-- Autor: Sistema de Plantillas WhatsApp
-- Fecha: 2024-12-19
-- Descripción: Tabla para gestionar plantillas de WhatsApp Business API con sincronización a Meta

-- Crear tabla principal de plantillas
CREATE TABLE IF NOT EXISTS whatsapp_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_phone_id UUID NOT NULL REFERENCES client_phone(id) ON DELETE CASCADE,
    name VARCHAR(512) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN ('AUTHENTICATION', 'MARKETING', 'UTILITY')),
    language VARCHAR(10) NOT NULL DEFAULT 'es_ES',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'PAUSED', 'DISABLED')),
    parameter_format VARCHAR(20) CHECK (parameter_format IN ('NAMED', 'POSITIONAL')),
    meta_template_id VARCHAR(100),
    quality_rating VARCHAR(20) DEFAULT 'PENDING' CHECK (quality_rating IN ('HIGH', 'MEDIUM', 'LOW', 'PENDING')),
    rejection_reason TEXT,
    components JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Crear índices para optimización
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_client_phone ON whatsapp_templates(client_phone_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_status ON whatsapp_templates(status);
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_category ON whatsapp_templates(category);
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_meta_id ON whatsapp_templates(meta_template_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_name ON whatsapp_templates(name);
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_language ON whatsapp_templates(language);

-- Índice compuesto para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_client_status ON whatsapp_templates(client_phone_id, status);
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_client_category ON whatsapp_templates(client_phone_id, category);

-- Índice GIN para búsquedas en JSONB components
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_components ON whatsapp_templates USING GIN (components);

-- Restricción de unicidad: nombre único por cliente y categoría
CREATE UNIQUE INDEX IF NOT EXISTS idx_whatsapp_templates_unique_name 
ON whatsapp_templates(client_phone_id, name, category);

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_whatsapp_templates_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para actualizar updated_at
CREATE TRIGGER trigger_update_whatsapp_templates_updated_at
    BEFORE UPDATE ON whatsapp_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_whatsapp_templates_updated_at();

-- Comentarios en la tabla y columnas
COMMENT ON TABLE whatsapp_templates IS 'Plantillas de WhatsApp Business API con sincronización a Meta';
COMMENT ON COLUMN whatsapp_templates.id IS 'ID único de la plantilla';
COMMENT ON COLUMN whatsapp_templates.client_phone_id IS 'ID del número de teléfono cliente asociado';
COMMENT ON COLUMN whatsapp_templates.name IS 'Nombre de la plantilla (máximo 512 caracteres alfanuméricos en minúscula y guiones bajos)';
COMMENT ON COLUMN whatsapp_templates.category IS 'Categoría: AUTHENTICATION, MARKETING, UTILITY';
COMMENT ON COLUMN whatsapp_templates.language IS 'Código de idioma (ej: es_ES, en_US)';
COMMENT ON COLUMN whatsapp_templates.status IS 'Estado: DRAFT, PENDING, APPROVED, REJECTED, PAUSED, DISABLED';
COMMENT ON COLUMN whatsapp_templates.parameter_format IS 'Formato de parámetros: NAMED o POSITIONAL';
COMMENT ON COLUMN whatsapp_templates.meta_template_id IS 'ID de la plantilla en Meta API (cuando se sincroniza)';
COMMENT ON COLUMN whatsapp_templates.quality_rating IS 'Calificación de calidad según Meta: HIGH, MEDIUM, LOW, PENDING';
COMMENT ON COLUMN whatsapp_templates.rejection_reason IS 'Razón de rechazo de Meta (si aplica)';
COMMENT ON COLUMN whatsapp_templates.components IS 'Estructura JSON completa de componentes de la plantilla';
COMMENT ON COLUMN whatsapp_templates.created_at IS 'Fecha de creación';
COMMENT ON COLUMN whatsapp_templates.updated_at IS 'Fecha de última actualización';

-- Insertar datos de ejemplo para plantillas comunes
/*
INSERT INTO whatsapp_templates (
    client_phone_id,
    name,
    category,
    language,
    status,
    parameter_format,
    components
) VALUES 
-- Plantilla de autenticación OTP
(
    (SELECT id FROM client_phone LIMIT 1), -- Usar el primer client_phone disponible
    'otp_verification',
    'AUTHENTICATION',
    'es_ES',
    'DRAFT',
    'POSITIONAL',
    '[
        {
            "type": "BODY",
            "text": "Tu código de verificación es: {{1}}. Este código expira en {{2}} minutos.",
            "parameters": [
                {
                    "type": "text",
                    "text": "123456",
                    "example": "123456"
                },
                {
                    "type": "text", 
                    "text": "5",
                    "example": "5"
                }
            ]
        },
        {
            "type": "BUTTONS",
            "buttons": [
                {
                    "type": "QUICK_REPLY",
                    "text": "Copiar código"
                }
            ]
        }
    ]'::jsonb
),
-- Plantilla de marketing con imagen
(
    (SELECT id FROM client_phone LIMIT 1),
    'marketing_promotion',
    'MARKETING',
    'es_ES',
    'DRAFT',
    'NAMED',
    '[
        {
            "type": "HEADER",
            "text": "¡Oferta especial!",
            "media": {
                "type": "IMAGE",
                "url": "https://example.com/promo.jpg",
                "altText": "Oferta especial"
            }
        },
        {
            "type": "BODY",
            "text": "Hola {{first_name}}, tenemos una oferta especial para ti: {{offer_description}}. Válido hasta {{expiry_date}}.",
            "parameters": [
                {
                    "type": "text",
                    "parameterName": "first_name",
                    "text": "Juan",
                    "example": "Juan"
                },
                {
                    "type": "text",
                    "parameterName": "offer_description", 
                    "text": "20% de descuento",
                    "example": "20% de descuento"
                },
                {
                    "type": "text",
                    "parameterName": "expiry_date",
                    "text": "31 de diciembre",
                    "example": "31 de diciembre"
                }
            ]
        },
        {
            "type": "FOOTER",
            "text": "No respondas a este mensaje"
        },
        {
            "type": "BUTTONS",
            "buttons": [
                {
                    "type": "URL",
                    "text": "Ver oferta",
                    "url": "https://example.com/offer"
                }
            ]
        }
    ]'::jsonb
),
-- Plantilla de utilidad - confirmación de pedido
(
    (SELECT id FROM client_phone LIMIT 1),
    'order_confirmation',
    'UTILITY',
    'es_ES',
    'DRAFT',
    'NAMED',
    '[
        {
            "type": "BODY",
            "text": "Hola {{customer_name}}, tu pedido #{{order_number}} ha sido confirmado. Total: {{total_amount}}. Tiempo estimado de entrega: {{delivery_time}}.",
            "parameters": [
                {
                    "type": "text",
                    "parameterName": "customer_name",
                    "text": "María",
                    "example": "María"
                },
                {
                    "type": "text",
                    "parameterName": "order_number",
                    "text": "ORD-12345",
                    "example": "ORD-12345"
                },
                {
                    "type": "currency",
                    "parameterName": "total_amount",
                    "text": "25.50",
                    "example": "25.50"
                },
                {
                    "type": "text",
                    "parameterName": "delivery_time",
                    "text": "30-45 minutos",
                    "example": "30-45 minutos"
                }
            ]
        },
        {
            "type": "FOOTER",
            "text": "Gracias por tu compra"
        }
    ]'::jsonb
)
ON CONFLICT (client_phone_id, name, category) DO NOTHING;
 */

-- Crear vista para plantillas aprobadas y listas para usar
CREATE OR REPLACE VIEW whatsapp_templates_ready AS
SELECT 
    wt.*,
    cp.e164 as phone_number,
    cp.provider,
    c.name as client_name
FROM whatsapp_templates wt
JOIN client_phone cp ON wt.client_phone_id = cp.id
JOIN client c ON cp.client_id = c.id
WHERE wt.status = 'APPROVED' 
  AND wt.meta_template_id IS NOT NULL
  AND wt.meta_template_id != '';

COMMENT ON VIEW whatsapp_templates_ready IS 'Vista de plantillas aprobadas y listas para enviar';

-- Crear función para obtener estadísticas de plantillas por cliente
CREATE OR REPLACE FUNCTION get_template_stats_by_client(client_uuid UUID)
RETURNS TABLE (
    total_templates BIGINT,
    approved_templates BIGINT,
    pending_templates BIGINT,
    rejected_templates BIGINT,
    draft_templates BIGINT,
    auth_templates BIGINT,
    marketing_templates BIGINT,
    utility_templates BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_templates,
        COUNT(*) FILTER (WHERE wt.status = 'APPROVED') as approved_templates,
        COUNT(*) FILTER (WHERE wt.status = 'PENDING') as pending_templates,
        COUNT(*) FILTER (WHERE wt.status = 'REJECTED') as rejected_templates,
        COUNT(*) FILTER (WHERE wt.status = 'DRAFT') as draft_templates,
        COUNT(*) FILTER (WHERE wt.category = 'AUTHENTICATION') as auth_templates,
        COUNT(*) FILTER (WHERE wt.category = 'MARKETING') as marketing_templates,
        COUNT(*) FILTER (WHERE wt.category = 'UTILITY') as utility_templates
    FROM whatsapp_templates wt
    JOIN client_phone cp ON wt.client_phone_id = cp.id
    WHERE cp.client_id = client_uuid;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_template_stats_by_client IS 'Obtiene estadísticas de plantillas por cliente';
