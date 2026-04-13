package com.connector.service;

import com.connector.dto.MetadataDto;
import com.connector.entity.DbConnection;
import com.connector.repository.DbConnectionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final DbConnectionRepository connectionRepository;
    private final DbDataSourceManager dataSourceManager;

    private final Cache<Long, List<MetadataDto.TableMeta>> tableCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    private final Cache<String, List<MetadataDto.ColumnMeta>> columnCache = Caffeine.newBuilder() // Key: connId + tableName
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public List<String> getSchemas(Long connectionId) {
        DbConnection conn = connectionRepository.findById(connectionId).orElseThrow(() -> new RuntimeException("Connection not found"));
        DataSource ds = dataSourceManager.getDataSource(conn);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);

        if (conn.getJdbcUrl().startsWith("jdbc:mysql:")) {
            return jdbcTemplate.queryForList("SHOW DATABASES", String.class);
        } else {
            // Oracle Schemas (Users)
            return jdbcTemplate.queryForList("SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME", String.class);
        }
    }

    public List<MetadataDto.TableMeta> getTables(Long connectionId, String schema) {
        // We cannot use the simple cache key because Caffeine key is Long.
        // We need to bypass cache or change cache key type.
        // For simplicity in this iteration, let's just fetch directly if schema is present, 
        // or clear/reload cache. 
        // Better: Just fetch directly. Metadata is fast enough or client caches it.
        return fetchTables(connectionId, schema);
    }

    // Deprecated or redirect
    public List<MetadataDto.TableMeta> getTables(Long connectionId) {
        return getTables(connectionId, null);
    }

    public List<MetadataDto.ColumnMeta> getColumns(Long connectionId, String tableName, String schema) {
        String cacheKey = connectionId + ":" + (schema == null ? "" : schema) + ":" + tableName;
        return columnCache.get(cacheKey, k -> fetchColumns(connectionId, tableName, schema));
    }

    private List<MetadataDto.TableMeta> fetchTables(Long connectionId, String schema) {
        DbConnection conn = connectionRepository.findById(connectionId).orElseThrow(() -> new RuntimeException("Connection not found"));
        DataSource ds = dataSourceManager.getDataSource(conn);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);

        if (conn.getJdbcUrl().startsWith("jdbc:mysql:")) {
            // MySQL
            String targetSchema = schema != null ? schema : "DATABASE()";
            String sql = "SELECT TABLE_NAME, TABLE_TYPE, TABLE_SCHEMA FROM information_schema.TABLES " +
                         "WHERE TABLE_SCHEMA = " + (schema != null ? "?" : "DATABASE()") + " ORDER BY TABLE_NAME";
            
            Object[] args = schema != null ? new Object[]{schema} : new Object[]{};
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> new MetadataDto.TableMeta(
                    rs.getString("TABLE_NAME"),
                    rs.getString("TABLE_TYPE"),
                    rs.getString("TABLE_SCHEMA")
            ), args);
        } else {
            // Oracle
            // If schema is provided, filter by OWNER. If not, use current user or ALL
            String sql;
            Object[] args;
            if (schema != null) {
                sql = "SELECT OWNER, OBJECT_NAME, OBJECT_TYPE FROM ALL_OBJECTS " +
                      "WHERE OBJECT_TYPE IN ('TABLE', 'VIEW', 'SYNONYM') " +
                      "AND OWNER = ? " +
                      "ORDER BY OBJECT_NAME";
                args = new Object[]{schema};
            } else {
                // Default behavior: show current user objects? or stay as before (ALL except sys)
                // Let's default to Current User for cleaner view if no schema selected
                sql = "SELECT OWNER, OBJECT_NAME, OBJECT_TYPE FROM ALL_OBJECTS " +
                      "WHERE OBJECT_TYPE IN ('TABLE', 'VIEW', 'SYNONYM') " +
                      "AND OWNER = USER " + 
                      "ORDER BY OBJECT_NAME";
                args = new Object[]{};
            }
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> new MetadataDto.TableMeta(
                    rs.getString("OBJECT_NAME"),
                    rs.getString("OBJECT_TYPE"),
                    rs.getString("OWNER")
            ), args);
        }
    }

    private List<MetadataDto.ColumnMeta> fetchColumns(Long connectionId, String tableName, String schema) {
        DbConnection conn = connectionRepository.findById(connectionId).orElseThrow(() -> new RuntimeException("Connection not found"));
        DataSource ds = dataSourceManager.getDataSource(conn);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);

        if (conn.getJdbcUrl().startsWith("jdbc:mysql:")) {
            // MySQL
            String effectiveSchema = schema;
            String effectiveTable = tableName;
            if (effectiveSchema == null && tableName != null && tableName.contains(".")) {
                String[] parts = tableName.split("\\.");
                if (parts.length >= 2) {
                    effectiveSchema = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1));
                    effectiveTable = parts[parts.length - 1];
                }
            }

            String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM information_schema.COLUMNS " +
                         "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = " + (effectiveSchema != null ? "?" : "DATABASE()") + " ORDER BY ORDINAL_POSITION";
            return jdbcTemplate.query(sql, (rs, rowNum) -> new MetadataDto.ColumnMeta(
                    rs.getString("COLUMN_NAME"),
                    rs.getString("DATA_TYPE"),
                    rs.getString("COLUMN_COMMENT")
            ), effectiveSchema != null ? new Object[]{effectiveTable, effectiveSchema} : new Object[]{effectiveTable});
        } else {
            // Oracle
            // Use LEFT JOIN to get comments from ALL_COL_COMMENTS
            String effectiveOwner = schema;
            String effectiveTable = tableName;
            if (effectiveOwner == null && tableName != null && tableName.contains(".")) {
                String[] parts = tableName.split("\\.");
                if (parts.length >= 2) {
                    effectiveOwner = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1));
                    effectiveTable = parts[parts.length - 1];
                }
            }

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT c.COLUMN_NAME, c.DATA_TYPE, m.COMMENTS ");
            sqlBuilder.append("FROM ALL_TAB_COLUMNS c ");
            sqlBuilder.append("LEFT JOIN ALL_COL_COMMENTS m ON c.OWNER = m.OWNER AND c.TABLE_NAME = m.TABLE_NAME AND c.COLUMN_NAME = m.COLUMN_NAME ");
            
            java.util.List<Object> argsList = new java.util.ArrayList<>();
            
            if (effectiveOwner != null && !effectiveOwner.trim().isEmpty()) {
                sqlBuilder.append("WHERE (c.OWNER = ? OR c.OWNER = UPPER(?)) AND (c.TABLE_NAME = ? OR c.TABLE_NAME = UPPER(?)) ");
                argsList.add(effectiveOwner);
                argsList.add(effectiveOwner);
                argsList.add(effectiveTable);
                argsList.add(effectiveTable);
            } else {
                sqlBuilder.append("WHERE (c.TABLE_NAME = ? OR c.TABLE_NAME = UPPER(?)) ");
                argsList.add(effectiveTable);
                argsList.add(effectiveTable);
            }

            sqlBuilder.append("ORDER BY c.COLUMN_ID");
            
            return jdbcTemplate.query(sqlBuilder.toString(), (rs, rowNum) -> new MetadataDto.ColumnMeta(
                    rs.getString("COLUMN_NAME"),
                    rs.getString("DATA_TYPE"),
                    rs.getString("COMMENTS")
            ), argsList.toArray());
        }
    }
}
