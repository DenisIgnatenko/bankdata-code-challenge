package com.bankdata.analytics.messaging;

import com.bankdata.analytics.application.AccountEventIngestionService;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class AccountEventConsumer {

    private final AccountEventIngestionService ingestion;

    @Inject
    public AccountEventConsumer(AccountEventIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @Incoming("words-in")
    public void onMessage(String json) {
        ingestion.ingest(json);
    }
}