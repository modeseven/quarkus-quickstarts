package com.example.queryapi.resource;

import com.example.queryapi.model.QueryRequest;
import com.example.queryapi.model.QueryResponse;
import com.example.queryapi.service.QueryBuilderService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QueryResource {

    @Inject
    QueryBuilderService queryBuilder;

    @POST
    @Path("/query")
    public QueryResponse query(QueryRequest req) {
        return queryBuilder.execute(req);
    }
}
