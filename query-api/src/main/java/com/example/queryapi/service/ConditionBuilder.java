package com.example.queryapi.service;

import com.example.queryapi.model.ConditionGroup;
import com.example.queryapi.model.ConditionRule;
import com.example.queryapi.model.QueryRequest;
import com.example.queryapi.registry.SearchKeyRegistry;
import com.example.queryapi.registry.SearchKeyDef;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Optional;

/**
 * Builds jOOQ WHERE clauses: selection filter, org scope, conditions grid.
 * Reused by both the main query and the census query.
 */
@ApplicationScoped
public class ConditionBuilder {

    @Inject
    FieldResolver fieldResolver;

    @Inject
    OrgScopeFilter orgScopeFilter;

    // ── Main WHERE clause (shared by main query and census) ──────────────

    public Condition buildWhereClause(QueryRequest req, QueryContext ctx) {
        // Selection category — always filters on the base
        Condition where = DSL.field("ic.code_icat").eq(ctx.baseCategory);

        // Active filter — relaxed when Dup Supr = OUT and SelCat = ARS
        boolean isDupSuprOut = "OUT".equalsIgnoreCase(req.duplicateSuppression)
                && "ARS".equalsIgnoreCase(ctx.baseCategory);
        if (!isDupSuprOut) {
            where = where.and(DSL.field("ic.active").eq("Y"));
        }

        // Assignment/value filter (skip if ALL; ANY/NONE only valid in conditions grid)
        String op = req.operator != null ? req.operator.toUpperCase() : "EQ";
        if (req.assignment != null && !"ALL".equalsIgnoreCase(req.assignment.trim())) {
            where = applySelectionFilter(where, ctx.selSuffix, op, req.assignment);
        }

        // Org scope filters
        where = orgScopeFilter.apply(where, req.orgScope);

        // Conditions grid: AND within group, OR across groups
        if (req.conditions != null && !req.conditions.isEmpty()) {
            Condition groupsOr = null;
            for (ConditionGroup grp : req.conditions) {
                Condition groupAnd = null;
                for (ConditionRule rule : grp.rules) {
                    Condition c = buildCondition(rule, ctx);
                    groupAnd = (groupAnd == null) ? c : groupAnd.and(c);
                }
                if (groupAnd != null) {
                    groupsOr = (groupsOr == null) ? groupAnd : groupsOr.or(groupAnd);
                }
            }
            if (groupsOr != null) {
                where = where.and(groupsOr);
            }
        }

        return where;
    }

    // ── Selection category filter (suffix-aware) ─────────────────────────

    private Condition applySelectionFilter(Condition where, char suffix, String op, String value) {
        switch (suffix) {
            case 'G':
                return applyDispgrpFilter(where, "ic", op, value);
            case 'D':
                return JooqOperators.applyOperator(where, DSL.field("ic.asn_str_dat"), op,
                        JooqOperators.castInt(value));
            case 'T':
                return JooqOperators.applyOperator(where, DSL.field("ic.asn_str_tim"), op,
                        JooqOperators.castInt(value));
            default:
                return JooqOperators.applyOperator(where,
                        DSL.function("trim", String.class, DSL.field("ic.assignment")),
                        op, value);
        }
    }

    private Condition applyDispgrpFilter(Condition where, String alias, String op, String value) {
        if (value != null && value.length() == 4) {
            return where.and(JooqOperators.dispgrpWildcard(alias, value));
        }
        // Not a 4-char pattern — fall back to concatenated comparison
        return JooqOperators.applyOperator(where, JooqOperators.dispgrpConcat(alias), op, value);
    }

    // ── Single condition rule → jOOQ Condition ───────────────────────────

    public Condition buildCondition(ConditionRule rule, QueryContext ctx) {
        String key = rule.field;
        boolean isSel = SearchKeyRegistry.isSel(key);

        // SEL with G suffix + 4-char wildcard pattern → dispgrp filter on primary ic alias
        if (isSel && key.length() == 4 && key.charAt(3) == 'G'
                && rule.value != null && rule.value.length() == 4) {
            return JooqOperators.dispgrpWildcard("ic", rule.value);
        }

        if (!isSel) {
            SearchKeyDef def = SearchKeyRegistry.get(key);

            // Dispgrp wildcard matching: parse 4-char pattern, individual column predicates
            if (def.isDispgrp && rule.value != null && rule.value.length() == 4) {
                String alias = ctx.getOrCreateAlias(def.icat);
                return JooqOperators.dispgrpWildcard(alias, rule.value);
            }
        }

        Field<Object> field = fieldResolver.resolve(key, ctx).coerce(Object.class);

        // Intercept universal keywords (ALL, ANY, NONE) before casting
        Optional<Condition> keyword = JooqOperators.resolveKeyword(field, rule.op.toUpperCase(), rule.value);
        if (keyword.isPresent()) {
            return keyword.get();
        }

        Object val;
        if (isSel) {
            // SEL D/T suffixes are integer columns
            char suffix = key.length() == 4 ? key.charAt(3) : 0;
            val = (suffix == 'D' || suffix == 'T') ? JooqOperators.castInt(rule.value) : rule.value;
        } else {
            val = JooqOperators.castValue(SearchKeyRegistry.get(key), rule.value);
        }

        return JooqOperators.compare(field, rule.op.toUpperCase(), val);
    }
}
