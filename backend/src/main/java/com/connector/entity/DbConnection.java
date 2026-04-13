package com.connector.entity;

import com.connector.util.CryptoConverter;
import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "db_connections")
public class DbConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String jdbcUrl;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    @Convert(converter = CryptoConverter.class)
    private String password;

    private String description;
}
