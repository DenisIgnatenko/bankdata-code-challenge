package com.bankdata.account.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Throwable e) {
        LOG.error("Unhandled error", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiError.of("INTERNAL_ERROR", "Unexpected error"))
                .build();
    }
}