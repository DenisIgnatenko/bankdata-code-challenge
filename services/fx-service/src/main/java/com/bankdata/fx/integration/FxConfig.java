package com.bankdata.fx.integration;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "fx.exchangerate")
public interface FxConfig {
    String baseUrl();

    String apiKey();
}