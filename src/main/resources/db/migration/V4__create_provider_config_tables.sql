-- =========================================================
-- V4__create_provider_config_tables.sql
-- - Crea tablas para configuración parametrizable de proveedores
-- - provider_config: Define esquemas de configuración por tipo de proveedor
-- - client_phone_provider_config: Almacena valores específicos por cliente
-- - Migra datos existentes de client_phone a la nueva estructura
-- =========================================================

SET search_path TO chatbotia, public;

-- Tabla para definir esquemas de configuración de proveedores
CREATE TABLE IF NOT EXISTS provider_config (
    id UUID PRIMARY KEY,
    provider_name VARCHAR(50) NOT NULL,
    provider_type VARCHAR(20) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    api_base_url VARCHAR(500),
    api_version VARCHAR(20),
    webhook_url_template VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    config_schema JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_provider_type UNIQUE (provider_type)
);

-- Tabla para configuraciones específicas de proveedores por cliente
CREATE TABLE IF NOT EXISTS client_phone_provider_config (
    id UUID PRIMARY KEY,
    client_phone_id UUID NOT NULL REFERENCES client_phone(id) ON DELETE CASCADE,
    provider_config_id UUID NOT NULL REFERENCES provider_config(id) ON DELETE RESTRICT,
    config_values JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_client_phone_provider UNIQUE (client_phone_id, provider_config_id)
);

-- Índices para mejorar el rendimiento
CREATE INDEX IF NOT EXISTS idx_provider_config_type ON provider_config(provider_type);
CREATE INDEX IF NOT EXISTS idx_provider_config_active ON provider_config(is_active);
CREATE INDEX IF NOT EXISTS idx_client_phone_provider_config_phone ON client_phone_provider_config(client_phone_id);
CREATE INDEX IF NOT EXISTS idx_client_phone_provider_config_provider ON client_phone_provider_config(provider_config_id);
CREATE INDEX IF NOT EXISTS idx_client_phone_provider_config_active ON client_phone_provider_config(is_active);

-- Triggers para updated_at
CREATE TRIGGER trg_provider_config_upd BEFORE UPDATE ON provider_config FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_client_phone_provider_config_upd BEFORE UPDATE ON client_phone_provider_config FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Insertar configuraciones por defecto para proveedores existentes
INSERT INTO provider_config (id, provider_name, provider_type, display_name, description, api_base_url, api_version, config_schema) VALUES
(
    gen_random_uuid(),
    'Meta WhatsApp Business',
    'META',
    'Meta WhatsApp Business API',
    'Proveedor oficial de WhatsApp Business de Meta',
    'https://graph.facebook.com',
    'v21.0',
    '{
        "required_fields": ["access_token", "phone_number_id"],
        "optional_fields": ["api_version", "webhook_secret", "verify_token"],
        "field_configs": {
            "access_token": {
                "type": "string",
                "max_length": 500,
                "description": "Token de acceso de Meta WhatsApp Business API",
                "sensitive": true
            },
            "phone_number_id": {
                "type": "string",
                "max_length": 50,
                "description": "ID del número de teléfono en Meta WhatsApp Business"
            },
            "api_version": {
                "type": "string",
                "max_length": 10,
                "description": "Versión de la API de Meta",
                "default": "v21.0"
            },
            "webhook_secret": {
                "type": "string",
                "max_length": 120,
                "description": "Secreto para validar webhooks",
                "sensitive": true
            },
            "verify_token": {
                "type": "string",
                "max_length": 100,
                "description": "Token de verificación para webhooks",
                "sensitive": true
            }
        }
    }'::jsonb
),
(
    gen_random_uuid(),
    'Twilio WhatsApp',
    'TWILIO',
    'Twilio WhatsApp API',
    'Proveedor de WhatsApp a través de Twilio',
    'https://api.twilio.com/2010-04-01',
    null,
    '{
        "required_fields": ["account_sid", "auth_token"],
        "optional_fields": ["webhook_secret", "verify_token"],
        "field_configs": {
            "account_sid": {
                "type": "string",
                "max_length": 50,
                "description": "Account SID de Twilio"
            },
            "auth_token": {
                "type": "string",
                "max_length": 100,
                "description": "Token de autenticación de Twilio",
                "sensitive": true
            },
            "webhook_secret": {
                "type": "string",
                "max_length": 120,
                "description": "Secreto para validar webhooks",
                "sensitive": true
            },
            "verify_token": {
                "type": "string",
                "max_length": 100,
                "description": "Token de verificación para webhooks",
                "sensitive": true
            }
        }
    }'::jsonb
),
(
    gen_random_uuid(),
    'WhatsApp Web JS',
    'WWEBJS',
    'WhatsApp Web JS',
    'Proveedor usando WhatsApp Web JS',
    null,
    null,
    '{
        "required_fields": ["session_id"],
        "optional_fields": ["webhook_url", "api_base_url"],
        "field_configs": {
            "session_id": {
                "type": "string",
                "max_length": 100,
                "description": "ID de sesión de WhatsApp Web JS"
            },
            "webhook_url": {
                "type": "string",
                "max_length": 500,
                "description": "URL del webhook para WhatsApp Web JS"
            },
            "api_base_url": {
                "type": "string",
                "max_length": 500,
                "description": "URL base de la API del proveedor"
            }
        }
    }'::jsonb
);

-- Migrar datos existentes de client_phone a la nueva estructura
-- Solo para registros que tengan datos de configuración
INSERT INTO client_phone_provider_config (id, client_phone_id, provider_config_id, config_values, is_active)
SELECT 
    gen_random_uuid(),
    cp.id,
    pc.id,
    CASE 
        WHEN cp.provider = 'META' THEN jsonb_build_object(
            'access_token', COALESCE(cp.meta_access_token, ''),
            'phone_number_id', COALESCE(cp.meta_phone_number_id, ''),
            'api_version', COALESCE(cp.meta_api_version, 'v21.0'),
            'webhook_secret', COALESCE(cp.webhook_secret, ''),
            'verify_token', COALESCE(cp.verify_token, '')
        )
        WHEN cp.provider = 'TWILIO' THEN jsonb_build_object(
            'account_sid', COALESCE(cp.twilio_account_sid, ''),
            'auth_token', COALESCE(cp.twilio_auth_token, ''),
            'webhook_secret', COALESCE(cp.webhook_secret, ''),
            'verify_token', COALESCE(cp.verify_token, '')
        )
        WHEN cp.provider = 'WWEBJS' THEN jsonb_build_object(
            'session_id', COALESCE(cp.wwebjs_session_id, ''),
            'webhook_url', COALESCE(cp.wwebjs_webhook_url, ''),
            'api_base_url', COALESCE(cp.api_base_url, '')
        )
        ELSE '{}'::jsonb
    END,
    cp.is_active
FROM client_phone cp
JOIN provider_config pc ON pc.provider_type = cp.provider
WHERE cp.provider IN ('META', 'TWILIO', 'WWEBJS')
AND (
    cp.meta_access_token IS NOT NULL OR 
    cp.twilio_account_sid IS NOT NULL OR 
    cp.wwebjs_session_id IS NOT NULL
);

-- Comentarios para documentar las tablas
COMMENT ON TABLE provider_config IS 'Esquemas de configuración parametrizables para proveedores de WhatsApp';
COMMENT ON TABLE client_phone_provider_config IS 'Configuraciones específicas de proveedores por número de WhatsApp de cliente';

COMMENT ON COLUMN provider_config.config_schema IS 'Esquema JSON que define los campos requeridos y opcionales para cada proveedor';
COMMENT ON COLUMN client_phone_provider_config.config_values IS 'Valores reales de configuración en formato JSON según el esquema del proveedor';
