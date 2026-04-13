package com.connector.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task_logs")
public class TaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private MonitorTask task;

    private LocalDateTime executionTime;

    // SUCCESS, FAILURE, TIMEOUT, ALERT_TRIGGERED
    @Enumerated(EnumType.STRING)
    private Status status;

    private Long executionDurationMs;

    @Lob
    private String resultSummary;

    private boolean alertSent;

    public enum Status {
        SUCCESS,
        FAILURE,
        TIMEOUT,
        ALERT_TRIGGERED
    }
}
