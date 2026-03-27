package com.example.queryapi.model;

public class OrgScope {
    public String type;              // FACL, REGN, AGEN, DEPT
    public String code;              // BOP (all), or specific facility code
    public String typeOfFacility;    // multi-char combo e.g. "TCM" → IN ('T','C','M')
    public String facilityManagedBy; // multi-char combo e.g. "AP" → IN ('A','P')
}
