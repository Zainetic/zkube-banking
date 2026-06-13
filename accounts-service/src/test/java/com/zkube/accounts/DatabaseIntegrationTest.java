package com.zkube.accounts;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseIntegrationTest {

    // This spins up a real Postgres Docker container just for this test!
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:16-alpine"
    );

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @Test
    void shouldConnectAndCreateAccountTable() throws Exception {
        // Arrange: Connect to the throwaway container
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            
            // Act: Build the table and insert a row
            stmt.execute("CREATE TABLE accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), owner_name VARCHAR(100), balance DECIMAL(15,2));");
            stmt.execute("INSERT INTO accounts (owner_name, balance) VALUES ('Alice', 1000.00);");

            // Assert: Verify the row exists
            ResultSet rs = stmt.executeQuery("SELECT * FROM accounts WHERE owner_name = 'Alice';");
            assertTrue(rs.next(), "Alice should exist in the database");
        }
    }
}