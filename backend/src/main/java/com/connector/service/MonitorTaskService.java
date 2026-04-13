package com.connector.service;

import com.connector.entity.MonitorTask;
import com.connector.repository.MonitorTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorTaskService {

    private final MonitorTaskRepository taskRepository;
    private final TaskSchedulerService schedulerService;

    public List<MonitorTask> getAllTasks() {
        return taskRepository.findAll();
    }

    public MonitorTask getTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
    }

    @Transactional
    public MonitorTask saveTask(MonitorTask task) {
        MonitorTask saved = taskRepository.save(task);
        schedulerService.scheduleTask(saved);
        return saved;
    }

    @Transactional
    public void deleteTask(Long id) {
        schedulerService.unscheduleTask(id);
        taskRepository.deleteById(id);
    }
}
