package com.zkube.accounts;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;

public class Database {
    private static HikariDataSource dataSource;

    public static void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://postgres-db:5432/zkube_chaos");
        config.setUsername("postgres");
        config.setPassword("admin");
        
        // Connection pool settings optimized for microservices
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        
        dataSource = new HikariDataSource(config);
        System.out.println("Database Connection Pool Initialized.");
    }

    public static DSLContext getContext() {
        try {
            Connection connection = dataSource.getConnection();
            return DSL.using(connection, SQLDialect.POSTGRES);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
    }
}