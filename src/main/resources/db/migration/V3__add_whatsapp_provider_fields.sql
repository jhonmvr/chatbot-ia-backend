-- =========================================================
-- V3__add_whatsapp_provider_fields.sql
-- - Agrega campos específicos para proveedores de WhatsApp
-- - Meta WhatsApp: access_token, phone_number_id, api_version
-- - Twilio: account_sid, auth_token
-- - WWebJs: session_id, webhook_url
-- - Campos comunes: api_base_url, webhook_url, verify_token
-- =========================================================

SET search_path TO chatbotia, public;

-- Campos específicos para Meta WhatsApp
ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS meta_access_token VARCHAR(500);

ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS meta_phone_number_id VARCHAR(50);

ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS meta_api_version VARCHAR(10);

-- Campos específicos para Twilio
ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS twilio_account_sid VARCHAR(50);

ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS twilio_auth_token VARCHAR(100);

-- Campos específicos para WWebJs
ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS wwebjs_session_id VARCHAR(100);

ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS wwebjs_webhook_url VARCHAR(500);

-- Campos comunes para todos los proveedores
ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS api_base_url VARCHAR(500);

ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS webhook_url VARCHAR(500);

ALTER TABLE chatbotia.client_phone 
    ADD COLUMN IF NOT EXISTS verify_token VARCHAR(100);

-- Comentarios para documentar los campos
COMMENT ON COLUMN chatbotia.client_phone.meta_access_token IS 'Token de acceso para la API de Meta WhatsApp Business';
COMMENT ON COLUMN chatbotia.client_phone.meta_phone_number_id IS 'ID del número de teléfono en Meta WhatsApp Business';
COMMENT ON COLUMN chatbotia.client_phone.meta_api_version IS 'Versión de la API de Meta (ej: v21.0)';

COMMENT ON COLUMN chatbotia.client_phone.twilio_account_sid IS 'Account SID de Twilio para WhatsApp';
COMMENT ON COLUMN chatbotia.client_phone.twilio_auth_token IS 'Token de autenticación de Twilio';

COMMENT ON COLUMN chatbotia.client_phone.wwebjs_session_id IS 'ID de sesión para WhatsApp Web JS';
COMMENT ON COLUMN chatbotia.client_phone.wwebjs_webhook_url IS 'URL del webhook para WhatsApp Web JS';

COMMENT ON COLUMN chatbotia.client_phone.api_base_url IS 'URL base de la API del proveedor';
COMMENT ON COLUMN chatbotia.client_phone.webhook_url IS 'URL del webhook general para recibir mensajes';
COMMENT ON COLUMN chatbotia.client_phone.verify_token IS 'Token de verificación para webhooks';

-- Índices para mejorar el rendimiento de consultas por proveedor
CREATE INDEX IF NOT EXISTS idx_client_phone_provider ON chatbotia.client_phone(provider);
CREATE INDEX IF NOT EXISTS idx_client_phone_meta_phone_id ON chatbotia.client_phone(meta_phone_number_id) WHERE meta_phone_number_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_client_phone_twilio_sid ON chatbotia.client_phone(twilio_account_sid) WHERE twilio_account_sid IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_client_phone_wwebjs_session ON chatbotia.client_phone(wwebjs_session_id) WHERE wwebjs_session_id IS NOT NULL;

COMMENT ON TABLE chatbotia.client_phone IS 'Tabla de números de teléfono de clientes con soporte para múltiples proveedores de WhatsApp (Meta, Twilio, WWebJs)';