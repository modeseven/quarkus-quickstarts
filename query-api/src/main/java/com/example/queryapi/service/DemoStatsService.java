package com.example.queryapi.service;

import com.example.queryapi.model.DemoStats;
import com.example.queryapi.model.QueryRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.util.*;
import java.util.logging.Logger;

/**
 * Demographic summary bar — the sex/race/ethnicity counts included
 * in every ROSTER response. Not the CENSUS function (see QueryBuilderService).
 */
@ApplicationScoped
public class DemoStatsService {

    private static final Logger LOG = Logger.getLogger(DemoStatsService.class.getName());

    @ConfigProperty(name = "query.show-sql", defaultValue = "false")
    boolean showSql;

    @Inject
    DSLContext dsl;

    @Inject
    JoinBuilder joinBuilder;

    public DemoStats fetch(QueryRequest req, QueryContext ctx, Condition where) {
        // Build census SELECT with conditional COUNT(DISTINCT)
        List<SelectFieldOrAsterisk> censusFields = new ArrayList<>();
        censusFields.add(DSL.countDistinct(DSL.field("i.intkey")).as("total"));
        censusFields.add(censusCount("i.code_sex", "M", "m"));
        censusFields.add(censusCount("i.code_sex", "F", "f"));
        censusFields.add(censusCount("i.code_rac", "W", "w"));
        censusFields.add(censusCount("i.code_rac", "B", "b"));
        censusFields.add(censusCount("i.code_rac", "I", "census_i"));
        censusFields.add(censusCount("i.code_rac", "A", "a"));
        censusFields.add(censusCount("i.code_eth", "H", "h"));
        censusFields.add(censusCount("i.code_eth", "0", "o")); // char zero = Other Than Hispanic

        // Build FROM/JOIN: base + condition joins + FACL
        SelectJoinStep<?> censusQuery = joinBuilder.buildBase(dsl, censusFields, req, ctx, null);

        SelectConditionStep<?> conditioned = censusQuery.where(where);

        if (showSql) {
            LOG.info("\n── CENSUS QUERY ──\n" + JooqOperators.formatSql(conditioned.getSQL(ParamType.INLINED)));
        }

        Map<String, Object> row = conditioned.fetchOne().intoMap();

        DemoStats stats = new DemoStats();
        stats.total = toInt(row.get("total"));
        stats.m = toInt(row.get("m"));
        stats.f = toInt(row.get("f"));
        stats.w = toInt(row.get("w"));
        stats.b = toInt(row.get("b"));
        stats.i = toInt(row.get("census_i"));
        stats.a = toInt(row.get("a"));
        stats.h = toInt(row.get("h"));
        stats.o = toInt(row.get("o"));
        return stats;
    }

    private Field<?> censusCount(String column, String value, String alias) {
        // COUNT(DISTINCT CASE WHEN trim(column) = value THEN i.intkey END)
        return DSL.field(
                "COUNT(DISTINCT CASE WHEN trim(" + column + ") = '" + value
                        + "' THEN i.intkey END)",
                Integer.class).as(alias);
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }
}
