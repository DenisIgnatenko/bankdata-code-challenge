package com.bankdata.account.api.dto;

import java.math.BigDecimal;

public record BalanceResponse(String accountNumber, BigDecimal balance) {
}