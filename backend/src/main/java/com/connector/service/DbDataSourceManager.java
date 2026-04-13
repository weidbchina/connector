package com.connector.service;

import com.connector.entity.DbConnection;
import com.connector.util.AesUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DbDataSourceManager {

    private final Map<Long, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();

    public DataSource getDataSource(DbConnection connection) {
        // If password or url changed, we might need to recreate. But for now assuming simple caching by ID.
        // If connection update happens, we should probably clear cache.
        // Let's ensure we are using the latest connection details if we can, or just trust the ID map.
        // Better: check if existing DS is closed or invalid? Hikari handles that.
        // But if user changed password, we need new DS.
        // Simple fix: Always return existing if present. ConnectionController should call removeDataSource on update.
        return dataSourceMap.computeIfAbsent(connection.getId(), id -> createDataSource(connection));
    }

    private HikariDataSource createDataSource(DbConnection connection) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connection.getJdbcUrl());
        config.setUsername(connection.getUsername());
        // Password is encrypted in entity, but the getter returns decrypted value because of AttributeConverter
        config.setPassword(connection.getPassword()); 
        
        String url = connection.getJdbcUrl();
        if (url.startsWith("jdbc:mysql:")) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Auto-append common compatibility parameters for MySQL
            StringBuilder sb = new StringBuilder(url);
            boolean hasQuestionMark = url.contains("?");
            
            if (!url.contains("useSSL=")) {
                sb.append(hasQuestionMark ? "&" : "?").append("useSSL=false");
                hasQuestionMark = true;
            }
            if (!url.contains("allowPublicKeyRetrieval=")) {
                sb.append(hasQuestionMark ? "&" : "?").append("allowPublicKeyRetrieval=true");
                hasQuestionMark = true;
            }
            // Update the URL in config
            config.setJdbcUrl(sb.toString());
        } else if (url.startsWith("jdbc:oracle:")) {
            config.setDriverClassName("oracle.jdbc.OracleDriver");
        } else {
            // Fallback or throw error. Let HikariCP try to resolve if possible, or default to Oracle for legacy
            config.setDriverClassName("oracle.jdbc.OracleDriver");
        }
        
        config.setMaximumPoolSize(5); // Keep it small for this tool
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(3000);
        config.setPoolName("Pool-" + connection.getName());
        return new HikariDataSource(config);
    }

    public void removeDataSource(Long connectionId) {
        HikariDataSource ds = dataSourceMap.remove(connectionId);
        if (ds != null) {
            ds.close();
        }
    }
}
