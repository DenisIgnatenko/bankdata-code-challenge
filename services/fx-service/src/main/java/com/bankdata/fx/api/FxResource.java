package com.bankdata.fx.api;

import com.bankdata.fx.integration.ExchangeRateApiGateway;
import com.bankdata.fx.integration.ExchangeRateApiPairResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Path("/fx")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class FxResource {

    private final ExchangeRateApiGateway gateway;

    @Inject
    public FxResource(ExchangeRateApiGateway gateway) {
        this.gateway = gateway;
    }

    @GET
    @Path("/dkk-usd")
    public DkkUsdResponse convert(@QueryParam("amount") BigDecimal amount) {
        BigDecimal dkk = (amount == null) ? new BigDecimal("100.00") : amount;

        if (dkk.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }

        String amountStr = dkk.setScale(2, RoundingMode.UNNECESSARY).toPlainString();

        ExchangeRateApiPairResponse response = gateway.pair("DKK", "USD", amountStr);

        return new DkkUsdResponse(dkk, response.conversionResult());
    }

    public record DkkUsdResponse(BigDecimal DKK, BigDecimal USD) {
    }
}