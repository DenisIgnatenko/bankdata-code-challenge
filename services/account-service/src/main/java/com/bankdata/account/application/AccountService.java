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

import org.jboss.logging.Logger;

@ApplicationScoped
public class AccountService {
    private static final Logger LOG = Logger.getLogger(AccountService.class);

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
        BigDecimal initial = normalizeMoney(
                request.initialDeposit() == null ? BigDecimal.ZERO : request.initialDeposit()
        );

        //2. Name check. Deleting spaces, null-check, blank-check. May be better to move to bean validation (@NotBlank)
        String firstName = request.firstName() == null ? null : request.firstName().trim();
        String lastName = request.lastName() == null ? null : request.lastName().trim();

        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First Name is Required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is Required");
        }

        //3. Retry in possible unique collisions
        //TRAP: we cannot make SELECT to check uniqueness (its a race). Must refer to unique costraint in DB
        for(int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String accountNumber = generator.next(); //remember that generator does not promised uniqueness to us :)

            AccountEntity entity = new AccountEntity(
                    accountNumber,
                    firstName,
                    lastName,
                    initial
            );

            try {
                //persist and flush makes Hibernate to make SQL Insert NOW. Important that we check uniqueness
                //inside try/catch block
                repository.persistAndFlush(entity);

                eventPublisher.safePublish(
                        AccountEvent.created(
                                entity.accountNumber,
                                entity.balance.toPlainString()
                        )
                );

                return new CreateAccountResponse(entity.accountNumber, entity.balance);
            } catch (PersistenceException exception) {
                if (isUniqueConstraintViolation(exception)) {
                    continue; //if we are in uniqueness violation - starting new attempt.
                }
                //if any other exception - push up.
                throw exception;
            }
        }
        throw new IllegalStateException(
                "Unable to generate unique account number after " + MAX_GENERATION_ATTEMPTS + " attempts"
        );
    }

    @Transactional
    public BalanceResponse deposit(String accountNumber, DepositRequest request) {
        BigDecimal amount = normalizeMoney(request.amount());
        AccountEntity entity = repository.getForUpdate(accountNumber);
        entity.deposit(amount);

        eventPublisher.safePublish(
                AccountEvent.deposited(
                        entity.accountNumber,
                        amount.toPlainString(),
                        entity.balance.toPlainString()
                )
        );

        return new BalanceResponse(entity.accountNumber, entity.balance);
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.fromAccountNumber().equals(request.toAccountNumber()))
            throw new IllegalStateException("accounts for transfer must be different");

        BigDecimal amount = normalizeMoney(request.amount());

        //deadlock possibility if there will be to parallel transactions A->B, B->A
        //idea is to sort them as strings, then lock from low to high
        String a = request.fromAccountNumber();
        String b = request.toAccountNumber();

        AccountEntity first;
        AccountEntity second;

        if (a.compareTo(b) < 0) {
            first = repository.getForUpdate(a);
            second = repository.getForUpdate(b);
        } else {
            first = repository.getForUpdate(b);
            second = repository.getForUpdate(a);
        }

        //comparing
        AccountEntity from = a.equals(first.accountNumber) ? first : second;
        AccountEntity to = a.equals(first.accountNumber) ? second : first;

        //then check amount > 0, balance - amount >=0)
        from.withdraw(amount);

        //check amount > 0;
        to.deposit(amount);

        eventPublisher.safePublish(
                AccountEvent.transferred(
                        from.accountNumber,
                        to.accountNumber,
                        amount.toPlainString()
                )
        );

        return new TransferResponse(
                from.accountNumber, from.balance,
                to.accountNumber, to.balance
        );

    }

    @Transactional
    public BalanceResponse balance(String accountNumber) {
        AccountEntity entity = repository.getByAccountNumber(accountNumber);
        return new BalanceResponse(entity.accountNumber, entity.balance);
    }


    //Normalizing money amounts.
    //Tasks: not null, not negative, must have 2 decimals after point, without rounding
    private static BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            throw new BadRequestException("Amount is required");
        }

        //we cannot accept negative numbers in amounts. Better to have deposit(positive) and withdraw(positive)
        if (value.signum() < 0) {
            throw new BadRequestException("Amount must be non-negative");
        }

        try {
            //important to get amount with 2 decimals after comma. UNNECESSARY because we dont want to round
            //amounts, for example if we get 10.001.
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Amount must have max 2 decimal places");
        }
    }

    //trying to find that exception stands for unique constraints
    //checking by text in message can be tricky. They can vary in different DBs
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
