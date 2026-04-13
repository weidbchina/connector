package com.connector.service;

import com.connector.dto.SqlResult;
import com.connector.entity.DbConnection;
import com.connector.util.SqlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecutor {

    private final DbDataSourceManager dataSourceManager;

    public SqlResult execute(DbConnection connection, String sql, int maxRows) {
        return execute(connection, sql, 0, maxRows);
    }

    public SqlResult execute(DbConnection connection, String sql, int offset, int limit) {
        long startTime = System.currentTimeMillis();
        SqlResult result = new SqlResult();

        try {
            SqlValidator.validateSelectOnly(sql);

            DataSource ds = dataSourceManager.getDataSource(connection);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            
            // Calculate total limit for JDBC
            int jdbcMaxRows = (offset + limit) > 0 ? (offset + limit) : 10000;
            
            // Ensure maxRows limit
            jdbcTemplate.setMaxRows(jdbcMaxRows);
            jdbcTemplate.setQueryTimeout(30); // Default timeout

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            
            // Double check size and apply pagination manually since JDBC drivers behave differently
            // Some drivers might return all rows up to maxRows, we need to slice
            int start = Math.min(offset, rows.size());
            int end = Math.min(start + limit, rows.size());
            
            if (start < rows.size()) {
                rows = rows.subList(start, end);
            } else {
                rows = new ArrayList<>();
            }
            
            // Handle Oracle TIMESTAMP and other types that might not serialize well
            List<Map<String, Object>> sanitizedRows = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> newRow = new java.util.LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof java.sql.Timestamp || val instanceof oracle.sql.TIMESTAMP || val instanceof java.sql.Date) {
                        newRow.put(entry.getKey(), String.valueOf(val));
                    } else if (val != null && val.getClass().getName().startsWith("oracle.sql.")) {
                        newRow.put(entry.getKey(), String.valueOf(val));
                    } else {
                        newRow.put(entry.getKey(), val);
                    }
                }
                sanitizedRows.add(newRow);
            }
            
            result.setRows(sanitizedRows);
            result.setRowCount(sanitizedRows.size());
            if (!sanitizedRows.isEmpty()) {
                result.setColumns(new ArrayList<>(sanitizedRows.get(0).keySet()));
            }
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("Error executing SQL: {}", sql, e);
            result.setSuccess(false);
            result.setMessage(e.getMessage() != null ? e.getMessage() : e.toString());
        } finally {
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        }
        
        return result;
    }
}
