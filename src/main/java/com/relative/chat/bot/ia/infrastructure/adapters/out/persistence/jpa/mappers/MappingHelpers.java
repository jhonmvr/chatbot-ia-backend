package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.common.LongId;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.vo.Email;
import com.relative.chat.bot.ia.domain.vo.PhoneE164;
import com.relative.chat.bot.ia.domain.vo.Money;

import java.math.BigDecimal;
import java.util.UUID;

public final class MappingHelpers {
    
    private MappingHelpers() {}
    
    // === Conversión UUID ===
    
    public static <T> UuidId<T> toUuidId(UUID uuid) {
        return uuid == null ? null : UuidId.of(uuid);
    }
    
    public static UUID toUuid(UuidId<?> id) {
        return id == null ? null : id.value();
    }
    
    // === Conversión Long ===
    
    public static <T> LongId<T> toLongId(Long value) {
        return value == null ? null : LongId.of(value);
    }
    
    public static Long toLong(LongId<?> id) {
        return id == null ? null : id.value();
    }
    
    // === Email ===
    
    public static String email(Email e) {
        return e == null ? null : e.value();
    }
    
    public static Email email(String s) {
        return s == null ? null : new Email(s);
    }
    
    // === Phone ===
    
    public static String phone(PhoneE164 p) {
        return p == null ? null : p.value();
    }
    
    public static PhoneE164 phone(String s) {
        return s == null ? null : new PhoneE164(s);
    }
    
    // === Money ===
    
    public static Money money(BigDecimal amount, String currency) {
        return (amount == null || currency == null) ? null : new Money(amount, currency);
    }
    
    public static BigDecimal moneyAmount(Money m) {
        return m == null ? null : m.amount();
    }
    
    public static String moneyCurrency(Money m) {
        return m == null ? null : m.currency();
    }
}
