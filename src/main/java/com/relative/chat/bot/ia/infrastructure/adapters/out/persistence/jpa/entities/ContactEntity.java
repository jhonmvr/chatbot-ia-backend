package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;


import jakarta.persistence.*;

import lombok.Getter;

import lombok.Setter;

import org.hibernate.annotations.ColumnDefault;

import org.hibernate.annotations.JdbcTypeCode;

import org.hibernate.annotations.OnDelete;

import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.type.SqlTypes;


import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "contact", schema = "chatbotia")
public class ContactEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity clientEntity;


    @Column(name = "external_id", length = 120)
    private String externalId;


    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "title", length = 50)
    private String title; // Sr., Sra., Dr., etc.

    @Column(name = "gender", length = 20)
    private String gender; // MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY

    @Column(name = "birth_date")
    private OffsetDateTime birthDate;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Column(name = "document_type", length = 20)
    private String documentType; // DNI, PASSPORT, CEDULA, etc.

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "phone_e164", length = 20)
    private String phoneE164;

    @Column(name = "phone_country_code", length = 5)
    private String phoneCountryCode;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "secondary_email", length = 200)
    private String secondaryEmail;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "locale", length = 10)
    private String locale;

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "linkedin_profile", length = 200)
    private String linkedinProfile;

    @Column(name = "twitter_handle", length = 50)
    private String twitterHandle;

    @Column(name = "facebook_profile", length = 200)
    private String facebookProfile;

    @Column(name = "instagram_profile", length = 200)
    private String instagramProfile;

    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_vip", nullable = false)
    @ColumnDefault("false")
    private Boolean isVip = false;

    @Column(name = "is_blocked", nullable = false)
    @ColumnDefault("false")
    private Boolean isBlocked = false;

    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private Boolean isActive = true;

    @Column(name = "preferred_contact_method", length = 20)
    private String preferredContactMethod; // EMAIL, PHONE, WHATSAPP, SMS

    @Column(name = "preferred_contact_time", length = 20)
    private String preferredContactTime; // MORNING, AFTERNOON, EVENING, ANYTIME

    @Column(name = "marketing_consent", nullable = false)
    @ColumnDefault("false")
    private Boolean marketingConsent = false;

    @Column(name = "data_processing_consent", nullable = false)
    @ColumnDefault("false")
    private Boolean dataProcessingConsent = false;

    @ColumnDefault("'{}'::jsonb")
    @Column(name = "attributes", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> attributes;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "last_contacted_at")
    private OffsetDateTime lastContactedAt;

    @Column(name = "total_interactions", nullable = false)
    @ColumnDefault("0")
    private Integer totalInteractions = 0;


    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Relaciones many-to-many con Category y Tag
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "contact_category",
        schema = "chatbotia",
        joinColumns = @JoinColumn(name = "contact_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<CategoryEntity> categories = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "contact_tag",
        schema = "chatbotia",
        joinColumns = @JoinColumn(name = "contact_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<TagEntity> tags = new ArrayList<>();

}