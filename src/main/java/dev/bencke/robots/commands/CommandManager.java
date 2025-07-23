package dev.bencke.robots.commands;

import dev.bencke.robots.RobotPlugin;

public class CommandManager {

    private final RobotPlugin plugin;

    public CommandManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        plugin.getCommand("robot").setExecutor(new RobotCommand(plugin));
        plugin.getCommand("robot").setTabCompleter(new RobotCommand(plugin));
    }
}