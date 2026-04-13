package com.connector.repository;

import com.connector.entity.MonitorTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MonitorTaskRepository extends JpaRepository<MonitorTask, Long> {
    List<MonitorTask> findByActiveTrue();
}
