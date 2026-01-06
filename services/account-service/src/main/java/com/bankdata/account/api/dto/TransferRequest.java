package com.bankdata.account.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String fromAccountNumber,
        @NotBlank String toAccountNumber,
        @NotNull BigDecimal amount
) {
}