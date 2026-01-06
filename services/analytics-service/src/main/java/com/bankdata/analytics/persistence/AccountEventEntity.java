package com.bankdata.analytics.persistence;

import com.bankdata.contracts.events.AccountEvent;
import com.bankdata.contracts.events.AccountEventType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "account_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_event_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_account_events_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_account_events_account_number", columnList = "account_number"),
                @Index(name = "idx_account_events_from_to", columnList = "from_account_number,to_account_number")
        }
)
public class AccountEventEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    // Idempotency key (важно!): одно событие не должно сохраниться дважды
    @Column(name = "event_id", nullable = false, updatable = false, unique = true)
    public UUID eventId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    public Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 64)
    public AccountEventType type;

    @Column(name = "account_number")
    public String accountNumber;

    @Column(name = "from_account_number")
    public String fromAccountNumber;

    @Column(name = "to_account_number")
    public String toAccountNumber;

    @Column(name = "amount", nullable = false, length = 64)
    public String amount;

    @Column(name = "balance", length = 64)
    public String balance;

    @Lob
    @Column(name = "raw_json", nullable = false)
    public String rawJson;

    public static AccountEventEntity fromContract(AccountEvent event, String rawJson) {
        AccountEventEntity entity = new AccountEventEntity();
        entity.eventId = event.eventId();
        entity.occurredAt = event.occurredAt();
        entity.type = event.type();
        entity.accountNumber = event.accountNumber();
        entity.fromAccountNumber = event.fromAccountNumber();
        entity.toAccountNumber = event.toAccountNumber();
        entity.amount = event.amount();
        entity.balance = event.balance();
        entity.rawJson = rawJson;
        return entity;
    }
}