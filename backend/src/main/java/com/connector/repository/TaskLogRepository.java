package com.connector.repository;

import com.connector.entity.TaskLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {
    Page<TaskLog> findByTaskId(Long taskId, Pageable pageable);
}
