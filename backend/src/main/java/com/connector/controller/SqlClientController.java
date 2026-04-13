package com.connector.controller;

import com.connector.dto.ExecuteRequest;
import com.connector.dto.MetadataDto;
import com.connector.dto.SqlResult;
import com.connector.entity.DbConnection;
import com.connector.repository.DbConnectionRepository;
import com.connector.service.MetadataService;
import com.connector.service.SqlExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sql-client")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SqlClientController {

    private final SqlExecutor sqlExecutor;
    private final MetadataService metadataService;
    private final DbConnectionRepository connectionRepository;

    @PostMapping("/execute")
    public SqlResult execute(@RequestBody ExecuteRequest request) {
        DbConnection conn = connectionRepository.findById(request.getConnectionId())
                .orElseThrow(() -> new RuntimeException("Connection not found"));
        return sqlExecutor.execute(conn, request.getSql(), request.getMaxRows());
    }

    @GetMapping("/metadata/tables")
    public List<MetadataDto.TableMeta> getTables(@RequestParam Long connectionId, @RequestParam(required = false) String schema) {
        return metadataService.getTables(connectionId, schema);
    }

    @GetMapping("/metadata/schemas")
    public List<String> getSchemas(@RequestParam Long connectionId) {
        return metadataService.getSchemas(connectionId);
    }

    @GetMapping("/metadata/columns")
    public List<MetadataDto.ColumnMeta> getColumns(@RequestParam Long connectionId,
                                                   @RequestParam String tableName,
                                                   @RequestParam(required = false) String schema) {
        return metadataService.getColumns(connectionId, tableName, schema);
    }
}
