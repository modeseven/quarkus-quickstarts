package com.example.queryapi.model;

public class DemoStats {
    public int total; // COUNT(DISTINCT i.intkey)
    public int m, f;  // code_sex: M, F
    public int w, b, i, a; // code_rac: W, B, I, A
    public int h, o;  // code_eth: H (Hispanic), 0 (Other — note: char zero)
}
