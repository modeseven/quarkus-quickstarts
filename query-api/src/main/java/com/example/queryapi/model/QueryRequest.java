package com.example.queryapi.model;

import java.util.List;

public class QueryRequest {
    public String function;              // "ROSTER" (default) or "CENSUS"
    public String selectionCategory;
    public String operator;
    public String assignment;
    public List<String> columns;

    // Organization scoping (replaces old facilityCode)
    public OrgScope orgScope;

    // Display controls
    public String duplicateSuppression;  // YES (default), NO, OUT (query-level, ARS only)
    public String judgment;              // pass-through, under investigation

    // Conditions and sorting
    public List<ConditionGroup> conditions;
    public SortSpec sort;
}
