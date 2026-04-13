package com.connector.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SqlResult {
    private boolean success;
    private String message;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private long executionTimeMs;
    private int rowCount;
}
