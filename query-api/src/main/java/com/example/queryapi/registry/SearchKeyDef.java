package com.example.queryapi.registry;

public class SearchKeyDef {
    public final JoinShape shape;
    public final String icat;        // only for ICUR_CATEGORY
    public final String table;       // DIRECT_SPOKE + TYPED_SPOKE
    public final String fkColumn;    // DIRECT_SPOKE + TYPED_SPOKE
    public final String column;      // the SELECT column
    public final boolean isDispgrp;  // true for G-suffix keys
    public final String typeColumn;  // only for TYPED_SPOKE (discriminator column)
    public final String typeValue;   // only for TYPED_SPOKE (discriminator value)

    SearchKeyDef(JoinShape shape, String icat, String table, String fkColumn,
                 String column, boolean isDispgrp, String typeColumn, String typeValue) {
        this.shape = shape;
        this.icat = icat;
        this.table = table;
        this.fkColumn = fkColumn;
        this.column = column;
        this.isDispgrp = isDispgrp;
        this.typeColumn = typeColumn;
        this.typeValue = typeValue;
    }

    public static SearchKeyDef hub(String column) {
        return new SearchKeyDef(JoinShape.HUB, null, null, null, column, false, null, null);
    }

    public static SearchKeyDef icur(String icat, String column) {
        return new SearchKeyDef(JoinShape.ICUR_CATEGORY, icat, null, null, column, false, null, null);
    }

    public static SearchKeyDef icurDispgrp(String icat) {
        return new SearchKeyDef(JoinShape.ICUR_CATEGORY, icat, null, null, null, true, null, null);
    }

    public static SearchKeyDef spoke(String table, String fkColumn, String column) {
        return new SearchKeyDef(JoinShape.DIRECT_SPOKE, null, table, fkColumn, column, false, null, null);
    }

    /** Typed spoke: 1:many table discriminated by a type column (e.g. CNUM by cod_typ_num) */
    public static SearchKeyDef typedSpoke(String table, String fkColumn,
                                          String typeColumn, String typeValue, String column) {
        return new SearchKeyDef(JoinShape.TYPED_SPOKE, null, table, fkColumn, column, false, typeColumn, typeValue);
    }
}
