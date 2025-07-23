package dev.bencke.robots.listeners;

import dev.bencke.robots.RobotPlugin;
import org.bukkit.event.Listener;

public class ListenerManager {

    private final RobotPlugin plugin;

    public ListenerManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        registerListener(new RobotInteractListener(plugin));
        registerListener(new PlayerJoinQuitListener(plugin));
        registerListener(plugin.getMenuManager());
    }

    private void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}