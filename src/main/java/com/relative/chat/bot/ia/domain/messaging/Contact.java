package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import com.relative.chat.bot.ia.domain.vo.*;

import java.time.Instant;
import java.util.*;

/**
 * Contacto con información completa de persona
 */
public record Contact(
        UuidId<Contact> id,
        UuidId<Client> clientId,
        String externalId,
        String displayName,
        String firstName,
        String lastName,
        String middleName,
        String title,
        String gender,
        Instant birthDate,
        String nationality,
        String documentType,
        String documentNumber,
        PhoneE164 phoneE164,
        String phoneCountryCode,
        Email email,
        Email secondaryEmail,
        String addressLine1,
        String addressLine2,
        String city,
        String stateProvince,
        String postalCode,
        String country,
        String timezone,
        String locale,
        String preferredLanguage,
        String companyName,
        String jobTitle,
        String department,
        String website,
        String linkedinProfile,
        String twitterHandle,
        String facebookProfile,
        String instagramProfile,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelationship,
        String notes,
        Boolean isVip,
        Boolean isBlocked,
        Boolean isActive,
        String preferredContactMethod,
        String preferredContactTime,
        Boolean marketingConsent,
        Boolean dataProcessingConsent,
        List<String> tagNames,
        Map<String, Object> attributes,
        Instant lastSeenAt,
        Instant lastContactedAt,
        Integer totalInteractions,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Constructor para nuevo contacto
     */
    public static Contact create(
            UuidId<Client> clientId,
            String displayName,
            String firstName,
            String lastName,
            PhoneE164 phoneE164,
            Email email
    ) {
        Instant now = Instant.now();
        return new Contact(
                null,
                clientId,
                null, // externalId
                displayName,
                firstName,
                lastName,
                null, // middleName
                null, // title
                null, // gender
                null, // birthDate
                null, // nationality
                null, // documentType
                null, // documentNumber
                phoneE164,
                null, // phoneCountryCode
                email,
                null, // secondaryEmail
                null, // addressLine1
                null, // addressLine2
                null, // city
                null, // stateProvince
                null, // postalCode
                null, // country
                null, // timezone
                null, // locale
                null, // preferredLanguage
                null, // companyName
                null, // jobTitle
                null, // department
                null, // website
                null, // linkedinProfile
                null, // twitterHandle
                null, // facebookProfile
                null, // instagramProfile
                null, // emergencyContactName
                null, // emergencyContactPhone
                null, // emergencyContactRelationship
                null, // notes
                false, // isVip
                false, // isBlocked
                true, // isActive
                null, // preferredContactMethod
                null, // preferredContactTime
                false, // marketingConsent
                false, // dataProcessingConsent
                new ArrayList<>(), // tags
                new HashMap<>(), // attributes
                null, // lastSeenAt
                null, // lastContactedAt
                0, // totalInteractions
                now,
                now
        );
    }

    /**
     * Constructor para contacto existente
     */
    public static Contact existing(
            UuidId<Contact> id,
            UuidId<Client> clientId,
            String externalId,
            String displayName,
            String firstName,
            String lastName,
            String middleName,
            String title,
            String gender,
            Instant birthDate,
            String nationality,
            String documentType,
            String documentNumber,
            PhoneE164 phoneE164,
            String phoneCountryCode,
            Email email,
            Email secondaryEmail,
            String addressLine1,
            String addressLine2,
            String city,
            String stateProvince,
            String postalCode,
            String country,
            String timezone,
            String locale,
            String preferredLanguage,
            String companyName,
            String jobTitle,
            String department,
            String website,
            String linkedinProfile,
            String twitterHandle,
            String facebookProfile,
            String instagramProfile,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String notes,
            Boolean isVip,
            Boolean isBlocked,
            Boolean isActive,
            String preferredContactMethod,
            String preferredContactTime,
            Boolean marketingConsent,
            Boolean dataProcessingConsent,
            List<String> tagNames,
            Map<String, Object> attributes,
            Instant lastSeenAt,
            Instant lastContactedAt,
            Integer totalInteractions,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Contact(
                id, clientId, externalId, displayName, firstName, lastName, middleName, title, gender,
                birthDate, nationality, documentType, documentNumber, phoneE164, phoneCountryCode,
                email, secondaryEmail, addressLine1, addressLine2, city, stateProvince, postalCode,
                country, timezone, locale, preferredLanguage, companyName, jobTitle, department,
                website, linkedinProfile, twitterHandle, facebookProfile, instagramProfile,
                emergencyContactName, emergencyContactPhone, emergencyContactRelationship, notes,
                isVip, isBlocked, isActive, preferredContactMethod, preferredContactTime,
                marketingConsent, dataProcessingConsent, tagNames, attributes, lastSeenAt,
                lastContactedAt, totalInteractions, createdAt, updatedAt
        );
    }

    /**
     * Obtiene el nombre completo
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) fullName.append(firstName);
        if (lastName != null) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName);
        }
        return fullName.length() > 0 ? fullName.toString() : displayName;
    }

    /**
     * Obtiene la dirección completa
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        if (addressLine1 != null) address.append(addressLine1);
        if (addressLine2 != null) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine2);
        }
        if (city != null) {
            if (address.length() > 0) address.append(", ");
            address.append(city);
        }
        if (stateProvince != null) {
            if (address.length() > 0) address.append(", ");
            address.append(stateProvince);
        }
        if (postalCode != null) {
            if (address.length() > 0) address.append(" ");
            address.append(postalCode);
        }
        if (country != null) {
            if (address.length() > 0) address.append(", ");
            address.append(country);
        }
        return address.toString();
    }

    /**
     * Verifica si el contacto está activo
     */
    public Boolean isActive() {
        return isActive != null && isActive;
    }

    /**
     * Verifica si es VIP
     */
    public Boolean isVip() {
        return isVip != null && isVip;
    }

    /**
     * Verifica si está bloqueado
     */
    public Boolean isBlocked() {
        return isBlocked != null && isBlocked;
    }

    /**
     * Obtiene email como Optional
     */
    public Optional<Email> emailOpt() {
        return Optional.ofNullable(email);
    }

    /**
     * Obtiene email secundario como Optional
     */
    public Optional<Email> secondaryEmailOpt() {
        return Optional.ofNullable(secondaryEmail);
    }

    /**
     * Obtiene teléfono como Optional
     */
    public Optional<PhoneE164> phoneOpt() {
        return Optional.ofNullable(phoneE164);
    }

    /**
     * Obtiene fecha de nacimiento como Optional
     */
    public Optional<Instant> birthDateOpt() {
        return Optional.ofNullable(birthDate);
    }

    /**
     * Obtiene última vez visto como Optional
     */
    public Optional<Instant> lastSeenAtOpt() {
        return Optional.ofNullable(lastSeenAt);
    }

    /**
     * Obtiene última vez contactado como Optional
     */
    public Optional<Instant> lastContactedAtOpt() {
        return Optional.ofNullable(lastContactedAt);
    }
}