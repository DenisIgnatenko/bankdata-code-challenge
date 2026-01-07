package com.bankdata.account.api.error;

import com.bankdata.account.domain.InvalidAmountException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidAmountExceptionMapper implements ExceptionMapper<InvalidAmountException> {

    @Override
    public Response toResponse(InvalidAmountException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiError.of("INVALID_AMOUNT", e.getMessage()))
                .build();
    }
}