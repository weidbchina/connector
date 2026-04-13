package com.connector.entity;

import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "task_validation_rules")
public class TaskValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // THRESHOLD, EMPTY, NOT_EMPTY, REGEX, CONTAINS
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    // e.g., "> 1000", "some_regex", "error_value"
    private String expectedValue;

    public enum RuleType {
        THRESHOLD_COUNT, // Count > X
        RESULT_EMPTY,
        RESULT_NOT_EMPTY,
        REGEX_MATCH,
        CONTAINS_TEXT
    }
}
