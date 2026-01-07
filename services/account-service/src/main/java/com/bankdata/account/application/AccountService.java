package com.bankdata.account.application;

import com.bankdata.account.api.dto.*;
import com.bankdata.account.api.error.BadRequestException;
import com.bankdata.account.domain.AccountEntity;
import com.bankdata.account.messaging.AccountEventPublisher;
import com.bankdata.account.persistence.AccountRepository;
import com.bankdata.account.support.AccountNumberGenerator;
import com.bankdata.contracts.events.AccountEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ApplicationScoped
public class AccountService {
    //for unique collisions
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final AccountRepository repository;
    private final AccountNumberGenerator generator;
    private final AccountEventPublisher eventPublisher;

    public AccountService(AccountRepository repository,
                          AccountNumberGenerator generator,
                          AccountEventPublisher eventPublisher) {
        this.repository = repository;
        this.generator = generator;
        this.eventPublisher = eventPublisher;
    }

    //!! Atomic operations inside.
    @Transactional
    public CreateAccountResponse create(CreateAccountRequest request) {
        //1. Normalizing money. ZERO amount - acceptable. Scale = 2, no rounding.
        BigDecimal initial = normalizeMoneyAllowZero(
                request.initialDeposit() == null ? BigDecimal.ZERO : request.initialDeposit(),
                "initialDeposit"
        );

        //2. Name check. Deleting spaces, null-check, blank-check. May be better to move to bean validation (@NotBlank)
        String firstName = normalizeName(request.firstName(), "firstName");
        String lastName = normalizeName(request.lastName(), "lastName");

        //3. Retry in possible unique collisions
        //TRAP: we cannot make SELECT to check uniqueness (its a race). Must refer to unique costraint in DB
        for(int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String accountNumber = generator.next(); //generator does not promised uniqueness to us :)

            AccountEntity entity = new AccountEntity(
                    accountNumber,
                    firstName,
                    lastName,
                    initial
            );

            try {
                //persist and flush makes Hibernate to execute SQL Insert NOW.
                //Important that we check uniqueness immediately
                repository.persistAndFlush(entity);

                eventPublisher.safePublish(
                        AccountEvent.created(
                                entity.getAccountNumber(),
                                entity.getBalance().toPlainString()
                        )
                );

                return new CreateAccountResponse(entity.getAccountNumber(), entity.getBalance());
            } catch (PersistenceException exception) {
                if (isUniqueConstraintViolation(exception)) {
                    continue; //if we are in uniqueness violation - starting new attempt.
                }
                //if any other exception - new cycle.
                throw exception;
            }
        }
        throw new IllegalStateException(
                "Unable to generate unique account number after " + MAX_GENERATION_ATTEMPTS + " attempts"
        );
    }

    @Transactional
    public BalanceResponse deposit(String accountNumber, DepositRequest request) {
        requireNonBlank(accountNumber, "accountNumber");

        // deposit amount: must be > 0; scale must be <= 2 without rounding
        BigDecimal amount = normalizeMoneyPositive(request.amount(), "amount");

        AccountEntity entity = repository.getForUpdate(accountNumber);
        entity.deposit(amount);

        eventPublisher.safePublish(
                AccountEvent.deposited(
                        entity.getAccountNumber(),
                        amount.toPlainString(),
                        entity.getBalance().toPlainString()
                )
        );

        return new BalanceResponse(entity.getAccountNumber(), entity.getBalance());
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        String fromAcc = normalizeAccountNumber(request.fromAccountNumber(), "fromAccountNumber");
        String toAcc = normalizeAccountNumber(request.toAccountNumber(), "toAccountNumber");

        if (fromAcc.equals(toAcc)) {
            throw new BadRequestException("fromAccountNumber and toAccountNumber must be different");
        }

        BigDecimal amount = normalizeMoneyPositive(request.amount(), "amount");

        // lock in order to prevent deadlocks: A->B and B-> A
        String firstKey = (fromAcc.compareTo(toAcc) < 0) ? fromAcc : toAcc;
        String secondKey = (fromAcc.compareTo(toAcc) < 0) ? toAcc : fromAcc;

        AccountEntity first = repository.getForUpdate(firstKey);
        AccountEntity second = repository.getForUpdate(secondKey);

        AccountEntity from = fromAcc.equals(first.getAccountNumber()) ? first : second;
        AccountEntity to = fromAcc.equals(first.getAccountNumber()) ? second : first;

        from.withdraw(amount);
        to.deposit(amount);

        eventPublisher.safePublish(
                AccountEvent.transferred(
                        from.getAccountNumber(),
                        to.getAccountNumber(),
                        amount.toPlainString()
                )
        );

        return new TransferResponse(
                from.getAccountNumber(), from.getBalance(),
                to.getAccountNumber(), to.getBalance()
        );
    }


    @Transactional
    public BalanceResponse balance(String accountNumber) {
        requireNonBlank(accountNumber, "accountNumber");
        AccountEntity entity = repository.getByAccountNumber(accountNumber);
        return new BalanceResponse(entity.getAccountNumber(), entity.getBalance());
    }

    // ### Helpers

    private static String normalizeName(String value, String field) {
        if (value == null) {
            throw new BadRequestException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException(field + " must not be blank");
        }
        return trimmed;
    }

    private static String normalizeAccountNumber(String value, String field) {
        if (value == null) {
            throw new BadRequestException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException(field + " must not be blank");
        }
        return trimmed;
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.trim().isBlank()) {
            throw new BadRequestException(field + " is required");
        }
    }

    // Normalize money. Not Null, max 2 decimals, no rounding
    private static BigDecimal normalizeMoneyAllowZero(BigDecimal value, String field) {
        BigDecimal normalized = normalizeScaleNoRounding(value, field);
        if (normalized.signum() < 0) {
            throw new BadRequestException(field + " must be non-negative");
        }
        return normalized;
    }

    private static BigDecimal normalizeMoneyPositive(BigDecimal value, String field) {
        BigDecimal normalized = normalizeScaleNoRounding(value, field);
        if (normalized.signum() <= 0) {
            throw new BadRequestException(field + " must be positive");
        }
        return normalized;
    }

    private static BigDecimal normalizeScaleNoRounding(BigDecimal value, String field) {
        if (value == null) {
            throw new BadRequestException(field + " is required");
        }
        try {
            // UNNECESSARY stops silent rounding
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BadRequestException(field + " must have max 2 decimal places");
        }
    }

    private static boolean isUniqueConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
