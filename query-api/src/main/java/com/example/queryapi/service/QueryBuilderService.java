package com.example.queryapi.service;

import com.example.queryapi.model.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jooq.*;

/**
 * Thin orchestrator: parses the request, delegates to collaborators,
 * assembles the response. All heavy lifting lives in the extracted services.
 */
@ApplicationScoped
public class QueryBuilderService {

    @Inject FieldResolver fieldResolver;
    @Inject ConditionBuilder conditionBuilder;
    @Inject RosterService rosterService;
    @Inject CensusService censusService;

    public QueryResponse execute(QueryRequest req) {
        // ── Shared pipeline (ROSTER + CENSUS) ────────────────────────────

        // 1. Parse selection category: base + optional G/D/T suffix
        String selCatRaw = req.selectionCategory != null ? req.selectionCategory.toUpperCase() : "";
        char selSuffix = 0;
        String baseCategory = selCatRaw;
        if (selCatRaw.length() >= 4) {
            char last = selCatRaw.charAt(selCatRaw.length() - 1);
            if (last == 'G' || last == 'D' || last == 'T') {
                selSuffix = last;
                baseCategory = selCatRaw.substring(0, selCatRaw.length() - 1);
            }
        }

        // 2. Create per-request context
        QueryContext ctx = new QueryContext(baseCategory, selSuffix);

        // 3. Pre-resolve aliases for all columns + conditions
        if (req.columns != null) {
            for (String col : req.columns) {
                fieldResolver.resolve(col, ctx);
            }
        }
        if (req.conditions != null) {
            for (ConditionGroup grp : req.conditions) {
                for (ConditionRule rule : grp.rules) {
                    fieldResolver.resolve(rule.field, ctx);
                }
            }
        }

        // 4. Build WHERE clause (shared by both functions)
        Condition where = conditionBuilder.buildWhereClause(req, ctx);

        // ── Function dispatch ────────────────────────────────────────────

        String function = req.function != null ? req.function.toUpperCase() : "ROSTER";
        if ("CENSUS".equals(function)) {
            return censusService.execute(req, ctx, where);
        }
        return rosterService.execute(req, ctx, where);
    }

}
