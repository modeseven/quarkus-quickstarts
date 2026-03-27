package com.example.queryapi.service;

import com.example.queryapi.model.*;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts a flat legacy SENTRY payload (Map of string key-value pairs)
 * into a modern QueryRequest.
 */
@ApplicationScoped
public class LegacyPayloadConverter {

    public QueryRequest convert(Map<String, String> p) {
        QueryRequest req = new QueryRequest();

        // Core fields
        req.function = mapFunction(get(p, "func"));
        req.selectionCategory = upper(get(p, "selcat"));
        req.operator = upper(get(p, "scop"));
        req.assignment = mapAssignment(get(p, "scass"));

        // Columns (col1..col8, collect non-blank in order)
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            String val = upper(get(p, "col" + i));
            if (val != null) {
                columns.add(val);
            }
        }
        if (!columns.isEmpty()) {
            req.columns = columns;
        }

        // OrgScope
        req.orgScope = mapOrgScope(p);

        // Display controls
        req.duplicateSuppression = upper(get(p, "dupsupr"));
        req.judgment = upper(get(p, "judg"));

        // Sort
        req.sort = mapSort(p);

        // Conditions grid (4 groups x 10 rows)
        req.conditions = mapConditions(p);

        return req;
    }

    private String mapFunction(String func) {
        if (func == null || "***".equals(func)) return "ROSTER";
        switch (func.toUpperCase()) {
            case "ROS": return "ROSTER";
            case "CEN": return "CENSUS";
            default:    return "ROSTER";
        }
    }

    private String mapAssignment(String scass) {
        if (scass == null) return null;
        String trimmed = scass.trim().toUpperCase();
        if (trimmed.isEmpty()) return null;
        return "***".equals(scass.trim()) ? "ALL" : trimmed;
    }

    private OrgScope mapOrgScope(Map<String, String> p) {
        String type = upper(get(p, "org"));
        String code = upper(get(p, "orgass"));
        String tof = upper(get(p, "tofass"));
        String fmb = upper(get(p, "fmb"));

        if (type == null && code == null && tof == null && fmb == null) {
            return null;
        }

        OrgScope scope = new OrgScope();
        scope.type = type;
        scope.code = code;
        scope.typeOfFacility = tof;
        scope.facilityManagedBy = fmb;
        return scope;
    }

    private SortSpec mapSort(Map<String, String> p) {
        String srtcol = get(p, "srtcol");
        if (srtcol == null) return null;

        try {
            int col = Integer.parseInt(srtcol);
            if (col <= 0) return null;

            SortSpec sort = new SortSpec();
            sort.column = col;

            String seq = upper(get(p, "seq"));
            if ("D".equals(seq)) {
                sort.direction = "DESC";
            } else {
                sort.direction = "ASC";
            }
            return sort;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<ConditionGroup> mapConditions(Map<String, String> p) {
        List<ConditionGroup> groups = new ArrayList<>();

        for (int g = 1; g <= 4; g++) {
            List<ConditionRule> rules = new ArrayList<>();
            for (int r = 1; r <= 10; r++) {
                String cat = upper(get(p, "cnd" + g + "cat_" + r));
                if (cat == null) continue;

                ConditionRule rule = new ConditionRule();
                rule.field = cat;
                rule.op = upper(get(p, "cnd" + g + "op_" + r));
                rule.value = upper(get(p, "cnd" + g + "ass_" + r));
                rules.add(rule);
            }
            if (!rules.isEmpty()) {
                ConditionGroup grp = new ConditionGroup();
                grp.group = g;
                grp.rules = rules;
                groups.add(grp);
            }
        }

        return groups.isEmpty() ? null : groups;
    }

    /** Trim + uppercase, returns null if blank. */
    private String upper(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    /** Case-insensitive key lookup with trim. */
    private String get(Map<String, String> p, String key) {
        String val = p.get(key);
        if (val != null) return val;
        // Try lowercase/uppercase variants
        val = p.get(key.toLowerCase());
        if (val != null) return val;
        val = p.get(key.toUpperCase());
        return val;
    }
}
