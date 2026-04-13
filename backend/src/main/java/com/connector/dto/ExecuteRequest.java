package com.connector.dto;

import lombok.Data;

@Data
public class ExecuteRequest {
    private Long connectionId;
    private String sql;
    private int maxRows = 50;
}
