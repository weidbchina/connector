package com.connector.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "monitor_tasks")
public class MonitorTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "connection_id", nullable = false)
    private DbConnection dbConnection;

    @Lob
    @Column(nullable = false)
    private String monitorSql;

    @Column(nullable = false)
    private String cronExpression;

    // Timeout in seconds
    private Integer timeoutSeconds = 30;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "task_id")
    private List<TaskValidationRule> validationRules = new ArrayList<>();

    // Alert Config
    private String alertPhoneNumbers; // Comma separated
    private String alertTemplate;
    
    // Rate limiting: Min interval between alerts in minutes
    private Integer alertIntervalMinutes = 10;

    private boolean active = true;
}
