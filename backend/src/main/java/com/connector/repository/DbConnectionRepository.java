package com.connector.repository;

import com.connector.entity.DbConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbConnectionRepository extends JpaRepository<DbConnection, Long> {
}
