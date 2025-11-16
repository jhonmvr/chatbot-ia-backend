-- =========================================================
-- V8__create_calendar_provider_account_table.sql
-- - Crea tabla para configuraciones de proveedores de calendario
-- - Soporta Google Calendar y Microsoft Outlook (Microsoft 365)
-- - Almacena credenciales OAuth2 y configuración adicional
-- =========================================================

SET search_path TO chatbotia, public;

-- Tabla para cuentas de proveedores de calendario
CREATE TABLE IF NOT EXISTS calendar_provider_account (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GOOGLE', 'OUTLOOK')),
    account_email VARCHAR(255) NOT NULL,
    access_token VARCHAR(2000) NOT NULL,
    refresh_token VARCHAR(2000) NOT NULL,
    token_expires_at TIMESTAMPTZ,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_client_provider_email UNIQUE (client_id, provider, account_email)
);

-- Índices para mejorar el rendimiento
CREATE INDEX IF NOT EXISTS idx_calendar_provider_account_client ON calendar_provider_account(client_id);
CREATE INDEX IF NOT EXISTS idx_calendar_provider_account_provider ON calendar_provider_account(provider);
CREATE INDEX IF NOT EXISTS idx_calendar_provider_account_active ON calendar_provider_account(is_active);
CREATE INDEX IF NOT EXISTS idx_calendar_provider_account_client_provider_active ON calendar_provider_account(client_id, provider, is_active);

-- Trigger para updated_at
CREATE TRIGGER trg_calendar_provider_account_upd 
    BEFORE UPDATE ON calendar_provider_account 
    FOR EACH ROW 
    EXECUTE FUNCTION set_updated_at();

-- Comentarios para documentar la tabla
COMMENT ON TABLE calendar_provider_account IS 'Cuentas de proveedores de calendario configuradas para clientes. Soporta Google Calendar y Microsoft Outlook.';
COMMENT ON COLUMN calendar_provider_account.provider IS 'Proveedor de calendario: GOOGLE o OUTLOOK';
COMMENT ON COLUMN calendar_provider_account.account_email IS 'Email de la cuenta de calendario (ej: usuario@gmail.com o usuario@outlook.com)';
COMMENT ON COLUMN calendar_provider_account.access_token IS 'Token de acceso OAuth2 (se renueva con refresh_token)';
COMMENT ON COLUMN calendar_provider_account.refresh_token IS 'Token de renovación OAuth2 para obtener nuevos access tokens';
COMMENT ON COLUMN calendar_provider_account.token_expires_at IS 'Fecha y hora de expiración del access_token';
COMMENT ON COLUMN calendar_provider_account.config IS 'Configuración adicional en JSON. Para Google: calendar_id, impersonated_email, timezone. Para Outlook: calendar_id, tenant_id, timezone';

