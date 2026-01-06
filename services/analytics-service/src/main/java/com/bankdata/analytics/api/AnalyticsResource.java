package com.bankdata.analytics.api;

import com.bankdata.analytics.persistence.AccountEventEntity;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Analytics", description = "Read-side endpoints for ingested account events.")
public class AnalyticsResource {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    @GET
    @Path("/events")
    @Operation(
            summary = "Get latest events",
            description = "Returns latest ingested account events ordered by occurredAt DESC."
    )
    @APIResponse(responseCode = "200", description = "List of events ordered by occurredAt desc")
    public List<AccountEventEntity> latest(
            @Parameter(description = "Max number of items to return. Default=50. Range=1..200.")
            @QueryParam("limit") Integer limit) {
        int l = normalizeLimit(limit);
        return AccountEventEntity.find("order by occurredAt desc")
                .page(0, l)
                .list();
    }

    static int normalizeLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        if (limit < 1) return 1;
        return Math.min(limit, MAX_LIMIT);
    }
}