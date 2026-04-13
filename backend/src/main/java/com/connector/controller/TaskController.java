package com.connector.controller;

import com.connector.entity.MonitorTask;
import com.connector.entity.TaskLog;
import com.connector.repository.TaskLogRepository;
import com.connector.service.MonitorTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TaskController {

    private final MonitorTaskService taskService;
    private final TaskLogRepository logRepository;

    @GetMapping
    public List<MonitorTask> getAll() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public MonitorTask get(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @PostMapping
    public MonitorTask create(@RequestBody MonitorTask task) {
        return taskService.saveTask(task);
    }

    @PutMapping("/{id}")
    public MonitorTask update(@PathVariable Long id, @RequestBody MonitorTask task) {
        task.setId(id);
        return taskService.saveTask(task);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @GetMapping("/{id}/logs")
    public Page<TaskLog> getLogs(@PathVariable Long id, 
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return logRepository.findByTaskId(id, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executionTime")));
    }
}
