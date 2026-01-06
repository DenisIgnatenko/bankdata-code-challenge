package com.bankdata.account.domain;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public final String accountNumber;
    public final BigDecimal currentBalance;
    public final BigDecimal attemptedAmount;

    public InsufficientFundsException(String accountNumber,
                                      BigDecimal currentBalance,
                                      BigDecimal attemptedAmount) {
        super("Insufficient finds");
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.attemptedAmount = attemptedAmount;
    }
}
