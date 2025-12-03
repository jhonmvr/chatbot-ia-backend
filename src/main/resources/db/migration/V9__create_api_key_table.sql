-- Crear tabla para API Keys de clientes
CREATE TABLE IF NOT EXISTS chatbotia.api_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    api_secret_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_api_key_client FOREIGN KEY (client_id) 
        REFERENCES chatbotia.client(id) ON DELETE CASCADE,
    CONSTRAINT chk_api_key_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'REVOKED'))
);

-- Índices para mejorar rendimiento
CREATE INDEX IF NOT EXISTS idx_api_key_client_id ON chatbotia.api_key(client_id);
CREATE INDEX IF NOT EXISTS idx_api_key_api_key ON chatbotia.api_key(api_key);
CREATE INDEX IF NOT EXISTS idx_api_key_status ON chatbotia.api_key(status);
CREATE INDEX IF NOT EXISTS idx_api_key_client_status ON chatbotia.api_key(client_id, status);

-- Comentarios
COMMENT ON TABLE chatbotia.api_key IS 'Almacena las API Keys de los clientes para autenticación';
COMMENT ON COLUMN chatbotia.api_key.id IS 'ID único de la API Key';
COMMENT ON COLUMN chatbotia.api_key.client_id IS 'ID del cliente propietario de la API Key';
COMMENT ON COLUMN chatbotia.api_key.api_key IS 'Token público (identificador único)';
COMMENT ON COLUMN chatbotia.api_key.api_secret_hash IS 'Hash del secreto (nunca almacenar en texto plano)';
COMMENT ON COLUMN chatbotia.api_key.status IS 'Estado: ACTIVE, INACTIVE, REVOKED';
COMMENT ON COLUMN chatbotia.api_key.created_at IS 'Fecha de creación';
COMMENT ON COLUMN chatbotia.api_key.last_used_at IS 'Última vez que se usó la API Key';

