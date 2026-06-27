package com.xqr.auth.managers;

import com.xqr.auth.XAuth;
import org.mindrot.jbcrypt.BCrypt;

public class AuthManager {

    private final XAuth plugin;

    public AuthManager(XAuth plugin) {
        this.plugin = plugin;
    }

    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public boolean checkPassword(String password, String hashed) {
        try {
            return BCrypt.checkpw(password, hashed);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean register(String username, String password, String ip) {
        String hashed = hashPassword(password);
        return plugin.getDatabaseManager().registerPlayer(username, hashed, ip);
    }

    public boolean login(String username, String password) {
        String storedHash = plugin.getDatabaseManager().getPassword(username);
        if (storedHash == null) return false;
        return checkPassword(password, storedHash);
    }

    public String getPlayerIp(org.bukkit.entity.Player player) {
        if (player.getAddress() == null) return "unknown";
        return player.getAddress().getAddress().getHostAddress();
    }
}
