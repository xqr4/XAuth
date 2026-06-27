package com.xqr.auth.managers;

import com.xqr.auth.XAuth;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DatabaseManager {

    private final XAuth plugin;
    private Connection connection;
    private final String dbPath;

    public DatabaseManager(XAuth plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + File.separator +
                plugin.getConfigManager().getDatabaseFilename();
    }

    public boolean initialize() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            createTables();
            plugin.getLogger().info("Veritabanı başarıyla başlatıldı: " + dbPath);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Veritabanı bağlantısı kurulamadı!", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Oyuncu kayıt tablosu
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS xauth_players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    password TEXT NOT NULL,
                    ip TEXT,
                    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP
                )
            """);

            // IP geçmişi tablosu
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS xauth_ip_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL COLLATE NOCASE,
                    ip TEXT NOT NULL,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Index'ler
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_username ON xauth_players(username)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ip_history_username ON xauth_ip_history(username)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ip_history_ip ON xauth_ip_history(ip)");
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        }
        return connection;
    }

    // --- Oyuncu kayıt işlemleri ---

    public boolean isRegistered(String username) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT 1 FROM xauth_players WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "isRegistered hatası", e);
            return false;
        }
    }

    public boolean registerPlayer(String username, String hashedPassword, String ip) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO xauth_players (username, password, ip) VALUES (?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            ps.setString(3, ip);
            ps.executeUpdate();
            recordIpHistory(username, ip);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "registerPlayer hatası", e);
            return false;
        }
    }

    public String getPassword(String username) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT password FROM xauth_players WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "getPassword hatası", e);
        }
        return null;
    }

    public boolean unregisterPlayer(String username) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM xauth_players WHERE username = ?")) {
            ps.setString(1, username);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "unregisterPlayer hatası", e);
            return false;
        }
    }

    public void updateLastLogin(String username, String ip) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE xauth_players SET last_login = CURRENT_TIMESTAMP, ip = ? WHERE username = ?")) {
            ps.setString(1, ip);
            ps.setString(2, username);
            ps.executeUpdate();
            recordIpHistory(username, ip);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "updateLastLogin hatası", e);
        }
    }

    public String getLastIp(String username) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT ip FROM xauth_players WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("ip");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "getLastIp hatası", e);
        }
        return null;
    }

    // --- IP geçmişi ---

    public void recordIpHistory(String username, String ip) {
        try {
            // Mevcut kaydı güncelle ya da yeni ekle
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO xauth_ip_history (username, ip, last_seen) VALUES (?, ?, CURRENT_TIMESTAMP)")) {
                ps.setString(1, username);
                ps.setString(2, ip);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "recordIpHistory hatası", e);
        }
    }

    public List<AltAccount> getAltAccounts(String username) {
        List<AltAccount> alts = new ArrayList<>();
        try {
            // Hedef kullanıcının tüm IP'lerini bul
            List<String> ips = new ArrayList<>();
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "SELECT DISTINCT ip FROM xauth_ip_history WHERE username = ?")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ips.add(rs.getString("ip"));
                }
            }

            if (ips.isEmpty()) return alts;

            // O IP'lerden giriş yapan diğer hesapları bul
            String placeholders = String.join(",", ips.stream().map(ip -> "?").toArray(String[]::new));
            String query = String.format(
                    "SELECT h.username, h.ip, h.last_seen FROM xauth_ip_history h " +
                    "WHERE h.ip IN (%s) AND LOWER(h.username) != LOWER(?) " +
                    "ORDER BY h.last_seen DESC", placeholders);

            try (PreparedStatement ps = getConnection().prepareStatement(query)) {
                int idx = 1;
                for (String ip : ips) {
                    ps.setString(idx++, ip);
                }
                ps.setString(idx, username);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    alts.add(new AltAccount(
                            rs.getString("username"),
                            rs.getString("ip"),
                            rs.getString("last_seen")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "getAltAccounts hatası", e);
        }
        return alts;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Veritabanı kapatılamadı", e);
        }
    }

    // --- Inner class ---
    public static class AltAccount {
        public final String username;
        public final String ip;
        public final String lastSeen;

        public AltAccount(String username, String ip, String lastSeen) {
            this.username = username;
            this.ip = ip;
            this.lastSeen = lastSeen;
        }
    }
}
