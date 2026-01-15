package com.ullivada.zeropanic.applister.database;

import com.ullivada.zeropanic.applister.model.InstalledApp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Manages SQLite database for user authentication and app tracking.
 * Modular design for easy Spring Boot migration.
 */
public class DatabaseService implements AutoCloseable {
    private final Connection connection;

    public DatabaseService(Path databasePath) throws SQLException {
        String url = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        this.connection = DriverManager.getConnection(url);
        initializeSchema();
    }

    public DatabaseService() throws SQLException {
        this(getDefaultDatabasePath());
    }

    private static Path getDefaultDatabasePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".applister", "applister.db");
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Users table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);

            // Apps table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS apps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    source TEXT NOT NULL,
                    app_id TEXT,
                    version TEXT,
                    origin TEXT,
                    last_seen_at INTEGER NOT NULL,
                    UNIQUE(name, source)
                )
                """);

            // Scan metadata
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS scan_metadata (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    last_scan_at INTEGER NOT NULL,
                    last_supabase_check_at INTEGER DEFAULT 0
                )
                """);
        }
    }

    /**
     * Create a new user with hashed password.
     */
    public void createUser(String username, String password) throws SQLException {
        String passwordHash = hashPassword(password);
        String sql = "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setLong(3, Instant.now().getEpochSecond());
            pstmt.executeUpdate();
        }
    }

    /**
     * Authenticate user with username and password.
     */
    public boolean authenticate(String username, String password) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String providedHash = hashPassword(password);
                    return storedHash.equals(providedHash);
                }
            }
        }
        return false;
    }

    /**
     * Check if last scan was within the specified hours.
     */
    public boolean wasScannedWithinHours(int hours) throws SQLException {
        String sql = "SELECT last_scan_at FROM scan_metadata WHERE id = 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long lastScanAt = rs.getLong("last_scan_at");
                long nowSeconds = Instant.now().getEpochSecond();
                long hoursDiff = (nowSeconds - lastScanAt) / 3600;
                return hoursDiff < hours;
            }
        }
        return false;
    }

    /**
     * Check if last Supabase check was within the specified hours.
     */
    public boolean wasSupabaseCheckedWithinHours(int hours) throws SQLException {
        String sql = "SELECT last_supabase_check_at FROM scan_metadata WHERE id = 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long lastCheckAt = rs.getLong("last_supabase_check_at");
                if (lastCheckAt == 0) {
                    return false; // Never checked
                }
                long nowSeconds = Instant.now().getEpochSecond();
                long hoursDiff = (nowSeconds - lastCheckAt) / 3600;
                return hoursDiff < hours;
            }
        }
        return false;
    }

    /**
     * Update Supabase check timestamp.
     */
    public void updateSupabaseCheckTimestamp() throws SQLException {
        long now = Instant.now().getEpochSecond();
        String sql = """
            INSERT INTO scan_metadata (id, last_scan_at, last_supabase_check_at) 
            VALUES (1, ?, ?)
            ON CONFLICT(id) DO UPDATE SET last_supabase_check_at = excluded.last_supabase_check_at
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, now); // Use current time for last_scan_at if inserting
            pstmt.setLong(2, now);
            pstmt.executeUpdate();
        }
    }

    /**
     * Update apps in database and record scan time.
     * Removes apps not in the provided list.
     */
    public void updateApps(List<InstalledApp> apps) throws SQLException {
        connection.setAutoCommit(false);
        try {
            long now = Instant.now().getEpochSecond();

            // Insert or update apps
            String upsertSql = """
                INSERT INTO apps (name, source, app_id, version, origin, last_seen_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(name, source) DO UPDATE SET
                    app_id = excluded.app_id,
                    version = excluded.version,
                    origin = excluded.origin,
                    last_seen_at = excluded.last_seen_at
                """;

            try (PreparedStatement pstmt = connection.prepareStatement(upsertSql)) {
                for (InstalledApp app : apps) {
                    pstmt.setString(1, app.name());
                    pstmt.setString(2, app.source());
                    pstmt.setString(3, app.id().orElse(null));
                    pstmt.setString(4, app.version().orElse(null));
                    pstmt.setString(5, app.origin().orElse(null));
                    pstmt.setLong(6, now);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // Delete apps not seen in this scan (older than current scan)
            String deleteSql = "DELETE FROM apps WHERE last_seen_at < ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
                pstmt.setLong(1, now);
                pstmt.executeUpdate();
            }

            // Update scan timestamp
            String scanSql = """
                INSERT INTO scan_metadata (id, last_scan_at) VALUES (1, ?)
                ON CONFLICT(id) DO UPDATE SET last_scan_at = excluded.last_scan_at
                """;
            try (PreparedStatement pstmt = connection.prepareStatement(scanSql)) {
                pstmt.setLong(1, now);
                pstmt.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Get all apps from database.
     */
    public List<InstalledApp> getAllApps() throws SQLException {
        List<InstalledApp> apps = new ArrayList<>();
        String sql = "SELECT name, source, app_id, version, origin FROM apps ORDER BY source, name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                apps.add(new InstalledApp(
                    rs.getString("name"),
                    rs.getString("source"),
                    Optional.ofNullable(rs.getString("app_id")),
                    Optional.ofNullable(rs.getString("version")),
                    Optional.ofNullable(rs.getString("origin"))
                ));
            }
        }
        return apps;
    }

    /**
     * Hash password using SHA-256.
     * Note: For production, use bcrypt or PBKDF2 via external library.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
