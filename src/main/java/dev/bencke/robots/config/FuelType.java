package dev.bencke.robots.config;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Data
public class FuelType {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final Material material;
    private final short data;
    private final long duration;
    private final boolean glow;

    public FuelType(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = section.getString("display-name", "&7Fuel");
        this.description = section.getStringList("description");
        this.material = Material.valueOf(section.getString("material", "COAL"));
        this.data = (short) section.getInt("data", 0);
        this.duration = section.getLong("duration", 100);
        this.glow = section.getBoolean("glow", false);
    }
}