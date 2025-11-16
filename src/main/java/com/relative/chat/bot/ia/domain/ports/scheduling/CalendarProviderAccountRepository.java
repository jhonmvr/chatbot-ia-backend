package com.relative.chat.bot.ia.domain.ports.scheduling;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para CalendarProviderAccount
 */
public interface CalendarProviderAccountRepository {
    
    /**
     * Busca una cuenta por ID
     */
    Optional<CalendarProviderAccount> findById(UuidId<CalendarProviderAccount> id);
    
    /**
     * Busca cuentas por client_id
     */
    List<CalendarProviderAccount> findByClientId(UuidId<Client> clientId);
    
    /**
     * Busca cuentas activas por client_id
     */
    List<CalendarProviderAccount> findActiveByClientId(UuidId<Client> clientId);
    
    /**
     * Busca cuenta activa por client_id y provider
     */
    Optional<CalendarProviderAccount> findActiveByClientIdAndProvider(UuidId<Client> clientId, CalendarProvider provider);
    
    /**
     * Busca cuenta por client_id y provider (sin importar si está activa)
     */
    Optional<CalendarProviderAccount> findByClientIdAndProvider(UuidId<Client> clientId, CalendarProvider provider);
    
    /**
     * Busca cuenta por account_email y provider
     */
    Optional<CalendarProviderAccount> findByAccountEmailAndProvider(String accountEmail, CalendarProvider provider);
    
    /**
     * Guarda una cuenta (crea o actualiza)
     */
    void save(CalendarProviderAccount account);
    
    /**
     * Elimina una cuenta
     */
    void delete(UuidId<CalendarProviderAccount> id);
    
    /**
     * Verifica si existe cuenta activa para un cliente y proveedor
     */
    boolean existsActiveByClientIdAndProvider(UuidId<Client> clientId, CalendarProvider provider);
}

