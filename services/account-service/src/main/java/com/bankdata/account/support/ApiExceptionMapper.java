package com.bankdata.account.support;

import com.bankdata.account.application.AccountNotFoundException;
import com.bankdata.account.domain.InsufficientFundsException;
import com.bankdata.account.domain.InvalidAmountException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof AccountNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("ACCOUNT_NOT_FOUND", exception.getMessage(), Map.of("accountNumber", e.accountNumber)))
                    .build();
        }
        if (exception instanceof InsufficientFundsException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("INSUFFICIENT_FUNDS", exception.getMessage(), Map.of(
                            "accountNumber", e.accountNumber,
                            "currentBalance", e.currentBalance,
                            "attemptedAmount", e.attemptedAmount
                    )))
                    .build();
        }
        if (exception instanceof InvalidAmountException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("INVALID_AMOUNT", exception.getMessage(), Map.of()))
                    .build();
        }
        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("BAD_REQUEST", exception.getMessage(), Map.of()))
                    .build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ApiError("INTERNAL_ERROR", "Unexpected error", Map.of()))
                .build();
    }
}