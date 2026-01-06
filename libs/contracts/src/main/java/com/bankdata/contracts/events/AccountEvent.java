package com.bankdata.contracts.events;

import java.time.Instant;
import java.util.UUID;

//one event-format for account-domain
public record AccountEvent(
        UUID eventId,
        Instant occurredAt,
        AccountEventType type,
        String accountNumber,
        String fromAccountNumber,
        String toAccountNumber,
        String amount,
        String balance
) {
    public static AccountEvent created(String accountNumber, String initialBalance) {
        return new AccountEvent(
                UUID.randomUUID(),
                Instant.now(),
                AccountEventType.ACCOUNT_CREATED,
                accountNumber,
                null,
                null,
                initialBalance,
                initialBalance
        );
    }

    public static AccountEvent deposited(String accountNumber, String amount, String newBalance) {
        return new AccountEvent(
                UUID.randomUUID(),
                Instant.now(),
                AccountEventType.MONEY_DEPOSITED,
                accountNumber,
                null,
                null,
                amount,
                newBalance
        );
    }

    public static AccountEvent transferred(String from, String to, String amount) {
        return new AccountEvent(
                UUID.randomUUID(),
                Instant.now(),
                AccountEventType.MONEY_TRANSFERRED,
                null,
                from,
                to,
                amount,
                null
        );
    }

}
