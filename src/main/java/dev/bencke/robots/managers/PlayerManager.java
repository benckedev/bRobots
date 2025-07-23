package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerManager {

    private final RobotPlugin plugin;
    private final Set<UUID> playersInRemovalMode = new HashSet<>();

    public PlayerManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void setRemovalMode(Player player, boolean enabled) {
        if (enabled) {
            playersInRemovalMode.add(player.getUniqueId());
        } else {
            playersInRemovalMode.remove(player.getUniqueId());
        }
    }

    public boolean isInRemovalMode(Player player) {
        return playersInRemovalMode.contains(player.getUniqueId());
    }
}