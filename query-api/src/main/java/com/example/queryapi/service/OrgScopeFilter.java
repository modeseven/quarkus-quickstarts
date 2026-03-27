package com.example.queryapi.service;

import com.example.queryapi.model.OrgScope;

import jakarta.enterprise.context.ApplicationScoped;

import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds jOOQ WHERE predicates for org-hierarchy scoping (FACL/REGN/AGEN/DEPT)
 * and facility-level filters (TOF, FMB).
 */
@ApplicationScoped
public class OrgScopeFilter {

    /**
     * Appends org-scope conditions to the given WHERE clause.
     *
     * Org hierarchy: DEPT → AGEN → REGN → FACL → INMT
     *   agen.agen_fk_dept → dept    regn.regn_fk_agen → agen
     *   facl.facl_fk_regn → regn    inmt.code_facl → facl
     */
    public Condition apply(Condition where, OrgScope orgScope) {
        if (orgScope == null) return where;

        String type = orgScope.type != null ? orgScope.type.toUpperCase() : "FACL";
        String code = orgScope.code;
        boolean hasCode = code != null && !code.isEmpty();

        // Type-driven org filter
        if (hasCode) {
            switch (type) {
                case "FACL":
                    if (!"BOP".equalsIgnoreCase(code)) {
                        where = where.and(DSL.field("i.code_facl").eq(code));
                    }
                    break;
                case "REGN":
                    where = where.and(DSL.field("f.facl_fk_regn").eq(code));
                    break;
                case "AGEN":
                    where = where.and(DSL.field("f.facl_fk_regn").in(
                            DSL.select(DSL.field("code_regn"))
                               .from("regn")
                               .where(DSL.field("regn_fk_agen").eq(code))));
                    break;
                case "DEPT":
                    where = where.and(DSL.field("f.facl_fk_regn").in(
                            DSL.select(DSL.field("r.code_regn"))
                               .from(DSL.table("regn").as("r"))
                               .join(DSL.table("agen").as("a"))
                               .on(DSL.field("r.regn_fk_agen").eq(DSL.field("a.code_agen")))
                               .where(DSL.field("a.agen_fk_dept").eq(code))));
                    break;
                default:
                    break;
            }
        }

        // Type of Facility: multi-char combo → IN clause on FACL table
        if (orgScope.typeOfFacility != null && !orgScope.typeOfFacility.isEmpty()) {
            List<String> tofCodes = splitChars(orgScope.typeOfFacility);
            where = where.and(DSL.field("f.code_tof").in(tofCodes));
        }

        // Facility Managed By: multi-char combo → IN clause on FACL table
        if (orgScope.facilityManagedBy != null && !orgScope.facilityManagedBy.isEmpty()) {
            List<String> fmbCodes = splitChars(orgScope.facilityManagedBy);
            where = where.and(DSL.field("f.code_fmb").in(fmbCodes));
        }

        return where;
    }

    private List<String> splitChars(String multiChar) {
        List<String> result = new ArrayList<>();
        for (char c : multiChar.toCharArray()) {
            result.add(String.valueOf(c));
        }
        return result;
    }
}
