package com.moneymap.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            String host = System.getProperty("DB_HOST", System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost");
            String port = System.getProperty("DB_PORT", System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "5432");
            String dbName = System.getProperty("DB_NAME", System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "moneymap");
            String user = System.getProperty("DB_USER", System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres");
            String password = System.getProperty("DB_PASSWORD", System.getenv("DB_PASSWORD"));

            if (password == null || password.isEmpty()) {
                throw new IllegalStateException("DB_PASSWORD environment variable or system property is required but not set.");
            }

            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName));
            config.setUsername(user);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(30000);
            config.setMaxLifetime(1800000);

            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            System.err.println("[DBConnection] Warning: Could not initialize HikariCP connection pool: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection pool is not initialized. Please ensure PostgreSQL is running.");
        }
        return dataSource.getConnection();
    }

    public static boolean isConnected() {
        if (dataSource == null) return false;
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
