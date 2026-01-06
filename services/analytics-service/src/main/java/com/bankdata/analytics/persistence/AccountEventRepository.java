package com.bankdata.analytics.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class AccountEventRepository implements PanacheRepository<AccountEventEntity> {

    public boolean existsByEventId(UUID eventId) {
        return count("eventId", eventId) > 0;
    }
}