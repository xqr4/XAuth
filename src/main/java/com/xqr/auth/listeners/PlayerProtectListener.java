package com.xqr.auth.listeners;

import com.xqr.auth.XAuth;
import com.xqr.auth.managers.ConfigManager;
import com.xqr.auth.managers.SessionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProtectListener implements Listener {

    private final XAuth plugin;
    private final SessionManager sessionManager;
    private final ConfigManager configManager;

    // Dondurma için spawn konumu: UUID -> Location
    private final Map<UUID, Location> frozenLocations = new HashMap<>();

    public PlayerProtectListener(XAuth plugin) {
        this.plugin = plugin;
        this.sessionManager = plugin.getSessionManager();
        this.configManager = plugin.getConfigManager();
    }

    private boolean isAuthenticated(Player player) {
        return sessionManager.isLoggedIn(player.getUniqueId());
    }

    // --- Hareket engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!configManager.isFreezeEnabled()) return;
        Player player = event.getPlayer();
        if (isAuthenticated(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Sadece x, y, z hareketini engelle (baş hareketi serbest)
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            Location fixed = from.clone();
            fixed.setPitch(to.getPitch());
            fixed.setYaw(to.getYaw());
            event.setTo(fixed);
        }
    }

    // --- Sohbet engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (isAuthenticated(player)) return;
        event.setCancelled(true);
        player.sendMessage(configManager.getMessage("no-chat"));
    }

    // --- Komut engeli (sadece /register ve /login'e izin ver) ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isAuthenticated(player)) return;

        String cmd = event.getMessage().toLowerCase().trim();
        if (cmd.startsWith("/login") || cmd.startsWith("/register")) return;

        event.setCancelled(true);
        if (plugin.getDatabaseManager().isRegistered(player.getName())) {
            player.sendMessage(configManager.getMessage("login-required"));
        } else {
            player.sendMessage(configManager.getMessage("register-required"));
        }
    }

    // --- Blok kırma / koyma engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // --- Hasar engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!isAuthenticated(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (!isAuthenticated(player)) {
                event.setCancelled(true);
            }
        }
    }

    // --- Envanter engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (!isAuthenticated(player)) {
                event.setCancelled(true);
            }
        }
    }

    // --- Item düşürme engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // --- Item toplama engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // --- Interact engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    // --- Food level değişimi (açlık) engeli ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!isAuthenticated(player)) {
                event.setCancelled(true);
            }
        }
    }
}
