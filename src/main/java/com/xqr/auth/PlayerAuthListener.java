package com.xqr.auth.listeners;

import com.xqr.auth.XAuth;
import com.xqr.auth.managers.AuthManager;
import com.xqr.auth.managers.ConfigManager;
import com.xqr.auth.managers.DatabaseManager;
import com.xqr.auth.managers.SessionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerAuthListener implements Listener {

    private final XAuth plugin;
    private final SessionManager sessionManager;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final AuthManager authManager;

    public PlayerAuthListener(XAuth plugin) {
        this.plugin = plugin;
        this.sessionManager = plugin.getSessionManager();
        this.databaseManager = plugin.getDatabaseManager();
        this.configManager = plugin.getConfigManager();
        this.authManager = plugin.getAuthManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        String ip = authManager.getPlayerIp(player);

        // Anti-bot kontrolü
        if (!sessionManager.checkJoinDelay(name)) {
            player.kickPlayer(configManager.getAntiBotKickMessage());
            return;
        }

        boolean isRegistered = databaseManager.isRegistered(name);

        // Kayıtlı değilse kayıt isteği gönder
        if (!isRegistered) {
            // Körlük efekti
            applyBlindness(player);

            // Kayıt timeout başlat
            int timeout = configManager.getRegisterTimeout();
            sessionManager.startRegisterTimeout(player.getUniqueId(), timeout);

            // Mesaj gönder
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(configManager.getMessage("register-required"));
                }
            }, 20L);
            return;
        }

        // Oturum tabanlı otomatik giriş dene
        if (sessionManager.trySessionLogin(player.getUniqueId(), name, ip)) {
            databaseManager.updateLastLogin(name, ip);
            player.sendMessage(configManager.getMessage("session-restored"));
            return;
        }

        // Normal login gerekli
        applyBlindness(player);

        int timeout = configManager.getLoginTimeout();
        sessionManager.startLoginTimeout(player.getUniqueId(), name, timeout);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(configManager.getMessage("login-required"));
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        sessionManager.setLoggedOut(player.getUniqueId());
    }

    private void applyBlindness(Player player) {
        if (!configManager.isBlindnessEnabled()) return;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                Integer.MAX_VALUE, // Sürekli
                1,
                false,
                false
        ));
    }
}
