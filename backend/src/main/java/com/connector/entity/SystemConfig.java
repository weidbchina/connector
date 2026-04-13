package com.connector.entity;

import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "system_config")
public class SystemConfig {
    @Id
    private String configKey;
    private String configValue;
}
