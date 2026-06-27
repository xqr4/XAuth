package com.xqr.auth.commands;

import com.xqr.auth.XAuth;
import com.xqr.auth.managers.AuthManager;
import com.xqr.auth.managers.ConfigManager;
import com.xqr.auth.managers.DatabaseManager;
import com.xqr.auth.managers.SessionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class LoginCommand implements CommandExecutor {

    private final XAuth plugin;
    private final AuthManager authManager;
    private final DatabaseManager databaseManager;
    private final SessionManager sessionManager;
    private final ConfigManager configManager;

    public LoginCommand(XAuth plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
        this.databaseManager = plugin.getDatabaseManager();
        this.sessionManager = plugin.getSessionManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komutu sadece oyuncular kullanabilir.");
            return true;
        }

        // Zaten giriş yaptıysa
        if (sessionManager.isLoggedIn(player.getUniqueId())) {
            player.sendMessage(configManager.getMessage("already-logged-in"));
            return true;
        }

        // Kayıt kontrolü
        if (!databaseManager.isRegistered(player.getName())) {
            player.sendMessage(configManager.getMessage("register-required"));
            return true;
        }

        // Argüman kontrolü
        if (args.length < 1) {
            player.sendMessage(configManager.getMessage("login-required"));
            return true;
        }

        String password = args[0];
        String ip = authManager.getPlayerIp(player);

        // Şifre doğrula
        if (authManager.login(player.getName(), password)) {
            // Başarılı giriş
            sessionManager.cancelLoginTimeout(player.getUniqueId());
            sessionManager.resetLoginAttempts(player.getUniqueId());
            sessionManager.setLoggedIn(player.getUniqueId(), ip);

            // Veritabanında last login güncelle
            databaseManager.updateLastLogin(player.getName(), ip);

            // Körlüğü kaldır
            player.removePotionEffect(PotionEffectType.BLINDNESS);

            player.sendMessage(configManager.getMessage("login-success",
                    "{player}", player.getName()));
        } else {
            // Hatalı şifre
            int attempts = sessionManager.incrementLoginAttempts(player.getUniqueId());
            int maxAttempts = configManager.getMaxLoginAttempts();

            if (attempts >= maxAttempts) {
                sessionManager.setLoggedOut(player.getUniqueId());
                player.kickPlayer(configManager.getMessage("login-too-many-attempts"));
            } else {
                int remaining = maxAttempts - attempts;
                player.sendMessage(configManager.getMessage("login-wrong-password",
                        "{attempts}", String.valueOf(remaining)));
            }
        }

        return true;
    }
}
