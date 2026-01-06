package com.bankdata.account.api.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String fromAccountNumber,
        BigDecimal fromBalance,
        String toAccountNumber,
        BigDecimal toBalance
) {
}