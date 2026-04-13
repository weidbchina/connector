package com.connector.controller;

import com.connector.entity.DbConnection;
import com.connector.repository.DbConnectionRepository;
import com.connector.service.DbDataSourceManager;
import com.connector.service.SqlExecutor;
import com.connector.dto.SqlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConnectionController {

    private final DbConnectionRepository repository;
    private final SqlExecutor sqlExecutor;

    private final DbDataSourceManager dataSourceManager;

    @GetMapping
    public List<DbConnection> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DbConnection get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Connection not found"));
    }

    @PostMapping
    public DbConnection create(@RequestBody DbConnection connection) {
        return repository.save(connection);
    }

    @PutMapping("/{id}")
    public DbConnection update(@PathVariable Long id, @RequestBody DbConnection connection) {
        connection.setId(id);
        dataSourceManager.removeDataSource(id); // Clear cache
        return repository.save(connection);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PostMapping("/test")
    public SqlResult testConnection(@RequestBody DbConnection connection) {
        // Force refresh datasource for test to ensure new credentials work
        if (connection.getId() != null) {
             dataSourceManager.removeDataSource(connection.getId());
        }
        
        // Try to execute a simple query
        String testSql = "SELECT 1 FROM DUAL";
        if (connection.getJdbcUrl() != null && connection.getJdbcUrl().startsWith("jdbc:mysql:")) {
            testSql = "SELECT 1";
        }
        return sqlExecutor.execute(connection, testSql, 1);
    }
}
