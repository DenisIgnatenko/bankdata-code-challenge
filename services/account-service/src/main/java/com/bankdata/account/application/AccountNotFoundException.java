package com.bankdata.account.application;

import java.math.BigDecimal;

public class AccountNotFoundException extends RuntimeException {
    public final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super("Account not found");
        this.accountNumber = accountNumber;
    }

}
