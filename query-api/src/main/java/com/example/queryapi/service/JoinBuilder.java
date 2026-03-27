package com.example.queryapi.service;

import com.example.queryapi.model.ConditionGroup;
import com.example.queryapi.model.ConditionRule;
import com.example.queryapi.model.QueryRequest;
import com.example.queryapi.registry.SearchKeyRegistry;
import com.example.queryapi.registry.SearchKeyDef;

import jakarta.enterprise.context.ApplicationScoped;

import org.jooq.DSLContext;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dynamic LEFT JOIN construction + deduplication.
 * spokeJoins passed explicitly so main query and census can each track their own.
 */
@ApplicationScoped
public class JoinBuilder {

    /**
     * Builds the base FROM/JOIN (ICUR + INMT), adds spoke joins for columns
     * and conditions, and conditionally adds the FACL join.
     *
     * @param columnKeys column keys needing joins (null if none, e.g. CENSUS/demoStats)
     */
    public SelectJoinStep<?> buildBase(DSLContext dsl, List<SelectFieldOrAsterisk> fields,
                                        QueryRequest req, QueryContext ctx,
                                        List<String> columnKeys) {
        SelectJoinStep<?> query = dsl.select(fields)
                .from(DSL.table("icur").as("ic"))
                .join(DSL.table("inmt").as("i"))
                .on(DSL.field("ic.icur_fk_inmt").eq(DSL.field("i.intkey")));

        Set<String> spokeJoins = new HashSet<>();
        if (columnKeys != null) {
            for (String col : columnKeys) {
                query = addJoinIfNeeded(query, col, ctx, spokeJoins);
            }
        }
        if (req.conditions != null) {
            for (ConditionGroup grp : req.conditions) {
                for (ConditionRule rule : grp.rules) {
                    query = addJoinIfNeeded(query, rule.field, ctx, spokeJoins);
                }
            }
        }
        return addFaclJoinIfNeeded(query, req);
    }

    public SelectJoinStep<?> addJoinIfNeeded(SelectJoinStep<?> query, String key,
                                              QueryContext ctx, Set<String> spokeJoins) {
        // SEL uses the primary ic alias — no additional join
        if (SearchKeyRegistry.isSel(key)) {
            return query;
        }

        SearchKeyDef def = SearchKeyRegistry.get(key);

        switch (def.shape) {
            case HUB:
                return query;

            case ICUR_CATEGORY:
                String alias = ctx.getOrCreateAlias(def.icat);
                if (!"ic".equals(alias) && !spokeJoins.contains(alias)) {
                    spokeJoins.add(alias);
                    query = query.leftJoin(DSL.table("icur").as(alias))
                            .on(DSL.field("i.intkey").eq(DSL.field(alias + ".icur_fk_inmt"))
                                    .and(DSL.field(alias + ".code_icat").eq(def.icat))
                                    .and(DSL.field(alias + ".active").eq("Y")));
                }
                return query;

            case DIRECT_SPOKE:
                if (!spokeJoins.contains(def.table)) {
                    spokeJoins.add(def.table);
                    query = query.leftJoin(DSL.table(def.table))
                            .on(DSL.field("i.intkey").eq(DSL.field(def.table + "." + def.fkColumn)));
                }
                return query;

            case TYPED_SPOKE:
                String tsAlias = key.toLowerCase();
                if (!spokeJoins.contains(tsAlias)) {
                    spokeJoins.add(tsAlias);
                    query = query.leftJoin(DSL.table(def.table).as(tsAlias))
                            .on(DSL.field("i.intkey").eq(DSL.field(tsAlias + "." + def.fkColumn))
                                    .and(DSL.function("trim", String.class,
                                            DSL.field(tsAlias + "." + def.typeColumn))
                                            .eq(def.typeValue)));
                }
                return query;

            default:
                return query;
        }
    }

    public boolean needsFaclJoin(QueryRequest req) {
        if (req.orgScope == null) return false;
        boolean needsTof = req.orgScope.typeOfFacility != null
                && !req.orgScope.typeOfFacility.isEmpty();
        boolean needsFmb = req.orgScope.facilityManagedBy != null
                && !req.orgScope.facilityManagedBy.isEmpty();
        // REGN, AGEN, DEPT all filter via facl columns
        boolean needsOrgType = false;
        if (req.orgScope.type != null) {
            String t = req.orgScope.type.toUpperCase();
            needsOrgType = "REGN".equals(t) || "AGEN".equals(t) || "DEPT".equals(t);
        }
        return needsTof || needsFmb || needsOrgType;
    }

    public SelectJoinStep<?> addFaclJoinIfNeeded(SelectJoinStep<?> query, QueryRequest req) {
        if (needsFaclJoin(req)) {
            query = query.join(DSL.table("facl").as("f"))
                    .on(DSL.field("i.code_facl").eq(DSL.field("f.code_facl")));
        }
        return query;
    }
}
