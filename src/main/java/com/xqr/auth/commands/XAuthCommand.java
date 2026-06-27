package com.xqr.auth.commands;

import com.xqr.auth.XAuth;
import com.xqr.auth.managers.ConfigManager;
import com.xqr.auth.managers.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XAuthCommand implements CommandExecutor, TabCompleter {

    private final XAuth plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    public XAuthCommand(XAuth plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("xauth.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unreg", "unregister" -> handleUnreg(sender, args);
            case "alts" -> handleAlts(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleUnreg(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("admin-usage"));
            return;
        }

        String target = args[1];

        if (!databaseManager.isRegistered(target)) {
            sender.sendMessage(configManager.getMessage("admin-unreg-not-found",
                    "{player}", target));
            return;
        }

        if (databaseManager.unregisterPlayer(target)) {
            sender.sendMessage(configManager.getMessage("admin-unreg-success",
                    "{player}", target));

            // Eğer oyuncu online ise onu da çıkart
            org.bukkit.entity.Player onlinePlayer = plugin.getServer().getPlayer(target);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                plugin.getSessionManager().setLoggedOut(onlinePlayer.getUniqueId());
                onlinePlayer.kickPlayer(configManager.getPrefix() +
                        ChatColor.RED + "Hesabınız yönetici tarafından silindi.");
            }
        }
    }

    private void handleAlts(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("admin-usage"));
            return;
        }

        String target = args[1];
        List<DatabaseManager.AltAccount> alts = databaseManager.getAltAccounts(target);

        if (alts.isEmpty()) {
            sender.sendMessage(configManager.getMessage("admin-alts-none",
                    "{player}", target));
            return;
        }

        sender.sendMessage(configManager.getMessage("admin-alts-header",
                "{player}", target));

        for (DatabaseManager.AltAccount alt : alts) {
            sender.sendMessage(configManager.getMessage("admin-alts-entry",
                    "{player}", alt.username,
                    "{ip}", alt.ip,
                    "{date}", alt.lastSeen));
        }
    }

    private void handleReload(CommandSender sender) {
        configManager.reload();
        sender.sendMessage(configManager.getPrefix() +
                ChatColor.GREEN + "XAuth yapılandırması yeniden yüklendi!");
    }

    private void sendHelp(CommandSender sender) {
        String prefix = configManager.getPrefix();
        sender.sendMessage(prefix + ChatColor.YELLOW + "XAuth Admin Komutları:");
        sender.sendMessage(ChatColor.GRAY + "  /xauth unreg <kullanıcı> " +
                ChatColor.WHITE + "- Oyuncunun kaydını sil");
        sender.sendMessage(ChatColor.GRAY + "  /xauth alts <kullanıcı> " +
                ChatColor.WHITE + "- Aynı IP'den giriş yapan hesapları göster");
        sender.sendMessage(ChatColor.GRAY + "  /xauth reload " +
                ChatColor.WHITE + "- Config'i yeniden yükle");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("xauth.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("unreg", "alts", "reload");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("unreg") || args[0].equalsIgnoreCase("alts"))) {
            List<String> players = new ArrayList<>();
            for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }

        return new ArrayList<>();
    }
}
