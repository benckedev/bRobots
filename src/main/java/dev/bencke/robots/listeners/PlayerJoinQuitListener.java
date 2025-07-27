package dev.bencke.robots.listeners;

import dev.bencke.robots.RobotPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final RobotPlugin plugin;

    public PlayerJoinQuitListener(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player's robots asynchronously
        plugin.runAsync(() -> {
            plugin.getRobotManager().loadPlayerRobots(player);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save and unload player's robots
        plugin.runAsync(() -> {
            plugin.getRobotManager().savePlayerRobots(player);
        });

        // Remove from removal mode if applicable
        plugin.getPlayerManager().setRemovalMode(player, false);
    }
}