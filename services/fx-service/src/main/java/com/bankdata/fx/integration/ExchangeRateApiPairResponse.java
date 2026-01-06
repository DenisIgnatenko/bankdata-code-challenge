package com.bankdata.fx.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ExchangeRateApiPairResponse(
        String result,
        @JsonProperty("error-type") String errorType,
        @JsonProperty("base-code") String baseCode,
        @JsonProperty("target-code") String targetCode,
        @JsonProperty("conversion_rate") BigDecimal conversionRate,
        @JsonProperty("conversion_result") BigDecimal conversionResult
) {
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(result);
    }
}
