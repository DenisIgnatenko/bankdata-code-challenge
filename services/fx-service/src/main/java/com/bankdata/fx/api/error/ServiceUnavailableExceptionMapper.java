package com.bankdata.fx.api.error;

import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ServiceUnavailableExceptionMapper implements ExceptionMapper<ServiceUnavailableException> {

    @Override
    public Response toResponse(ServiceUnavailableException exception) {
        ApiError error = ApiError.of(
                "SERVICE_UNAVAILABLE",
                exception.getMessage() == null ? "Service temporarily unavailable" : exception.getMessage()
        );

        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}