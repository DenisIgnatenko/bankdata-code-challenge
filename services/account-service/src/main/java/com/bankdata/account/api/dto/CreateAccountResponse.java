package com.bankdata.account.api.dto;

import java.math.BigDecimal;

public record CreateAccountResponse(String accountNumber, BigDecimal balance) {
}