-- V6: Añadir campos de datos básicos de persona a la tabla contact
-- Autor: Sistema de Contactos
-- Fecha: 2024-12-19
-- Descripción: Añade campos completos de información de persona a la tabla contact

-- Añadir campos de información personal básica
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS middle_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS title VARCHAR(50),
ADD COLUMN IF NOT EXISTS gender VARCHAR(20),
ADD COLUMN IF NOT EXISTS birth_date TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS nationality VARCHAR(50),
ADD COLUMN IF NOT EXISTS document_type VARCHAR(20),
ADD COLUMN IF NOT EXISTS document_number VARCHAR(50);

-- Añadir campos de contacto adicionales
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS phone_country_code VARCHAR(5),
ADD COLUMN IF NOT EXISTS secondary_email VARCHAR(200);

-- Añadir campos de dirección
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(200),
ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(200),
ADD COLUMN IF NOT EXISTS city VARCHAR(100),
ADD COLUMN IF NOT EXISTS state_province VARCHAR(100),
ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS country VARCHAR(50),
ADD COLUMN IF NOT EXISTS timezone VARCHAR(50),
ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10);

-- Añadir campos de información profesional
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS company_name VARCHAR(200),
ADD COLUMN IF NOT EXISTS job_title VARCHAR(100),
ADD COLUMN IF NOT EXISTS department VARCHAR(100),
ADD COLUMN IF NOT EXISTS website VARCHAR(200);

-- Añadir campos de redes sociales
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS linkedin_profile VARCHAR(200),
ADD COLUMN IF NOT EXISTS twitter_handle VARCHAR(50),
ADD COLUMN IF NOT EXISTS facebook_profile VARCHAR(200),
ADD COLUMN IF NOT EXISTS instagram_profile VARCHAR(200);

-- Añadir campos de contacto de emergencia
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(200),
ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(20),
ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(50);

-- Añadir campos de notas y estado
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS notes TEXT,
ADD COLUMN IF NOT EXISTS is_vip BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Añadir campos de preferencias
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS preferred_contact_method VARCHAR(20),
ADD COLUMN IF NOT EXISTS preferred_contact_time VARCHAR(20),
ADD COLUMN IF NOT EXISTS marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS data_processing_consent BOOLEAN NOT NULL DEFAULT FALSE;

-- Añadir campos de seguimiento
ALTER TABLE chatbotia.contact 
ADD COLUMN IF NOT EXISTS last_contacted_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS total_interactions INTEGER NOT NULL DEFAULT 0;

-- Crear índices para optimización
CREATE INDEX IF NOT EXISTS idx_contact_first_name ON chatbotia.contact(first_name);
CREATE INDEX IF NOT EXISTS idx_contact_last_name ON chatbotia.contact(last_name);
CREATE INDEX IF NOT EXISTS idx_contact_email ON chatbotia.contact(email);
CREATE INDEX IF NOT EXISTS idx_contact_phone_e164 ON chatbotia.contact(phone_e164);
CREATE INDEX IF NOT EXISTS idx_contact_is_vip ON chatbotia.contact(is_vip);
CREATE INDEX IF NOT EXISTS idx_contact_is_active ON chatbotia.contact(is_active);
CREATE INDEX IF NOT EXISTS idx_contact_is_blocked ON chatbotia.contact(is_blocked);
CREATE INDEX IF NOT EXISTS idx_contact_company_name ON chatbotia.contact(company_name);
CREATE INDEX IF NOT EXISTS idx_contact_city ON chatbotia.contact(city);
CREATE INDEX IF NOT EXISTS idx_contact_country ON chatbotia.contact(country);

-- Índices compuestos para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_contact_client_active ON chatbotia.contact(client_id, is_active);
CREATE INDEX IF NOT EXISTS idx_contact_client_vip ON chatbotia.contact(client_id, is_vip);
CREATE INDEX IF NOT EXISTS idx_contact_client_blocked ON chatbotia.contact(client_id, is_blocked);

-- Comentarios en las nuevas columnas
COMMENT ON COLUMN chatbotia.contact.first_name IS 'Nombre de pila de la persona';
COMMENT ON COLUMN chatbotia.contact.last_name IS 'Apellido de la persona';
COMMENT ON COLUMN chatbotia.contact.middle_name IS 'Segundo nombre de la persona';
COMMENT ON COLUMN chatbotia.contact.title IS 'Título de cortesía (Sr., Sra., Dr., etc.)';
COMMENT ON COLUMN chatbotia.contact.gender IS 'Género (MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY)';
COMMENT ON COLUMN chatbotia.contact.birth_date IS 'Fecha de nacimiento';
COMMENT ON COLUMN chatbotia.contact.nationality IS 'Nacionalidad';
COMMENT ON COLUMN chatbotia.contact.document_type IS 'Tipo de documento (DNI, PASSPORT, CEDULA, etc.)';
COMMENT ON COLUMN chatbotia.contact.document_number IS 'Número de documento';
COMMENT ON COLUMN chatbotia.contact.phone_country_code IS 'Código de país del teléfono';
COMMENT ON COLUMN chatbotia.contact.secondary_email IS 'Email secundario';
COMMENT ON COLUMN chatbotia.contact.address_line1 IS 'Primera línea de dirección';
COMMENT ON COLUMN chatbotia.contact.address_line2 IS 'Segunda línea de dirección';
COMMENT ON COLUMN chatbotia.contact.city IS 'Ciudad';
COMMENT ON COLUMN chatbotia.contact.state_province IS 'Estado o provincia';
COMMENT ON COLUMN chatbotia.contact.postal_code IS 'Código postal';
COMMENT ON COLUMN chatbotia.contact.country IS 'País';
COMMENT ON COLUMN chatbotia.contact.timezone IS 'Zona horaria';
COMMENT ON COLUMN chatbotia.contact.preferred_language IS 'Idioma preferido';
COMMENT ON COLUMN chatbotia.contact.company_name IS 'Nombre de la empresa';
COMMENT ON COLUMN chatbotia.contact.job_title IS 'Cargo o puesto de trabajo';
COMMENT ON COLUMN chatbotia.contact.department IS 'Departamento';
COMMENT ON COLUMN chatbotia.contact.website IS 'Sitio web personal o profesional';
COMMENT ON COLUMN chatbotia.contact.linkedin_profile IS 'Perfil de LinkedIn';
COMMENT ON COLUMN chatbotia.contact.twitter_handle IS 'Handle de Twitter';
COMMENT ON COLUMN chatbotia.contact.facebook_profile IS 'Perfil de Facebook';
COMMENT ON COLUMN chatbotia.contact.instagram_profile IS 'Perfil de Instagram';
COMMENT ON COLUMN chatbotia.contact.emergency_contact_name IS 'Nombre del contacto de emergencia';
COMMENT ON COLUMN chatbotia.contact.emergency_contact_phone IS 'Teléfono del contacto de emergencia';
COMMENT ON COLUMN chatbotia.contact.emergency_contact_relationship IS 'Relación con el contacto de emergencia';
COMMENT ON COLUMN chatbotia.contact.notes IS 'Notas adicionales sobre el contacto';
COMMENT ON COLUMN chatbotia.contact.is_vip IS 'Indica si es un contacto VIP';
COMMENT ON COLUMN chatbotia.contact.is_blocked IS 'Indica si el contacto está bloqueado';
COMMENT ON COLUMN chatbotia.contact.is_active IS 'Indica si el contacto está activo';
COMMENT ON COLUMN chatbotia.contact.preferred_contact_method IS 'Método de contacto preferido (EMAIL, PHONE, WHATSAPP, SMS)';
COMMENT ON COLUMN chatbotia.contact.preferred_contact_time IS 'Horario de contacto preferido (MORNING, AFTERNOON, EVENING, ANYTIME)';
COMMENT ON COLUMN chatbotia.contact.marketing_consent IS 'Consentimiento para marketing';
COMMENT ON COLUMN chatbotia.contact.data_processing_consent IS 'Consentimiento para procesamiento de datos';
COMMENT ON COLUMN chatbotia.contact.last_contacted_at IS 'Última vez que se contactó al usuario';
COMMENT ON COLUMN chatbotia.contact.total_interactions IS 'Total de interacciones con el contacto';

-- Crear vista para contactos VIP
CREATE OR REPLACE VIEW chatbotia.contacts_vip AS
SELECT 
    c.*,
    cl.name as client_name
FROM chatbotia.contact c
JOIN chatbotia.client cl ON c.client_id = cl.id
WHERE c.is_vip = true AND c.is_active = true;

COMMENT ON VIEW chatbotia.contacts_vip IS 'Vista de contactos VIP activos';

-- Crear vista para estadísticas de contactos por cliente
CREATE OR REPLACE VIEW chatbotia.contact_stats_by_client AS
SELECT 
    client_id,
    COUNT(*) as total_contacts,
    COUNT(*) FILTER (WHERE is_vip = true) as vip_contacts,
    COUNT(*) FILTER (WHERE is_active = true) as active_contacts,
    COUNT(*) FILTER (WHERE is_blocked = true) as blocked_contacts,
    SUM(total_interactions) as total_interactions,
    AVG(total_interactions) as avg_interactions_per_contact
FROM chatbotia.contact
GROUP BY client_id;

COMMENT ON VIEW chatbotia.contact_stats_by_client IS 'Estadísticas de contactos agrupadas por cliente';

-- Función para obtener el nombre completo de un contacto
CREATE OR REPLACE FUNCTION get_contact_full_name(contact_id UUID)
RETURNS TEXT AS $$
DECLARE
    full_name TEXT;
BEGIN
    SELECT 
        CASE 
            WHEN first_name IS NOT NULL AND last_name IS NOT NULL THEN 
                first_name || ' ' || last_name
            WHEN first_name IS NOT NULL THEN first_name
            WHEN last_name IS NOT NULL THEN last_name
            ELSE display_name
        END
    INTO full_name
    FROM chatbotia.contact
    WHERE id = contact_id;
    
    RETURN full_name;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_contact_full_name IS 'Obtiene el nombre completo de un contacto';

-- Función para obtener la dirección completa de un contacto
CREATE OR REPLACE FUNCTION get_contact_full_address(contact_id UUID)
RETURNS TEXT AS $$
DECLARE
    full_address TEXT;
BEGIN
    SELECT 
        CASE 
            WHEN address_line1 IS NOT NULL THEN
                address_line1 || 
                CASE WHEN address_line2 IS NOT NULL THEN ', ' || address_line2 ELSE '' END ||
                CASE WHEN city IS NOT NULL THEN ', ' || city ELSE '' END ||
                CASE WHEN state_province IS NOT NULL THEN ', ' || state_province ELSE '' END ||
                CASE WHEN postal_code IS NOT NULL THEN ' ' || postal_code ELSE '' END ||
                CASE WHEN country IS NOT NULL THEN ', ' || country ELSE '' END
            ELSE NULL
        END
    INTO full_address
    FROM chatbotia.contact
    WHERE id = contact_id;
    
    RETURN full_address;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_contact_full_address IS 'Obtiene la dirección completa de un contacto';

-- Insertar datos de ejemplo para contactos
/*
INSERT INTO chatbotia.contact (
    id,
    client_id,
    display_name,
    first_name,
    last_name,
    title,
    gender,
    phone_e164,
    email,
    city,
    country,
    is_vip,
    is_active,
    preferred_contact_method,
    marketing_consent,
    data_processing_consent,
    tags,
    created_at,
    updated_at
) VALUES 
(
    gen_random_uuid(),
    (SELECT id FROM chatbotia.client LIMIT 1),
    'Juan Carlos Pérez',
    'Juan Carlos',
    'Pérez',
    'Sr.',
    'MALE',
    '+525512345678',
    'juan.perez@email.com',
    'Ciudad de México',
    'México',
    true,
    true,
    'WHATSAPP',
    true,
    true,
    ARRAY['VIP', 'TECH', 'PREMIUM'],
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    (SELECT id FROM chatbotia.client LIMIT 1),
    'María González',
    'María',
    'González',
    'Sra.',
    'FEMALE',
    '+525512345679',
    'maria.gonzalez@email.com',
    'Guadalajara',
    'México',
    false,
    true,
    'EMAIL',
    true,
    true,
    ARRAY['STANDARD'],
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

 */