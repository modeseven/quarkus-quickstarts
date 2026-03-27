package com.example.queryapi.model;

import java.util.List;
import java.util.Map;

public class QueryResponse {
    public List<String> columns;
    public List<Map<String, Object>> rows;
    public int count;

    // Demographic summary
    public DemoStats demoStats;

    // Echo display controls for frontend rendering
    public String duplicateSuppression;

    public QueryResponse(List<String> columns, List<Map<String, Object>> rows) {
        this.columns = columns;
        this.rows = rows;
        this.count = rows.size();
    }
}
