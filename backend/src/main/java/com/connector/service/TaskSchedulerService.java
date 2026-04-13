package com.connector.service;

import com.connector.entity.MonitorTask;
import com.connector.job.MonitorJob;
import com.connector.repository.MonitorTaskRepository;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerService {

    private final Scheduler scheduler;
    private final MonitorTaskRepository taskRepository;

    @PostConstruct
    public void init() {
        try {
            scheduler.clear();
            List<MonitorTask> tasks = taskRepository.findByActiveTrue();
            for (MonitorTask task : tasks) {
                scheduleTask(task);
            }
        } catch (SchedulerException e) {
            log.error("Error initializing scheduler", e);
        }
    }

    public void scheduleTask(MonitorTask task) {
        try {
            JobKey jobKey = JobKey.jobKey("task-" + task.getId());
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            if (!task.isActive()) return;

            JobDetail jobDetail = JobBuilder.newJob(MonitorJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("taskId", task.getId())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + task.getId())
                    .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled task: {}", task.getName());
        } catch (SchedulerException e) {
            log.error("Error scheduling task: " + task.getName(), e);
        }
    }

    public void unscheduleTask(Long taskId) {
        try {
            scheduler.deleteJob(JobKey.jobKey("task-" + taskId));
        } catch (SchedulerException e) {
            log.error("Error unscheduling task: " + taskId, e);
        }
    }
}
