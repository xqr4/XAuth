package com.xqr.auth.managers;

import com.xqr.auth.XAuth;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    private final XAuth plugin;

    // Giriş yapmış oyuncular: UUID -> IP
    private final Map<UUID, String> loggedIn = new HashMap<>();

    // Oturum süresi takibi: UUID -> BukkitTask
    private final Map<UUID, BukkitTask> sessionTasks = new HashMap<>();

    // Login timeout takibi: UUID -> BukkitTask
    private final Map<UUID, BukkitTask> loginTimeoutTasks = new HashMap<>();

    // Son giriş zamanı (anti-bot): username -> timestamp (ms)
    private final Map<String, Long> lastJoinTime = new HashMap<>();

    // Giriş deneme sayısı: UUID -> count
    private final Map<UUID, Integer> loginAttempts = new HashMap<>();

    public SessionManager(XAuth plugin) {
        this.plugin = plugin;
    }

    // --- Giriş durumu ---

    public boolean isLoggedIn(UUID uuid) {
        return loggedIn.containsKey(uuid);
    }

    public void setLoggedIn(UUID uuid, String ip) {
        loggedIn.put(uuid, ip);
        cancelLoginTimeout(uuid);

        int timeoutMinutes = plugin.getConfigManager().getSessionTimeout();
        if (timeoutMinutes > 0) {
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                loggedIn.remove(uuid);
                sessionTasks.remove(uuid);
            }, timeoutMinutes * 60L * 20L);
            sessionTasks.put(uuid, task);
        }
    }

    public void setLoggedOut(UUID uuid) {
        loggedIn.remove(uuid);
        BukkitTask sessionTask = sessionTasks.remove(uuid);
        if (sessionTask != null) sessionTask.cancel();
        cancelLoginTimeout(uuid);
        loginAttempts.remove(uuid);
    }

    // --- Login timeout ---

    public void startLoginTimeout(UUID uuid, String playerName, int seconds) {
        if (seconds <= 0) return;
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            loginTimeoutTasks.remove(uuid);
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline() && !isLoggedIn(uuid)) {
                player.kickPlayer(plugin.getConfigManager().getMessage("login-timeout"));
            }
        }, seconds * 20L);
        loginTimeoutTasks.put(uuid, task);
    }

    public void startRegisterTimeout(UUID uuid, int seconds) {
        if (seconds <= 0) return;
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            loginTimeoutTasks.remove(uuid);
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline() && !isLoggedIn(uuid)) {
                player.kickPlayer(plugin.getConfigManager().getMessage("register-timeout"));
            }
        }, seconds * 20L);
        loginTimeoutTasks.put(uuid, task);
    }

    public void cancelLoginTimeout(UUID uuid) {
        BukkitTask task = loginTimeoutTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    // --- Giriş denemeleri ---

    public int getLoginAttempts(UUID uuid) {
        return loginAttempts.getOrDefault(uuid, 0);
    }

    public int incrementLoginAttempts(UUID uuid) {
        int attempts = loginAttempts.getOrDefault(uuid, 0) + 1;
        loginAttempts.put(uuid, attempts);
        return attempts;
    }

    public void resetLoginAttempts(UUID uuid) {
        loginAttempts.remove(uuid);
    }

    // --- Anti-bot ---

    public boolean checkJoinDelay(String username) {
        if (!plugin.getConfigManager().isAntiBotEnabled()) return true;

        long now = System.currentTimeMillis();
        long minDelay = plugin.getConfigManager().getMinJoinDelay() * 1000L;
        Long last = lastJoinTime.get(username.toLowerCase());

        if (last != null && (now - last) < minDelay) {
            return false; // Çok hızlı giriş
        }
        lastJoinTime.put(username.toLowerCase(), now);
        return true;
    }

    public void removeJoinTime(String username) {
        lastJoinTime.remove(username.toLowerCase());
    }

    // --- Oturum tabanlı otomatik login ---

    public boolean trySessionLogin(UUID uuid, String username, String currentIp) {
        if (!plugin.getConfigManager().isSessionEnabled()) return false;

        String lastIp = plugin.getDatabaseManager().getLastIp(username);
        if (lastIp != null && lastIp.equals(currentIp)) {
            setLoggedIn(uuid, currentIp);
            return true;
        }
        return false;
    }
}
