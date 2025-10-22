-- V7__create_categories_and_tags_tables.sql

-- Crear tabla de categorías
CREATE TABLE chatbotia.category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    color VARCHAR(7), -- Hex color code
    icon VARCHAR(50), -- Icon name or class
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Crear tabla de etiquetas
CREATE TABLE chatbotia.tag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    color VARCHAR(7), -- Hex color code
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    usage_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Crear tabla de rompimiento para contact_category
CREATE TABLE chatbotia.contact_category (
    contact_id UUID NOT NULL,
    category_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (contact_id, category_id),
    FOREIGN KEY (contact_id) REFERENCES chatbotia.contact(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES chatbotia.category(id) ON DELETE CASCADE
);

-- Crear tabla de rompimiento para contact_tag
CREATE TABLE chatbotia.contact_tag (
    contact_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (contact_id, tag_id),
    FOREIGN KEY (contact_id) REFERENCES chatbotia.contact(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES chatbotia.tag(id) ON DELETE CASCADE
);

-- Crear índices para optimizar consultas
CREATE INDEX idx_category_name ON chatbotia.category (name);
CREATE INDEX idx_category_is_active ON chatbotia.category (is_active);
CREATE INDEX idx_category_sort_order ON chatbotia.category (sort_order);

CREATE INDEX idx_tag_name ON chatbotia.tag (name);
CREATE INDEX idx_tag_is_active ON chatbotia.tag (is_active);
CREATE INDEX idx_tag_usage_count ON chatbotia.tag (usage_count);

CREATE INDEX idx_contact_category_contact_id ON chatbotia.contact_category (contact_id);
CREATE INDEX idx_contact_category_category_id ON chatbotia.contact_category (category_id);

CREATE INDEX idx_contact_tag_contact_id ON chatbotia.contact_tag (contact_id);
CREATE INDEX idx_contact_tag_tag_id ON chatbotia.contact_tag (tag_id);

-- Crear función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_category_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_tag_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Crear triggers para actualizar updated_at
CREATE TRIGGER trigger_update_category_updated_at
    BEFORE UPDATE ON chatbotia.category
    FOR EACH ROW
    EXECUTE FUNCTION update_category_updated_at();

CREATE TRIGGER trigger_update_tag_updated_at
    BEFORE UPDATE ON chatbotia.tag
    FOR EACH ROW
    EXECUTE FUNCTION update_tag_updated_at();

-- Crear función para actualizar usage_count de tags automáticamente
CREATE OR REPLACE FUNCTION update_tag_usage_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- Incrementar usage_count cuando se añade una etiqueta a un contacto
        UPDATE chatbotia.tag 
        SET usage_count = usage_count + 1 
        WHERE id = NEW.tag_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        -- Decrementar usage_count cuando se quita una etiqueta de un contacto
        UPDATE chatbotia.tag 
        SET usage_count = GREATEST(usage_count - 1, 0) 
        WHERE id = OLD.tag_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Crear trigger para actualizar usage_count automáticamente
CREATE TRIGGER trigger_update_tag_usage_count
    AFTER INSERT OR DELETE ON chatbotia.contact_tag
    FOR EACH ROW
    EXECUTE FUNCTION update_tag_usage_count();

-- Crear vistas útiles
CREATE VIEW chatbotia.categories_with_contact_count AS
SELECT 
    c.id,
    c.name,
    c.description,
    c.color,
    c.icon,
    c.is_active,
    c.sort_order,
    COUNT(cc.contact_id) as contact_count,
    c.created_at,
    c.updated_at
FROM chatbotia.category c
LEFT JOIN chatbotia.contact_category cc ON c.id = cc.category_id
GROUP BY c.id, c.name, c.description, c.color, c.icon, c.is_active, c.sort_order, c.created_at, c.updated_at
ORDER BY c.sort_order ASC, c.name ASC;

CREATE VIEW chatbotia.tags_with_contact_count AS
SELECT 
    t.id,
    t.name,
    t.description,
    t.color,
    t.is_active,
    t.usage_count,
    COUNT(ct.contact_id) as contact_count,
    t.created_at,
    t.updated_at
FROM chatbotia.tag t
LEFT JOIN chatbotia.contact_tag ct ON t.id = ct.tag_id
GROUP BY t.id, t.name, t.description, t.color, t.is_active, t.usage_count, t.created_at, t.updated_at
ORDER BY t.usage_count DESC, t.name ASC;

-- Crear función para obtener estadísticas de categorías
CREATE OR REPLACE FUNCTION get_category_stats()
RETURNS TABLE (
    total_categories BIGINT,
    active_categories BIGINT,
    inactive_categories BIGINT,
    categories_with_contacts BIGINT,
    total_contact_categorizations BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_categories,
        COUNT(*) FILTER (WHERE is_active = true) as active_categories,
        COUNT(*) FILTER (WHERE is_active = false) as inactive_categories,
        COUNT(DISTINCT cc.category_id) as categories_with_contacts,
        COUNT(cc.contact_id) as total_contact_categorizations
    FROM chatbotia.category c
    LEFT JOIN chatbotia.contact_category cc ON c.id = cc.category_id;
END;
$$ LANGUAGE plpgsql;

-- Crear función para obtener estadísticas de etiquetas
CREATE OR REPLACE FUNCTION get_tag_stats()
RETURNS TABLE (
    total_tags BIGINT,
    active_tags BIGINT,
    inactive_tags BIGINT,
    tags_with_contacts BIGINT,
    total_contact_taggings BIGINT,
    avg_usage_count NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_tags,
        COUNT(*) FILTER (WHERE is_active = true) as active_tags,
        COUNT(*) FILTER (WHERE is_active = false) as inactive_tags,
        COUNT(DISTINCT ct.tag_id) as tags_with_contacts,
        COUNT(ct.contact_id) as total_contact_taggings,
        ROUND(AVG(usage_count), 2) as avg_usage_count
    FROM chatbotia.tag t
    LEFT JOIN chatbotia.contact_tag ct ON t.id = ct.tag_id;
END;
$$ LANGUAGE plpgsql;

-- Insertar datos de ejemplo
INSERT INTO chatbotia.category (name, description, color, icon, sort_order) VALUES
('Clientes VIP', 'Clientes con alto valor y prioridad', '#FF6B35', 'star', 1),
('Prospectos', 'Contactos potenciales en proceso de conversión', '#4ECDC4', 'user-plus', 2),
('Clientes Activos', 'Clientes que realizan compras regularmente', '#45B7D1', 'users', 3),
('Clientes Inactivos', 'Clientes que no han comprado recientemente', '#96CEB4', 'user-minus', 4),
('Soporte', 'Contactos que requieren atención de soporte', '#FFEAA7', 'headphones', 5);

INSERT INTO chatbotia.tag (name, description, color) VALUES
('Lead Caliente', 'Contactos con alta probabilidad de conversión', '#E74C3C'),
('Lead Tibio', 'Contactos con probabilidad media de conversión', '#F39C12'),
('Lead Frío', 'Contactos con baja probabilidad de conversión', '#95A5A6'),
('Interesado en Producto A', 'Contactos interesados en el producto A', '#3498DB'),
('Interesado en Producto B', 'Contactos interesados en el producto B', '#9B59B6'),
('Presupuesto Alto', 'Contactos con presupuesto alto', '#27AE60'),
('Presupuesto Medio', 'Contactos con presupuesto medio', '#F1C40F'),
('Presupuesto Bajo', 'Contactos con presupuesto bajo', '#E67E22'),
('Urgente', 'Contactos que requieren atención urgente', '#E74C3C'),
('Seguimiento Pendiente', 'Contactos que requieren seguimiento', '#F39C12');

-- Comentarios de documentación
COMMENT ON TABLE chatbotia.category IS 'Categorías para clasificar contactos';
COMMENT ON TABLE chatbotia.tag IS 'Etiquetas para etiquetar contactos';
COMMENT ON TABLE chatbotia.contact_category IS 'Tabla de rompimiento entre contactos y categorías';
COMMENT ON TABLE chatbotia.contact_tag IS 'Tabla de rompimiento entre contactos y etiquetas';

COMMENT ON COLUMN chatbotia.category.color IS 'Código de color hexadecimal (ej: #FF6B35)';
COMMENT ON COLUMN chatbotia.category.icon IS 'Nombre del icono o clase CSS';
COMMENT ON COLUMN chatbotia.category.sort_order IS 'Orden de clasificación para mostrar las categorías';

COMMENT ON COLUMN chatbotia.tag.color IS 'Código de color hexadecimal (ej: #E74C3C)';
COMMENT ON COLUMN chatbotia.tag.usage_count IS 'Contador automático de cuántas veces se ha usado esta etiqueta';
