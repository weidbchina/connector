package com.connector.job;

import com.connector.dto.SqlResult;
import com.connector.entity.MonitorTask;
import com.connector.entity.TaskLog;
import com.connector.repository.MonitorTaskRepository;
import com.connector.repository.TaskLogRepository;
import com.connector.service.AlertService;
import com.connector.service.SqlExecutor;
import com.connector.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorJob implements Job {

    private final MonitorTaskRepository taskRepository;
    private final SqlExecutor sqlExecutor;
    private final ValidationService validationService;
    private final AlertService alertService;
    private final TaskLogRepository logRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = context.getJobDetail().getJobDataMap().getLong("taskId");
        MonitorTask task = taskRepository.findById(taskId).orElse(null);

        if (task == null || !task.isActive()) {
            return;
        }

        log.info("Executing Monitor Task: {}", task.getName());
        
        TaskLog taskLog = new TaskLog();
        taskLog.setTask(task);
        taskLog.setExecutionTime(LocalDateTime.now());

        try {
            SqlResult result = sqlExecutor.execute(task.getDbConnection(), task.getMonitorSql(), 1000); // Limit for monitor
            taskLog.setExecutionDurationMs(result.getExecutionTimeMs());
            
            if (result.isSuccess()) {
                log.info("Task [{}]: SQL Executed. Rows: {}. Content: {}", task.getName(), result.getRowCount(), result.getRows());
                String validationError = validationService.validate(result, task.getValidationRules());
                
                if (validationError == null) {
                    log.info("Task [{}]: Validation PASSED.", task.getName());
                    taskLog.setStatus(TaskLog.Status.SUCCESS);
                    taskLog.setResultSummary("Success. Rows: " + result.getRowCount());
                } else {
                    log.warn("Task [{}]: Validation FAILED. Reason: {}", task.getName(), validationError);
                    taskLog.setStatus(TaskLog.Status.ALERT_TRIGGERED);
                    taskLog.setResultSummary("Validation Failed: " + validationError);
                    alertService.sendAlert(task, validationError, result.getRows().toString());
                    taskLog.setAlertSent(true);
                }
            } else {
                log.error("Task [{}]: SQL Failed. Error: {}", task.getName(), result.getMessage());
                taskLog.setStatus(TaskLog.Status.FAILURE);
                taskLog.setResultSummary("SQL Error: " + result.getMessage());
            }

        } catch (Exception e) {
            taskLog.setStatus(TaskLog.Status.FAILURE);
            taskLog.setResultSummary("System Error: " + e.getMessage());
            log.error("Job Error", e);
        }

        logRepository.save(taskLog);
    }
}
