package com.bankdata.account.messaging;

import com.bankdata.contracts.events.AccountEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AccountEventPublisher {
    private static final Logger LOG = Logger.getLogger(AccountEventPublisher.class);

    private final MutinyEmitter<String> emitter;
    private final ObjectMapper objectMapper;

    @Inject
    public AccountEventPublisher(@Channel("words-out") MutinyEmitter<String> emitter,
                                 ObjectMapper objectMapper) {
        this.emitter = emitter;
        this.objectMapper = objectMapper;
    }

    public void publish(AccountEvent event) {
        String json = toJson(event);

        emitter.send(json).await().indefinitely();
    }

    public void safePublish(AccountEvent event) {
        try {
            publish(event);
        } catch (Exception exception) {
            LOG.warnf(exception, "Failed to publish AccountEvent. eventId=%s type=%s", event.eventId(), event.type());
        }
    }

    private String toJson(AccountEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AccountEvent to JSON", e);
        }
    }
}