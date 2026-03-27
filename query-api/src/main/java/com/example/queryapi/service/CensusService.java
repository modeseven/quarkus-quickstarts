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
 * CENSUS function: grouped demographic counts (GROUP BY mode).
 * Same FROM/JOIN/WHERE as ROSTER, but SELECT is fixed demographic
 * aggregation instead of per-assignment display columns.
 */
@ApplicationScoped
public class CensusService {

    private static final Logger LOG = Logger.getLogger(CensusService.class.getName());

    private static final List<String> CENSUS_COLUMNS = Arrays.asList(
            "GRP", "SPECIFIC", "TOTAL", "MALE", "FEM",
            "WHITE", "BLACK", "AM_IN", "ASIAN", "HISP", "OTHER");

    @ConfigProperty(name = "query.show-sql", defaultValue = "false")
    boolean showSql;

    @Inject DSLContext dsl;
    @Inject JoinBuilder joinBuilder;
    @Inject DemoStatsService demoStatsService;

    public QueryResponse execute(QueryRequest req, QueryContext ctx, Condition where) {
        // Build fixed SELECT fields
        List<SelectFieldOrAsterisk> selectFields = new ArrayList<>();
        selectFields.add(JooqOperators.dispgrpConcat("ic").as("GRP"));
        selectFields.add(DSL.function("trim", String.class, DSL.field("ic.assignment")).as("SPECIFIC"));
        selectFields.add(DSL.count().as("TOTAL"));
        selectFields.add(censusCount("i.code_sex", "M", "MALE"));
        selectFields.add(censusCount("i.code_sex", "F", "FEM"));
        selectFields.add(censusCount("i.code_rac", "W", "WHITE"));
        selectFields.add(censusCount("i.code_rac", "B", "BLACK"));
        selectFields.add(censusCount("i.code_rac", "I", "AM_IN"));
        selectFields.add(censusCount("i.code_rac", "A", "ASIAN"));
        selectFields.add(censusCount("i.code_eth", "H", "HISP"));
        selectFields.add(censusCount("i.code_eth", "0", "OTHER"));

        // Build FROM/JOIN: base + condition joins + FACL (no column joins for CENSUS)
        SelectJoinStep<?> query = joinBuilder.buildBase(dsl, selectFields, req, ctx, null);

        // WHERE + GROUP BY + ORDER BY
        ResultQuery<?> finalQuery = query.where(where)
                .groupBy(
                        DSL.field("ic.dispgrp_1"),
                        DSL.field("ic.dispgrp_2"),
                        DSL.field("ic.dispgrp_3"),
                        DSL.field("ic.dispgrp_4"),
                        DSL.field("ic.assignment"))
                .orderBy(
                        DSL.field(DSL.name("GRP")).asc(),
                        DSL.field(DSL.name("SPECIFIC")).asc());

        // Debug SQL logging
        if (showSql) {
            LOG.info("\n── CENSUS QUERY ──\n" + JooqOperators.formatSql(finalQuery.getSQL(ParamType.INLINED)));
        }

        // Execute + clean results
        List<Map<String, Object>> rows = finalQuery.fetchMaps();
        List<Map<String, Object>> cleanRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (String col : CENSUS_COLUMNS) {
                Object val = row.get(col);
                if (val instanceof String) {
                    val = ((String) val).trim();
                } else if (val instanceof Number && !(val instanceof Integer)) {
                    val = ((Number) val).intValue();
                }
                clean.put(col, val);
            }
            cleanRows.add(clean);
        }

        // Fetch demographic summary bar (same as ROSTER)
        DemoStats demoStats = demoStatsService.fetch(req, ctx, where);

        // Assemble response
        QueryResponse response = new QueryResponse(CENSUS_COLUMNS, cleanRows);
        response.demoStats = demoStats;
        response.duplicateSuppression = req.duplicateSuppression != null
                ? req.duplicateSuppression : "YES";
        return response;
    }

    private Field<?> censusCount(String column, String value, String alias) {
        // COUNT(CASE WHEN trim(column) = value THEN 1 END)
        return DSL.field(
                "COUNT(CASE WHEN trim(" + column + ") = '" + value + "' THEN 1 END)",
                Integer.class).as(alias);
    }
}
