package com.xqr.auth;

import com.xqr.auth.commands.LoginCommand;
import com.xqr.auth.commands.RegisterCommand;
import com.xqr.auth.commands.XAuthCommand;
import com.xqr.auth.listeners.PlayerAuthListener;
import com.xqr.auth.listeners.PlayerProtectListener;
import com.xqr.auth.managers.AuthManager;
import com.xqr.auth.managers.ConfigManager;
import com.xqr.auth.managers.DatabaseManager;
import com.xqr.auth.managers.SessionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class XAuth extends JavaPlugin {

    private static XAuth instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        instance = this;

        // Config yükle
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // Veritabanı başlat
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Veritabanı başlatılamadı! Plugin devre dışı bırakılıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Manager'ları başlat
        this.sessionManager = new SessionManager(this);
        this.authManager = new AuthManager(this);

        // Komutları kaydet
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("xauth").setExecutor(new XAuthCommand(this));

        // Listener'ları kaydet
        getServer().getPluginManager().registerEvents(new PlayerAuthListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerProtectListener(this), this);

        getLogger().info("╔══════════════════════════════╗");
        getLogger().info("║   XAuth v1.0.0 Aktif!        ║");
        getLogger().info("║   Geliştirici: xqr            ║");
        getLogger().info("╚══════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("XAuth devre dışı bırakıldı.");
    }

    public static XAuth getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
