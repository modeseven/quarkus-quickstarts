package com.example.queryapi.registry;

import java.util.Map;
import java.util.HashMap;

public class SearchKeyRegistry {

    private static final Map<String, SearchKeyDef> KEYS = new HashMap<>();

    static {
        // Hub columns (INMT — no join needed)
        KEYS.put("REG",  SearchKeyDef.hub("regno"));
        KEYS.put("LN",   SearchKeyDef.hub("name_last"));
        KEYS.put("FN",   SearchKeyDef.hub("name_first"));
        KEYS.put("FACL", SearchKeyDef.hub("code_facl"));
        KEYS.put("CITZ", SearchKeyDef.hub("cou_cit"));

        // Direct spoke columns (CCOM — 1:1 with INMT, same PK)
        KEYS.put("PRD",  SearchKeyDef.spoke("ccom", "intkey", "proj_rel_dt"));
        KEYS.put("PRM",  SearchKeyDef.spoke("ccom", "intkey", "proj_rel_mt"));
        KEYS.put("DETN", SearchKeyDef.spoke("ccom", "intkey", "detainer"));

        // Typed spoke columns (CNUM — 1:many with INMT, discriminated by cod_typ_num)
        KEYS.put("SSN",  SearchKeyDef.typedSpoke("cnum", "cnum_fk_inmt", "cod_typ_num", "SSN", "num"));
        KEYS.put("FBI",  SearchKeyDef.typedSpoke("cnum", "cnum_fk_inmt", "cod_typ_num", "FBI", "num"));
        KEYS.put("INS",  SearchKeyDef.typedSpoke("cnum", "cnum_fk_inmt", "cod_typ_num", "INS", "num"));

        // ICUR categories are resolved dynamically in get() — no need to
        // enumerate all 39 code_icat values. Any 3-char key not registered
        // above is treated as an ICUR category (assignment column).
    }

    public static boolean isSel(String key) {
        return key.equals("SEL") || (key.startsWith("SEL") && key.length() == 4);
    }

    public static SearchKeyDef get(String key) {
        // Direct lookup first (hub keys, spoke keys)
        SearchKeyDef def = KEYS.get(key);
        if (def != null) return def;

        // Dynamic ICUR resolution — any 3-4 char key not registered above
        // is treated as an ICUR category. Supports all 39+ code_icat values
        // without enumerating them.
        if (key.length() == 4) {
            char suffix = key.charAt(3);
            String base = key.substring(0, 3);
            switch (suffix) {
                case 'G': return SearchKeyDef.icurDispgrp(base);
                case 'D': return SearchKeyDef.icur(base, "asn_str_dat");
                case 'T': return SearchKeyDef.icur(base, "asn_str_tim");
                default:  break;
            }
        }

        if (key.length() == 3) {
            // Base ICUR category (assignment column)
            return SearchKeyDef.icur(key, "assignment");
        }

        throw new IllegalArgumentException("Unknown search key: " + key);
    }
}
