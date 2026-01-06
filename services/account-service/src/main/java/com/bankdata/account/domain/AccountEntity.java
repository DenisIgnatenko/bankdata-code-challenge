package com.bankdata.account.domain;


import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uq_account_number", columnNames = "account_number")
)
public class AccountEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "account_number", nullable = false, updatable = false, length = 32)
    public String accountNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    public String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    public String lastName;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    public BigDecimal balance;

    /*Optimistic*/
    @Version
    public long version;

    public AccountEntity() {
    }

    public AccountEntity(String accountNumber, String firstName, String lastName, BigDecimal initialBalance) {
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
        this.balance = Objects.requireNonNull(initialBalance);
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) { //SRP: domain entity controls consistency
            throw new InsufficientFundsException(accountNumber, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
    }

}
