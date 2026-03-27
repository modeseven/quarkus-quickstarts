package com.example.queryapi.service;

import com.example.queryapi.registry.SearchKeyDef;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Optional;

/**
 * Static utility: operator dispatch, dispgrp wildcard matching, type casting.
 * Pure functions — no state, no CDI.
 */
public final class JooqOperators {

    private JooqOperators() {}

    /** Sentinel: keyword matched but no filter should be applied (ALL). */
    private static final Condition SKIP = DSL.trueCondition();

    /**
     * Intercept universal filter keywords (ALL, ANY, NONE).
     * Returns empty for normal values that should proceed to compare().
     * Returns SKIP (trueCondition) for ALL — caller just ANDs it on harmlessly.
     */
    public static Optional<Condition> resolveKeyword(Field<?> field, String op, String value) {
        if (value == null) return Optional.empty();
        String upper = value.trim().toUpperCase();
        switch (upper) {
            case "ALL":
                return Optional.of(SKIP);
            case "ANY": {
                Condition notBlank = field.isNotNull()
                        .and(DSL.trim(field.cast(String.class)).ne(""));
                return Optional.of("NE".equals(op) ? DSL.not(notBlank) : notBlank);
            }
            case "NONE": {
                Condition isBlank = field.isNull()
                        .or(DSL.trim(field.cast(String.class)).eq(""));
                return Optional.of("NE".equals(op) ? DSL.not(isBlank) : isBlank);
            }
            default:
                return Optional.empty();
        }
    }

    /** AND a comparison onto an existing condition. Used by selection filter paths. */
    @SuppressWarnings("unchecked")
    public static Condition applyOperator(Condition where, Field<?> field, String op, Object value) {
        return where.and(compare((Field<Object>) field, op, value));
    }

    /** Standalone comparison — returns a single Condition. Used by condition grid. */
    public static Condition compare(Field<Object> field, String op, Object value) {
        switch (op) {
            case "EQ": return field.eq(value);
            case "NE": return field.ne(value);
            case "GT": return field.gt(value);
            case "LT": return field.lt(value);
            case "GE": return field.ge(value);
            case "LE": return field.le(value);
            case "LK": return field.likeIgnoreCase("%" + value + "%");
            default:   return field.eq(value);
        }
    }

    /**
     * Dispgrp wildcard matching: 4-char pattern where '*' = wildcard, anything else = exact.
     * Returns a standalone Condition (AND of matched positions).
     */
    public static Condition dispgrpWildcard(String alias, String value) {
        Condition c = DSL.trueCondition();
        String[] cols = {"dispgrp_1", "dispgrp_2", "dispgrp_3", "dispgrp_4"};
        for (int pos = 0; pos < 4; pos++) {
            char ch = value.charAt(pos);
            if (ch != '*') {
                c = c.and(DSL.field(alias + "." + cols[pos]).eq(String.valueOf(ch)));
            }
        }
        return c;
    }

    /** Build the 4-field dispgrp concatenation expression. */
    public static Field<?> dispgrpConcat(String alias) {
        return DSL.field(alias + ".dispgrp_1")
                .concat(DSL.field(alias + ".dispgrp_2"))
                .concat(DSL.field(alias + ".dispgrp_3"))
                .concat(DSL.field(alias + ".dispgrp_4"));
    }

    /** Safe integer parse with string fallback. */
    public static Object castInt(String val) {
        if (val == null) return val;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return val; }
    }

    /** Format SQL for human-readable log output — newlines before major clauses. */
    public static String formatSql(String sql) {
        return sql
                .replaceAll("(?i)\\bfrom\\b", "\nFROM")
                .replaceAll("(?i)\\bjoin\\b", "\n  JOIN")
                .replaceAll("(?i)\\bleft join\\b", "\n  LEFT JOIN")
                .replaceAll("(?i)\\bwhere\\b", "\nWHERE")
                .replaceAll("(?i)\\band\\b", "\n  AND")
                .replaceAll("(?i)\\bor\\b", "\n  OR")
                .replaceAll("(?i)\\border by\\b", "\nORDER BY")
                .replaceAll("(?i)\\bgroup by\\b", "\nGROUP BY");
    }

    /** Context-aware casting using SearchKeyDef column hints. */
    public static Object castValue(SearchKeyDef def, String val) {
        if (def.column != null && (def.column.contains("dat") || def.column.contains("dt")
                || def.column.equals("regno"))) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                // fall through to string
            }
        }
        return val;
    }
}
