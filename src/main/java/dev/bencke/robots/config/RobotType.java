package dev.bencke.robots.config;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RobotType {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final Material material;
    private final String headTexture;
    private final ItemStack chestplate;
    private final ItemStack leggings;
    private final ItemStack boots;
    private final ItemStack itemInHand;
    private final long fuelCapacity;
    private final Map<Integer, LevelData> levels = new HashMap<>();

    public RobotType(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = section.getString("display-name", "&7Robot");
        this.description = section.getStringList("description");
        this.material = Material.valueOf(section.getString("item.material", "ARMOR_STAND"));
        this.headTexture = section.getString("appearance.head-texture");
        this.fuelCapacity = section.getLong("fuel-capacity", 1000);

        // Load equipment
        this.chestplate = loadItem(section, "appearance.chestplate");
        this.leggings = loadItem(section, "appearance.leggings");
        this.boots = loadItem(section, "appearance.boots");
        this.itemInHand = loadItem(section, "appearance.item-in-hand");

        // Load levels
        ConfigurationSection levelsSection = section.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String levelKey : levelsSection.getKeys(false)) {
                int levelNum = Integer.parseInt(levelKey);
                ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);

                LevelData levelData = new LevelData(
                        levelSection.getDouble("cost", 0),
                        levelSection.getInt("generation-delay", 60),
                        levelSection.getStringList("rewards"),
                        levelSection.getInt("storage-limit", 256)
                );

                levels.put(levelNum, levelData);
            }
        }
    }

    private ItemStack loadItem(ConfigurationSection section, String path) {
        if (!section.contains(path)) {
            return null;
        }

        ConfigurationSection itemSection = section.getConfigurationSection(path);
        if (itemSection == null) {
            return null;
        }

        Material material = Material.valueOf(itemSection.getString("material"));
        ItemStack item = new ItemStack(material);

        if (itemSection.contains("data")) {
            item.setDurability((short) itemSection.getInt("data"));
        }

        return item;
    }

    @Data
    public static class LevelData {
        private final double cost;
        private final int generationDelay; // in seconds
        private final List<String> rewards;
        private final int storageLimit;
    }
}