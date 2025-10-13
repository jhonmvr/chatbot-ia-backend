-- =========================================================
-- V2__backend_pgvector.sql  (DESACOPLADO)
-- - Crea extensión pgvector (si está instalada en el servidor)
-- - Crea tabla de embeddings separada del core
-- - Crea índices vectoriales y de filtros
-- - NO define triggers ni funciones (la app Java maneja I/O)
-- =========================================================

SET search_path TO chatbotia, public;


DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
    RAISE EXCEPTION 'pgvector no está instalado en este servidor.';

END IF;

END$$;


CREATE EXTENSION IF NOT EXISTS vector;


-- Tabla física para embeddings de pgvector (separada del core).
-- Nota: el core ya tiene kb_chunk (texto/metadata) y kb_vector_ref (referencia portable).
CREATE TABLE IF NOT EXISTS kb_embedding_pgvector (
    chunk_id   UUID PRIMARY KEY REFERENCES kb_chunk(id) ON DELETE CASCADE,
    embedding  vector(1536) NOT NULL,  -- 1536 para text-embedding-3-small (compatible con IVFFlat)
    client_id  UUID,                   -- Opcional: para filtros
    kb_id      UUID,                   -- Opcional: para filtros
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- Índice vectorial usando IVFFlat (eficiente para vectores <= 2000 dimensiones)
-- lists = número de listas de inverted file (100 es bueno para datasets pequeños/medianos)
-- Ajustar según el tamaño: 1-10K docs: 100 | 10-100K docs: 1000 | 100K-1M: 10000
CREATE INDEX IF NOT EXISTS idx_kbemb_pgvector_ivf
    ON kb_embedding_pgvector USING ivfflat (embedding vector_cosine_ops) 
    WITH (lists = 100);


-- Índices por filtros típicos (opcionales pero útiles)
CREATE INDEX IF NOT EXISTS idx_kbemb_pgvector_client ON kb_embedding_pgvector (client_id);

CREATE INDEX IF NOT EXISTS idx_kbemb_pgvector_kb     ON kb_embedding_pgvector (kb_id);


COMMENT ON TABLE kb_embedding_pgvector IS
  'Embeddings almacenados con pgvector. La app/adapter Java maneja inserciones, lecturas y la sincronización con kb_vector_ref.';

