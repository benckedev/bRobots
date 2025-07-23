package dev.bencke.robots.config;

import dev.bencke.robots.RobotPlugin;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ConfigManager {

    private final RobotPlugin plugin;
    private FileConfiguration mainConfig;
    private FileConfiguration robotsConfig;
    private FileConfiguration rewardsConfig;
    private FileConfiguration fuelConfig;
    private FileConfiguration menuConfig;

    private final Map<String, RobotType> robotTypes = new HashMap<>();
    private final Map<String, FuelType> fuelTypes = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();

    public ConfigManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        createConfigs();
        loadConfigs();
        loadRobotTypes();
        loadFuelTypes();
        loadMessages();
    }

    private void createConfigs() {
        saveDefaultConfig("config.yml");
        saveDefaultConfig("robots.yml");
        saveDefaultConfig("rewards.yml");
        saveDefaultConfig("fuel.yml");
        saveDefaultConfig("menus.yml");
    }

    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private void loadConfigs() {
        mainConfig = plugin.getConfig();
        robotsConfig = loadConfig("robots.yml");
        rewardsConfig = loadConfig("rewards.yml");
        fuelConfig = loadConfig("fuel.yml");
        menuConfig = loadConfig("menus.yml");
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }

    private void loadRobotTypes() {
        robotTypes.clear();
        ConfigurationSection section = robotsConfig.getConfigurationSection("robots");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection robotSection = section.getConfigurationSection(key);
                RobotType type = new RobotType(key, robotSection);
                robotTypes.put(key, type);
            }
        }
    }

    private void loadFuelTypes() {
        fuelTypes.clear();
        ConfigurationSection section = fuelConfig.getConfigurationSection("fuel");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection fuelSection = section.getConfigurationSection(key);
                FuelType type = new FuelType(key, fuelSection);
                fuelTypes.put(key, type);
            }
        }
    }

    private void loadMessages() {
        messages.clear();
        ConfigurationSection section = mainConfig.getConfigurationSection("messages");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                messages.put(key, section.getString(key));
            }
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    public void saveConfig(String fileName, FileConfiguration config) {
        try {
            File file = new File(plugin.getDataFolder(), fileName);
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}