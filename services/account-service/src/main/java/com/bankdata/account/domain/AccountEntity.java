package com.bankdata.account.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;


//Idea is to make AccountEntity as a Domain Entity.
//1. Invariants live inside domain (like amount > 0), balance not negative
//2. State is protected, nobody can set balance inside service.
//So AccountService rules orchestration, transaction, events, but not storing.
@Entity
@Table(
        name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uq_account_number", columnNames = "account_number")
)
public class AccountEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, updatable = false, length = 32)
    private String accountNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    protected AccountEntity() {
        //for JPA
    }


    public AccountEntity(String accountNumber, String firstName, String lastName, BigDecimal initialBalance) {
        this.accountNumber = requireNonBlank(accountNumber, "accountNumber");
        this.firstName = requireNonBlank(firstName, "firstName");
        this.lastName = requireNonBlank(lastName, "lastName");

        this.balance = requireNonNull(initialBalance, "initialBalance");
        if (this.balance.signum() < 0) {
            throw new InvalidAmountException("initialBalance must be >= 0");
        }

        this.balance = initialBalance;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount, "amount");
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount, "amount");
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(this.accountNumber, this.balance, amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    private static BigDecimal requireNonNull(BigDecimal value, String field) {
        if (value == null) {
            throw new InvalidAmountException(field + " must not be null");
        }
        return value;
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null) {
            throw new InvalidAmountException(field + " must not be null");
        }
        if (value.signum() <= 0) {
            throw new InvalidAmountException(field + " must be positive");
        }
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null) {
            throw new InvalidAmountException(field + " must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidAmountException(field + " must not be blank");
        }
        return trimmed;
    }

}
