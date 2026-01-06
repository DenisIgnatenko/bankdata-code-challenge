package com.bankdata.fx.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class ExchangeRateApiGateway {
    private static final Logger LOG = Logger.getLogger(ExchangeRateApiGateway.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final ObjectMapper mapper;
    private final FxConfig config;

    public ExchangeRateApiGateway(ObjectMapper mapper, FxConfig config) {
        this.mapper = mapper;
        this.config = config;
    }

    public ExchangeRateApiPairResponse pair(String base, String target, String amount) {
        String apiKey = config.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("EXCHANGE_RATE_API_KEY is not configured");
        }

        URI uri = URI.create(config.baseUrl() + "/" + apiKey + "/pair/" + base + "/" + target + "/" + amount);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                LOG.warnf("ExchangeRate API non-2xx: status: %s, body: %s", response.statusCode(), response.body());
            }

            ExchangeRateApiPairResponse body = mapper.readValue(response.body(), ExchangeRateApiPairResponse.class);
            if (!body.isSuccess()) {
                LOG.warnf("ExchangeRate API error: errorType=%s body = %s", body.errorType(), response.body());
                throw new IllegalStateException("ExchangeRate provider error: " + body.errorType());
            }

            return body;

        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse ExchangeRate provider response", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ExchangeRate provider call interrupted", exception);
        }
    }


}
