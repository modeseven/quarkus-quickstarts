package com.example.queryapi.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-request mutable state container. Created at the start of execute(),
 * passed to all collaborators. Future-proof: when AuthContext is added,
 * decorate this with user/facility info — zero signature changes downstream.
 */
public class QueryContext {

    public final String baseCategory;
    public final char selSuffix;
    public final Map<String, String> icurAliases = new HashMap<>();

    public QueryContext(String baseCategory, char selSuffix) {
        this.baseCategory = baseCategory;
        this.selSuffix = selSuffix;
        // Primary selection category always uses the "ic" alias
        icurAliases.put(baseCategory, "ic");
    }

    public String getOrCreateAlias(String icat) {
        return icurAliases.computeIfAbsent(icat, k -> "ic_" + k.toLowerCase());
    }
}
