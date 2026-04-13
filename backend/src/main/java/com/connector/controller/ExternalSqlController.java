package com.connector.controller;

import com.connector.dto.ExecuteSqlRequest;
import com.connector.dto.SqlResult;
import com.connector.entity.DbConnection;
import com.connector.repository.DbConnectionRepository;
import com.connector.service.SqlExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/external/sql")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ExternalSqlController {

    private final DbConnectionRepository repository;
    private final SqlExecutor sqlExecutor;

    @PostMapping("/execute")
    public SqlResult execute(@RequestBody ExecuteSqlRequest request) {
        if (request.getConnectionId() == null) {
            throw new IllegalArgumentException("connectionId is required");
        }
        if (request.getSql() == null || request.getSql().trim().isEmpty()) {
            throw new IllegalArgumentException("sql is required");
        }

        DbConnection connection = repository.findById(request.getConnectionId())
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        int offset = request.getOffset() != null ? request.getOffset() : 0;
        int limit = request.getLimit() != null ? request.getLimit() : 100; // Default 100

        return sqlExecutor.execute(connection, request.getSql(), offset, limit);
    }
}
