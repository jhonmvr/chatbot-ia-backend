-- =========================================================
-- V1__core.sql  (NO requiere pgvector;
-- crea kb_vector_ref)
-- =========================================================
CREATE SCHEMA IF NOT EXISTS chatbotia;

SET search_path TO chatbotia, public;


CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE EXTENSION IF NOT EXISTS pg_trgm;


-- Utilidad
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN NEW.updated_at = NOW();
 RETURN NEW;
 END;
 $$ LANGUAGE plpgsql;


-- =============== Tenancy / Planes (resumen) =================
CREATE TABLE IF NOT EXISTS plan (
                                    id UUID PRIMARY KEY,
                                    code VARCHAR(40) NOT NULL,
                                    name VARCHAR(120) NOT NULL,
                                    monthly_price_usd NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
                                    msg_limit_month BIGINT,
                                    users_limit INT,
                                    kb_tokens_limit_month BIGINT,
                                    concurrent_sessions_limit INT,
                                    retention_days INT DEFAULT 365,
                                    ai_model VARCHAR(80),
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_plan_code ON plan (UPPER(code));


CREATE TABLE IF NOT EXISTS client (
                                      id UUID PRIMARY KEY,
                                      name VARCHAR(200) NOT NULL,
                                      tax_id VARCHAR(50),
                                      domain VARCHAR(200),
                                      timezone VARCHAR(50) DEFAULT 'America/Guayaquil',
                                      status VARCHAR(30)  DEFAULT 'ACTIVE',
                                      metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_client_name ON client (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_client_name_trgm ON client USING gin (name gin_trgm_ops);

CREATE TRIGGER trg_client_upd BEFORE UPDATE ON client FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE IF NOT EXISTS subscription (
                                            id UUID PRIMARY KEY,
                                            client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                            plan_id   UUID NOT NULL REFERENCES plan(id)   ON DELETE RESTRICT,
                                            start_date DATE NOT NULL DEFAULT CURRENT_DATE,
                                            end_date   DATE,
                                            status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                            cancel_at  TIMESTAMPTZ,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sub_client ON subscription(client_id);

CREATE INDEX IF NOT EXISTS idx_sub_status ON subscription(status);

CREATE TRIGGER trg_sub_upd BEFORE UPDATE ON subscription FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============== Canales / Números =================
CREATE TABLE IF NOT EXISTS client_phone (
                                            id UUID PRIMARY KEY,
                                            client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                            channel   VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
                                            e164      VARCHAR(20) NOT NULL,
                                            provider  VARCHAR(20) NOT NULL DEFAULT 'TWILIO',
                                            provider_sid   VARCHAR(120),
                                            webhook_secret VARCHAR(120),
                                            is_active  BOOLEAN NOT NULL DEFAULT TRUE,
                                            is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                            metadata   JSONB NOT NULL DEFAULT '{}'::jsonb,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            CONSTRAINT uq_phone_per_client UNIQUE (client_id, channel, e164)
);

CREATE INDEX IF NOT EXISTS idx_client_phone_client ON client_phone(client_id);

CREATE INDEX IF NOT EXISTS idx_client_phone_e164   ON client_phone(e164);

CREATE TRIGGER trg_client_phone_upd BEFORE UPDATE ON client_phone FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============== Contactos =================
CREATE TABLE IF NOT EXISTS contact (
                                       id UUID PRIMARY KEY,
                                       client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                       external_id  VARCHAR(120),
                                       display_name VARCHAR(200),
                                       phone_e164   VARCHAR(20),
                                       email        VARCHAR(200),
                                       locale       VARCHAR(10),
                                       tags         TEXT[],
                                       attributes   JSONB NOT NULL DEFAULT '{}'::jsonb,
                                       last_seen_at TIMESTAMPTZ,
                                       created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       CONSTRAINT uq_contact_phone UNIQUE (client_id, phone_e164)
);

CREATE INDEX IF NOT EXISTS idx_contact_client ON contact(client_id);

CREATE INDEX IF NOT EXISTS idx_contact_name_trgm ON contact USING gin (display_name gin_trgm_ops);

CREATE TRIGGER trg_contact_upd BEFORE UPDATE ON contact FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============== Conversaciones / Mensajes =================
CREATE TABLE IF NOT EXISTS conversation (
                                            id UUID PRIMARY KEY,
                                            client_id  UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                            contact_id UUID NOT NULL REFERENCES contact(id) ON DELETE CASCADE,
                                            phone_id   UUID REFERENCES client_phone(id) ON DELETE SET NULL,
                                            channel    VARCHAR(20) NOT NULL,
                                            title      VARCHAR(250),
                                            status     VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                                            started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            closed_at  TIMESTAMPTZ,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conv_client  ON conversation(client_id);

CREATE INDEX IF NOT EXISTS idx_conv_contact ON conversation(contact_id);

CREATE INDEX IF NOT EXISTS idx_conv_status  ON conversation(status);

CREATE TRIGGER trg_conv_upd BEFORE UPDATE ON conversation FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE IF NOT EXISTS message (
                                       id UUID PRIMARY KEY,
                                       client_id       UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                       conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
                                       contact_id      UUID NOT NULL REFERENCES contact(id) ON DELETE CASCADE,
                                       phone_id        UUID REFERENCES client_phone(id) ON DELETE SET NULL,
                                       direction   VARCHAR(20) NOT NULL,  -- INBOUND/OUTBOUND/SYSTEM
                                       channel     VARCHAR(20) NOT NULL,
                                       provider    VARCHAR(20),
                                       message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
                                       body        TEXT,
                                       media       JSONB NOT NULL DEFAULT '{}'::jsonb,
                                       status      VARCHAR(20) NOT NULL,  -- QUEUED/SENT/...
                                       error_code  VARCHAR(50),
                                       raw_payload JSONB,
                                       created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       sent_at     TIMESTAMPTZ,
                                       delivered_at TIMESTAMPTZ,
                                       read_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_msg_conv    ON message(conversation_id);

CREATE INDEX IF NOT EXISTS idx_msg_client  ON message(client_id);

CREATE INDEX IF NOT EXISTS idx_msg_created ON message(created_at);

CREATE INDEX IF NOT EXISTS idx_msg_body_trgm ON message USING gin (body gin_trgm_ops);


-- =============== Cola (interno) =================
CREATE TABLE IF NOT EXISTS outbound_queue (
                                              id BIGSERIAL PRIMARY KEY,
                                              client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                              contact_id UUID REFERENCES contact(id) ON DELETE SET NULL,
                                              conversation_id UUID REFERENCES conversation(id) ON DELETE SET NULL,
                                              phone_id UUID REFERENCES client_phone(id) ON DELETE SET NULL,
                                              channel  VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
                                              template_id UUID,
                                              body TEXT,
                                              media JSONB NOT NULL DEFAULT '{}'::jsonb,
                                              schedule_at TIMESTAMPTZ,
                                              status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
                                              retries INT NOT NULL DEFAULT 0,
                                              last_error TEXT,
                                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                              updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outq_client_status ON outbound_queue(client_id, status);

CREATE INDEX IF NOT EXISTS idx_outq_schedule      ON outbound_queue(schedule_at);

CREATE TRIGGER trg_outq_upd BEFORE UPDATE ON outbound_queue FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============== Plantillas =================
CREATE TABLE IF NOT EXISTS message_template (
                                                id UUID PRIMARY KEY,
                                                client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                                name VARCHAR(120) NOT NULL,
                                                channel  VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
                                                language VARCHAR(10) NOT NULL DEFAULT 'es',
                                                category VARCHAR(20) NOT NULL DEFAULT 'UTILITY',
                                                content  JSONB NOT NULL DEFAULT '{}'::jsonb,
                                                provider_sid VARCHAR(120),
                                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_template
    ON message_template (client_id, channel, LOWER(name));

CREATE INDEX IF NOT EXISTS idx_template_client ON message_template(client_id);

CREATE TRIGGER trg_template_upd BEFORE UPDATE ON message_template FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============== KB (texto y metadata) =================
CREATE TABLE IF NOT EXISTS kb (
                                  id UUID PRIMARY KEY,
                                  client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                  name VARCHAR(200) NOT NULL,
                                  description TEXT,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_kb_name ON kb (client_id, LOWER(name));

CREATE INDEX IF NOT EXISTS idx_kb_client ON kb(client_id);

CREATE TRIGGER trg_kb_upd BEFORE UPDATE ON kb FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE IF NOT EXISTS kb_document (
                                           id UUID PRIMARY KEY,
                                           kb_id UUID NOT NULL REFERENCES kb(id) ON DELETE CASCADE,
                                           source_uri TEXT,
                                           mime_type  VARCHAR(100),
                                           language   VARCHAR(10),
                                           status     VARCHAR(20) DEFAULT 'READY',
                                           chunk_count INT DEFAULT 0,
                                           metadata   JSONB NOT NULL DEFAULT '{}'::jsonb,
                                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kbd_kb ON kb_document(kb_id);

CREATE TRIGGER trg_kbd_upd BEFORE UPDATE ON kb_document FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE IF NOT EXISTS kb_chunk (
                                        id UUID PRIMARY KEY,
                                        document_id UUID NOT NULL REFERENCES kb_document(id) ON DELETE CASCADE,
                                        chunk_index INT NOT NULL,
                                        content     TEXT NOT NULL,
                                        metadata    JSONB NOT NULL DEFAULT '{}'::jsonb,
                                        created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        CONSTRAINT uq_kb_chunk UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_kbchunk_doc ON kb_chunk(document_id);

CREATE INDEX IF NOT EXISTS idx_kbchunk_content_trgm ON kb_chunk USING gin (content gin_trgm_ops);


-- =============== Referencia al vector store (SIEMPRE) =================
-- Default backend = 'pgvector', y lista blanca de opciones
CREATE TABLE IF NOT EXISTS kb_vector_ref (
                                             chunk_id   UUID PRIMARY KEY REFERENCES kb_chunk(id) ON DELETE CASCADE,
                                             backend    VARCHAR(40)  NOT NULL DEFAULT 'pgvector',
                                             index_name VARCHAR(128) NOT NULL DEFAULT 'kb_embedding_pgvector',
                                             vector_id  VARCHAR(256) NOT NULL,  -- id en el backend;
 --en pgvector puede ser chunk_id::text
                                             created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                             CONSTRAINT ck_kbvec_backend CHECK (backend IN ('pgvector','qdrant','pinecone','milvus','weaviate','opensearch'))
);

CREATE INDEX IF NOT EXISTS idx_kbvec_backend_index ON kb_vector_ref (backend, index_name);


-- =============== Métricas =================
CREATE TABLE IF NOT EXISTS usage_daily (
                                           id BIGSERIAL PRIMARY KEY,
                                           client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
                                           day DATE NOT NULL,
                                           messages_in  BIGINT NOT NULL DEFAULT 0,
                                           messages_out BIGINT NOT NULL DEFAULT 0,
                                           tokens_in    BIGINT NOT NULL DEFAULT 0,
                                           tokens_out   BIGINT NOT NULL DEFAULT 0,
                                           vector_mb    NUMERIC(12,2) NOT NULL DEFAULT 0,
                                           created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                           CONSTRAINT uq_usage_day UNIQUE (client_id, day)
);

CREATE INDEX IF NOT EXISTS idx_usage_client ON usage_daily(client_id);


-- Vista útil
CREATE OR REPLACE VIEW v_conversation_last_message AS
SELECT DISTINCT ON (m.conversation_id)
    m.conversation_id,
    m.id AS last_message_id,
    m.created_at AS last_message_at,
    m.direction,
    m.status,
    LEFT(m.body, 500) AS snippet
FROM message m
ORDER BY m.conversation_id, m.created_at DESC;


COMMENT ON SCHEMA chatbotia IS 'Core del chatbot IA (desacoplado). kb_vector_ref siempre presente y por defecto backend=pgvector.';

