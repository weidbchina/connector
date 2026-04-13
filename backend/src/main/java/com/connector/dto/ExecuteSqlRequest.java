package com.connector.dto;

import lombok.Data;

@Data
public class ExecuteSqlRequest {
    private Long connectionId;
    private String sql;
    private Integer offset; // Start row (0-based)
    private Integer limit;  // Number of rows to return
}
