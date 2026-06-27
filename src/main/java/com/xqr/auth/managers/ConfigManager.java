package com.xqr.auth.managers;

import com.xqr.auth.XAuth;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final XAuth plugin;
    private FileConfiguration config;

    public ConfigManager(XAuth plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // --- Database ---
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getDatabaseFilename() {
        return config.getString("database.filename", "xauth.db");
    }

    // --- Session ---
    public boolean isSessionEnabled() {
        return config.getBoolean("session.enabled", true);
    }

    public int getSessionTimeout() {
        return config.getInt("session.timeout", 60);
    }

    // --- Auth ---
    public int getMaxLoginAttempts() {
        return config.getInt("auth.max-login-attempts", 5);
    }

    public int getMinPasswordLength() {
        return config.getInt("auth.min-password-length", 6);
    }

    public int getMaxPasswordLength() {
        return config.getInt("auth.max-password-length", 32);
    }

    public int getLoginTimeout() {
        return config.getInt("auth.login-timeout", 60);
    }

    public int getRegisterTimeout() {
        return config.getInt("auth.register-timeout", 120);
    }

    // --- Anti-bot ---
    public boolean isAntiBotEnabled() {
        return config.getBoolean("anti-bot.enabled", true);
    }

    public int getMinJoinDelay() {
        return config.getInt("anti-bot.min-join-delay", 2);
    }

    public String getAntiBotKickMessage() {
        return colorize(config.getString("anti-bot.kick-message",
                "&cXAuth: Sunucuya Çok Hızlı Giriş Yapıyorsunuz. Lütfen Biraz Yavaşlayın!"));
    }

    // --- Blindness & Freeze ---
    public boolean isBlindnessEnabled() {
        return config.getBoolean("blindness.enabled", true);
    }

    public boolean isFreezeEnabled() {
        return config.getBoolean("freeze.enabled", true);
    }

    // --- Messages ---
    public String getPrefix() {
        return colorize(config.getString("messages.prefix", "&8[&bXAuth&8] &r"));
    }

    public String getMessage(String key) {
        String path = "messages." + key;
        String msg = config.getString(path, "&cMesaj bulunamadı: " + key);
        return getPrefix() + colorize(msg);
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return msg;
    }

    public String getRawMessage(String key) {
        return colorize(config.getString("messages." + key, ""));
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
