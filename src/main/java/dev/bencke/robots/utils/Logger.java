package dev.bencke.robots.utils;

import dev.bencke.robots.RobotPlugin;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class Logger {

    private static final String PREFIX = "[bRobots] ";

    public static void info(String message) {
        Bukkit.getLogger().log(Level.INFO, PREFIX + message);
    }

    public static void warning(String message) {
        Bukkit.getLogger().log(Level.WARNING, PREFIX + message);
    }

    public static void severe(String message) {
        Bukkit.getLogger().log(Level.SEVERE, PREFIX + message);
    }

    public static void debug(String message) {
        if (RobotPlugin.getInstance().getConfig().getBoolean("debug", false)) {
            Bukkit.getLogger().log(Level.INFO, PREFIX + "[DEBUG] " + message);
        }
    }
}
