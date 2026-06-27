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

public class RegisterCommand implements CommandExecutor {

    private final XAuth plugin;
    private final AuthManager authManager;
    private final DatabaseManager databaseManager;
    private final SessionManager sessionManager;
    private final ConfigManager configManager;

    public RegisterCommand(XAuth plugin) {
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

        // Zaten kayıtlıysa
        if (databaseManager.isRegistered(player.getName())) {
            player.sendMessage(configManager.getMessage("register-already-registered"));
            return true;
        }

        // Kullanım kontrolü
        if (args.length < 2) {
            player.sendMessage(configManager.getMessage("register-usage"));
            return true;
        }

        String password = args[0];
        String confirm = args[1];

        // Şifre uzunluk kontrolleri
        int minLen = configManager.getMinPasswordLength();
        int maxLen = configManager.getMaxPasswordLength();

        if (password.length() < minLen) {
            player.sendMessage(configManager.getMessage("register-password-too-short",
                    "{min}", String.valueOf(minLen)));
            return true;
        }

        if (password.length() > maxLen) {
            player.sendMessage(configManager.getMessage("register-password-too-long",
                    "{max}", String.valueOf(maxLen)));
            return true;
        }

        // Şifre eşleşme kontrolü
        if (!password.equals(confirm)) {
            player.sendMessage(configManager.getMessage("register-passwords-not-match"));
            return true;
        }

        String ip = authManager.getPlayerIp(player);

        // Kayıt işlemi
        if (authManager.register(player.getName(), password, ip)) {
            // Timeout iptal et
            sessionManager.cancelLoginTimeout(player.getUniqueId());

            // Giriş yaptır
            sessionManager.setLoggedIn(player.getUniqueId(), ip);

            // Körlüğü kaldır
            player.removePotionEffect(PotionEffectType.BLINDNESS);

            player.sendMessage(configManager.getMessage("register-success",
                    "{player}", player.getName()));
        } else {
            player.sendMessage(configManager.getMessage("register-already-registered"));
        }

        return true;
    }
}
