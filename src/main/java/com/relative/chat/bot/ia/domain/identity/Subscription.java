package com.relative.chat.bot.ia.domain.identity;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.types.*;
import java.time.*;
import java.util.*;

public final class Subscription {
    
    private final UuidId<Subscription> id;
    private final UuidId<Client> clientId;
    private final String planCode;
    private SubscriptionStatus status;
    private final LocalDate startDate;
    private LocalDate endDate;
    private LocalDate cancelAt;
    
    public Subscription(UuidId<Subscription> id, UuidId<Client> clientId, String planCode, LocalDate startDate) {
        this.id = Objects.requireNonNull(id);
        this.clientId = Objects.requireNonNull(clientId);
        this.planCode = Objects.requireNonNull(planCode);
        this.startDate = Objects.requireNonNull(startDate);
        this.status = SubscriptionStatus.ACTIVE;
    }
    
    public void cancel(LocalDate at) {
        this.cancelAt = at;
        this.status = SubscriptionStatus.CANCELED;
    }
    
    public UuidId<Subscription> id() {
        return id;
    }
    
    public UuidId<Client> clientId() {
        return clientId;
    }
    
    public String planCode() {
        return planCode;
    }
    
    public SubscriptionStatus status() {
        return status;
    }
    
    public LocalDate startDate() {
        return startDate;
    }
    
    public Optional<LocalDate> endDate() {
        return Optional.ofNullable(endDate);
    }
    
    public Optional<LocalDate> cancelAt() {
        return Optional.ofNullable(cancelAt);
    }
}