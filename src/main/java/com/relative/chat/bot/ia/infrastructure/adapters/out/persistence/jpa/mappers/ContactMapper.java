package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.TagEntity;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;

public final class ContactMapper {
    
    private ContactMapper() {}
    
    public static Contact toDomain(ContactEntity e) {
        if (e == null) return null;
        
        return Contact.existing(
                MappingHelpers.toUuidId(e.getId()),
                e.getClientEntity() != null ? MappingHelpers.toUuidId(e.getClientEntity().getId()) : null,
                e.getExternalId(),
                e.getDisplayName(),
                e.getFirstName(),
                e.getLastName(),
                e.getMiddleName(),
                e.getTitle(),
                e.getGender(),
                e.getBirthDate() != null ? e.getBirthDate().toInstant() : null,
                e.getNationality(),
                e.getDocumentType(),
                e.getDocumentNumber(),
                e.getPhoneE164() != null ? MappingHelpers.phone(e.getPhoneE164()) : null,
                e.getPhoneCountryCode(),
                e.getEmail() != null ? MappingHelpers.email(e.getEmail()) : null,
                e.getSecondaryEmail() != null ? MappingHelpers.email(e.getSecondaryEmail()) : null,
                e.getAddressLine1(),
                e.getAddressLine2(),
                e.getCity(),
                e.getStateProvince(),
                e.getPostalCode(),
                e.getCountry(),
                e.getTimezone(),
                e.getLocale(),
                e.getPreferredLanguage(),
                e.getCompanyName(),
                e.getJobTitle(),
                e.getDepartment(),
                e.getWebsite(),
                e.getLinkedinProfile(),
                e.getTwitterHandle(),
                e.getFacebookProfile(),
                e.getInstagramProfile(),
                e.getEmergencyContactName(),
                e.getEmergencyContactPhone(),
                e.getEmergencyContactRelationship(),
                e.getNotes(),
                e.getIsVip(),
                e.getIsBlocked(),
                e.getIsActive(),
                e.getPreferredContactMethod(),
                e.getPreferredContactTime(),
                e.getMarketingConsent(),
                e.getDataProcessingConsent(),
                e.getTags().stream().map(TagEntity::getName).toList(),
                e.getAttributes() != null ? e.getAttributes() : new HashMap<>(),
                e.getLastSeenAt() != null ? e.getLastSeenAt().toInstant() : null,
                e.getLastContactedAt() != null ? e.getLastContactedAt().toInstant() : null,
                e.getTotalInteractions(),
                e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
                e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null
        );
    }
    
    public static ContactEntity toEntity(Contact d) {
        if (d == null) return null;
        
        ContactEntity e = new ContactEntity();
        e.setId(d.id() != null ? d.id().value() : null);
        e.setExternalId(d.externalId());
        e.setDisplayName(d.displayName());
        e.setFirstName(d.firstName());
        e.setLastName(d.lastName());
        e.setMiddleName(d.middleName());
        e.setTitle(d.title());
        e.setGender(d.gender());
        e.setBirthDate(d.birthDate() != null ? OffsetDateTime.ofInstant(d.birthDate(), ZoneOffset.UTC) : null);
        e.setNationality(d.nationality());
        e.setDocumentType(d.documentType());
        e.setDocumentNumber(d.documentNumber());
        e.setPhoneE164(d.phoneE164() != null ? d.phoneE164().value() : null);
        e.setPhoneCountryCode(d.phoneCountryCode());
        e.setEmail(d.email() != null ? d.email().value() : null);
        e.setSecondaryEmail(d.secondaryEmail() != null ? d.secondaryEmail().value() : null);
        e.setAddressLine1(d.addressLine1());
        e.setAddressLine2(d.addressLine2());
        e.setCity(d.city());
        e.setStateProvince(d.stateProvince());
        e.setPostalCode(d.postalCode());
        e.setCountry(d.country());
        e.setTimezone(d.timezone());
        e.setLocale(d.locale());
        e.setPreferredLanguage(d.preferredLanguage());
        e.setCompanyName(d.companyName());
        e.setJobTitle(d.jobTitle());
        e.setDepartment(d.department());
        e.setWebsite(d.website());
        e.setLinkedinProfile(d.linkedinProfile());
        e.setTwitterHandle(d.twitterHandle());
        e.setFacebookProfile(d.facebookProfile());
        e.setInstagramProfile(d.instagramProfile());
        e.setEmergencyContactName(d.emergencyContactName());
        e.setEmergencyContactPhone(d.emergencyContactPhone());
        e.setEmergencyContactRelationship(d.emergencyContactRelationship());
        e.setNotes(d.notes());
        e.setIsVip(d.isVip());
        e.setIsBlocked(d.isBlocked());
        e.setIsActive(d.isActive());
        e.setPreferredContactMethod(d.preferredContactMethod());
        e.setPreferredContactTime(d.preferredContactTime());
        e.setMarketingConsent(d.marketingConsent());
        e.setDataProcessingConsent(d.dataProcessingConsent());
        // tagNames ya no existe, usamos la relación many-to-many con tags
        e.setAttributes(d.attributes() != null ? d.attributes() : new HashMap<>());
        e.setLastSeenAt(d.lastSeenAt() != null ? OffsetDateTime.ofInstant(d.lastSeenAt(), ZoneOffset.UTC) : null);
        e.setLastContactedAt(d.lastContactedAt() != null ? OffsetDateTime.ofInstant(d.lastContactedAt(), ZoneOffset.UTC) : null);
        e.setTotalInteractions(d.totalInteractions());
        e.setCreatedAt(d.createdAt() != null ? OffsetDateTime.ofInstant(d.createdAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        e.setUpdatedAt(d.updatedAt() != null ? OffsetDateTime.ofInstant(d.updatedAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        
        return e;
    }
    
    /**
     * Versión que necesita EntityManager para setear la relación con Client
     */
    public static ContactEntity toEntity(Contact d, EntityManager em) {
        ContactEntity e = toEntity(d);
        if (e != null && d.clientId() != null) {
            e.setClientEntity(em.getReference(ClientEntity.class, d.clientId().value()));
        }
        return e;
    }
}
