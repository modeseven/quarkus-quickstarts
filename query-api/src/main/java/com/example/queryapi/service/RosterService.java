package com.example.queryapi.service;

import com.example.queryapi.model.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.util.*;
import java.util.logging.Logger;

/**
 * ROSTER function: per-assignment rows + demographic summary bar.
 */
@ApplicationScoped
public class RosterService {

    private static final Logger LOG = Logger.getLogger(RosterService.class.getName());

    @ConfigProperty(name = "query.show-sql", defaultValue = "false")
    boolean showSql;

    @Inject DSLContext dsl;
    @Inject FieldResolver fieldResolver;
    @Inject JoinBuilder joinBuilder;
    @Inject DemoStatsService demoStatsService;

    public QueryResponse execute(QueryRequest req, QueryContext ctx, Condition where) {
        // Build SELECT fields
        List<SelectFieldOrAsterisk> selectFields = new ArrayList<>();
        for (String col : req.columns) {
            selectFields.add(fieldResolver.resolve(col, ctx).as(col));
        }

        // Build FROM/JOIN: base + column joins + condition joins + FACL
        SelectJoinStep<?> query = joinBuilder.buildBase(dsl, selectFields, req, ctx, req.columns);

        // Apply ORDER BY
        SelectConditionStep<?> conditioned = query.where(where);
        ResultQuery<?> finalQuery;
        if (req.sort != null && req.sort.column > 0 && req.sort.column <= req.columns.size()) {
            SortField<?> sortField = "DESC".equalsIgnoreCase(req.sort.direction)
                    ? DSL.field(DSL.name(req.columns.get(req.sort.column - 1))).desc()
                    : DSL.field(DSL.name(req.columns.get(req.sort.column - 1))).asc();
            finalQuery = conditioned.orderBy(sortField);
        } else {
            finalQuery = conditioned;
        }

        // Debug SQL logging
        if (showSql) {
            LOG.info("\n── ROSTER QUERY ──\n" + JooqOperators.formatSql(finalQuery.getSQL(ParamType.INLINED)));
        }

        // Execute + trim results
        List<Map<String, Object>> rows = finalQuery.fetchMaps();
        List<Map<String, Object>> cleanRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (String col : req.columns) {
                Object val = row.get(col);
                if (val instanceof String) {
                    val = ((String) val).trim();
                }
                clean.put(col, val);
            }
            cleanRows.add(clean);
        }

        // Fetch demographic summary bar
        DemoStats demoStats = demoStatsService.fetch(req, ctx, where);

        // Assemble response
        QueryResponse response = new QueryResponse(req.columns, cleanRows);
        response.demoStats = demoStats;
        response.duplicateSuppression = req.duplicateSuppression != null
                ? req.duplicateSuppression : "YES";
        return response;
    }
}
