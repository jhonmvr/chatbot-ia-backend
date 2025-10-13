-- =========================================================
-- V3__update_vector_dimensions.sql
-- - Actualiza la dimensión del vector de 3072 a 1536
-- - Para usar text-embedding-3-small (compatible con IVFFlat)
-- - IVFFlat tiene límite de 2000 dimensiones
-- =========================================================

SET search_path TO chatbotia, public;

-- Eliminar índices vectoriales existentes (IVFFlat o HNSW)
DROP INDEX IF EXISTS chatbotia.idx_kbemb_pgvector_ivf;
DROP INDEX IF EXISTS chatbotia.idx_kbemb_pgvector_hnsw;

-- Eliminar todos los embeddings existentes (si los hay)
-- Esto es necesario porque no se puede cambiar la dimensión de un vector existente
TRUNCATE TABLE chatbotia.kb_embedding_pgvector;

-- Cambiar la definición de la columna
ALTER TABLE chatbotia.kb_embedding_pgvector 
    ALTER COLUMN embedding TYPE vector(1536);

-- Crear índice IVFFlat (compatible con pgvector estándar)
-- IVFFlat funciona bien con vectores de 1536 dimensiones
CREATE INDEX idx_kbemb_pgvector_ivf
    ON chatbotia.kb_embedding_pgvector USING ivfflat (embedding vector_cosine_ops) 
    WITH (lists = 100);

COMMENT ON COLUMN chatbotia.kb_embedding_pgvector.embedding IS 
    'Vector embedding de 1536 dimensiones (text-embedding-3-small) con índice IVFFlat';

