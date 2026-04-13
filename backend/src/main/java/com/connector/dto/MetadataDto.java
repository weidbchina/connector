package com.connector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MetadataDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TableMeta {
        private String name;
        private String type; // TABLE, VIEW, SYNONYM
        private String schema;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ColumnMeta {
        private String name;
        private String type;
        private String comment;
    }
}
