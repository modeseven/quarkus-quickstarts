package com.example.queryapi.service;

import com.example.queryapi.registry.SearchKeyRegistry;
import com.example.queryapi.registry.SearchKeyDef;

import jakarta.enterprise.context.ApplicationScoped;

import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Maps a search key string to a jOOQ Field expression.
 * Handles all 4 join shapes + the SEL keyword.
 */
@ApplicationScoped
public class FieldResolver {

    public Field<?> resolve(String key, QueryContext ctx) {
        // SEL — alias for the primary selection category's ICUR join (ic)
        if (SearchKeyRegistry.isSel(key)) {
            return resolveSelField(key);
        }

        SearchKeyDef def = SearchKeyRegistry.get(key);

        switch (def.shape) {
            case HUB:
                return DSL.field("i." + def.column);

            case ICUR_CATEGORY:
                String alias = ctx.getOrCreateAlias(def.icat);
                if (def.isDispgrp) {
                    return JooqOperators.dispgrpConcat(alias);
                }
                return DSL.field(alias + "." + def.column);

            case DIRECT_SPOKE:
                return DSL.field(def.table + "." + def.column);

            case TYPED_SPOKE:
                String tsAlias = key.toLowerCase();
                return DSL.field(tsAlias + "." + def.column);

            default: // how come i dont' have to check this exectipoin?
                throw new IllegalStateException("Unknown shape: " + def.shape);
        }
    }

    private Field<?> resolveSelField(String key) {
        if (key.length() <= 3) {
            return DSL.field("ic.assignment");
        }
        switch (key.charAt(3)) {
            case 'G': return JooqOperators.dispgrpConcat("ic");
            case 'D': return DSL.field("ic.asn_str_dat");
            case 'T': return DSL.field("ic.asn_str_tim");
            default:  return DSL.field("ic.assignment");
        }
    }
}
