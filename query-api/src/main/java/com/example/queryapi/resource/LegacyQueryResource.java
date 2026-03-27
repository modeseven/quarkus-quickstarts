package com.example.queryapi.resource;

import com.example.queryapi.model.QueryRequest;
import com.example.queryapi.model.QueryResponse;
import com.example.queryapi.service.LegacyPayloadConverter;
import com.example.queryapi.service.QueryBuilderService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/**
 * Accepts the legacy SENTRY flat payload, converts to modern QueryRequest,
 * and delegates to the standard query pipeline.
 */
@Path("/api/legacy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LegacyQueryResource {

    @Inject
    LegacyPayloadConverter converter;

    @Inject
    QueryBuilderService queryBuilder;

    @POST
    @Path("/query")
    public QueryResponse query(Map<String, String> payload) {
        QueryRequest req = converter.convert(payload);
        return queryBuilder.execute(req);
    }
}
