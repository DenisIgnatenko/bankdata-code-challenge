package com.bankdata.analytics.application;

import com.bankdata.analytics.persistence.AccountEventEntity;
import com.bankdata.analytics.persistence.AccountEventRepository;
import com.bankdata.contracts.events.AccountEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.io.IOException;

@ApplicationScoped
public class AccountEventDBService {
    private static final Logger LOG = Logger.getLogger(AccountEventDBService.class);

    private final ObjectMapper mapper;
    private final AccountEventRepository repository;

    @Inject
    public AccountEventDBService(ObjectMapper mapper, AccountEventRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    @Transactional
    public void ingest(String json) {
        final AccountEvent event = parse(json);

        if (repository.existsByEventId(event.eventId())) {
            LOG.debugf("Duplicate AccountEvent ignored: eventId=%s type=%s", event.eventId(), event.type());
            return;
        }

        AccountEventEntity entity = AccountEventEntity.fromContract(event, json);

        try {
            repository.persist(entity);
        } catch (PersistenceException error) {
            if (isUniqueViolation(error)) {
                LOG.debugf("Duplicate AccountEvent (db constraint) ignored: eventId=%s", event.eventId());
                return;
            }
            throw error;
        }

        LOG.infof("Stored AccountEvent: eventId=%s type=%s occurredAt=%s", event.eventId(), event.type(), event.occurredAt());
    }

    private AccountEvent parse(String json) {
        try {
            return mapper.readValue(json, AccountEvent.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid AccountEvent JSON payload", exception);
        }
    }

    private static boolean isUniqueViolation(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}