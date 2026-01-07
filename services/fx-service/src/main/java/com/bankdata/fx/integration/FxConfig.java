package com.bankdata.fx.integration;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "fx.exchangerate")
public interface FxConfig {
    String apiKey();

    @WithDefault("https://v6.exchangerate-api.com/v6")
    String baseUrl();
}