package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.WhatsAppTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para plantillas de WhatsApp
 */
@Repository
public interface WhatsAppTemplateJpa extends JpaRepository<WhatsAppTemplateEntity, UUID> {
    
    /**
     * Busca plantillas por ID del número de teléfono cliente
     */
    List<WhatsAppTemplateEntity> findByClientPhoneEntityId(UUID clientPhoneId);
    
    /**
     * Busca plantillas por estado
     */
    List<WhatsAppTemplateEntity> findByStatus(String status);
    
    /**
     * Busca plantillas por categoría
     */
    List<WhatsAppTemplateEntity> findByCategory(String category);
    
    /**
     * Busca plantillas por cliente y estado
     */
    List<WhatsAppTemplateEntity> findByClientPhoneEntityIdAndStatus(UUID clientPhoneId, String status);
    
    /**
     * Busca plantillas por cliente y categoría
     */
    List<WhatsAppTemplateEntity> findByClientPhoneEntityIdAndCategory(UUID clientPhoneId, String category);
    
    /**
     * Busca plantilla por nombre, cliente y categoría
     */
    Optional<WhatsAppTemplateEntity> findByClientPhoneEntityIdAndNameAndCategory(
        UUID clientPhoneId, String name, String category
    );
    
    /**
     * Busca plantilla por nombre y cliente (sin categoría)
     */
    Optional<WhatsAppTemplateEntity> findByClientPhoneEntityIdAndName(UUID clientPhoneId, String name);
    
    /**
     * Busca plantilla por ID de Meta
     */
    Optional<WhatsAppTemplateEntity> findByMetaTemplateId(String metaTemplateId);

    Optional<WhatsAppTemplateEntity> findByName(String name);

    /**
     * Busca plantillas aprobadas y listas para usar
     */
    @Query("SELECT wt FROM WhatsAppTemplateEntity wt WHERE wt.status = 'APPROVED' AND wt.metaTemplateId IS NOT NULL AND wt.metaTemplateId != ''")
    List<WhatsAppTemplateEntity> findReadyTemplates();
    
    /**
     * Busca plantillas pendientes de sincronización
     */
    @Query("SELECT wt FROM WhatsAppTemplateEntity wt WHERE wt.status = 'DRAFT' OR (wt.status = 'PENDING' AND wt.metaTemplateId IS NULL)")
    List<WhatsAppTemplateEntity> findPendingSyncTemplates();
    
    /**
     * Cuenta plantillas por cliente y estado
     */
    @Query("SELECT COUNT(wt) FROM WhatsAppTemplateEntity wt WHERE wt.clientPhoneEntity.id = :clientPhoneId AND wt.status = :status")
    long countByClientPhoneIdAndStatus(@Param("clientPhoneId") UUID clientPhoneId, @Param("status") String status);
    
    /**
     * Cuenta plantillas por cliente y categoría
     */
    @Query("SELECT COUNT(wt) FROM WhatsAppTemplateEntity wt WHERE wt.clientPhoneEntity.id = :clientPhoneId AND wt.category = :category")
    long countByClientPhoneIdAndCategory(@Param("clientPhoneId") UUID clientPhoneId, @Param("category") String category);
    
    /**
     * Busca plantillas por cliente con estadísticas
     */
    @Query("SELECT wt FROM WhatsAppTemplateEntity wt WHERE wt.clientPhoneEntity.id = :clientPhoneId ORDER BY wt.createdAt DESC")
    List<WhatsAppTemplateEntity> findByClientPhoneIdWithStats(@Param("clientPhoneId") UUID clientPhoneId);
    
    /**
     * Busca plantillas que necesitan actualización de estado desde Meta
     */
    @Query("SELECT wt FROM WhatsAppTemplateEntity wt WHERE wt.metaTemplateId IS NOT NULL AND wt.status IN ('PENDING', 'APPROVED')")
    List<WhatsAppTemplateEntity> findTemplatesNeedingStatusUpdate();
}
