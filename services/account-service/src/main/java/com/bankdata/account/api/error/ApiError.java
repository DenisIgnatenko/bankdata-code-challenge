package com.bankdata.account.api.error;

import java.util.Map;

public record ApiError(String code, String message, Map<String, Object> details) {

    public static ApiError badRequest(String message) {
        return new ApiError("BAD_REQUEST", message, Map.of());
    }

    public static ApiError internalError() {
        return new ApiError("INTERNAL_ERROR", "Unexpected error", Map.of());
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of());
    }
}