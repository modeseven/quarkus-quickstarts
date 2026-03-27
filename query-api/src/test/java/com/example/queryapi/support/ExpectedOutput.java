package com.example.queryapi.support;

import com.example.queryapi.model.DemoStats;
import java.util.List;
import java.util.Map;

/**
 * Expected output for a fixture test. All fields nullable — null = skip assertion.
 */
public class ExpectedOutput {
    public Integer count;
    public List<String> columns;
    public DemoStats demoStats;
    public List<Map<String, Object>> rows;
}
