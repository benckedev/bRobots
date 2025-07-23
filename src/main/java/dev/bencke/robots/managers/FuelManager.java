package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.config.FuelType;
import dev.bencke.robots.utils.ItemBuilder;
import dev.bencke.robots.utils.NBTUtil;
import org.bukkit.inventory.ItemStack;

public class FuelManager {

    private final RobotPlugin plugin;

    public FuelManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createFuelItem(FuelType fuelType) {
        ItemBuilder builder = new ItemBuilder(fuelType.getMaterial())
                .durability(fuelType.getData())
                .name(fuelType.getDisplayName())
                .lore(fuelType.getDescription())
                .nbt("fuel_type", fuelType.getId())
                .nbt("fuel_duration", String.valueOf(fuelType.getDuration()));

        if (fuelType.isGlow()) {
            builder.glow(true);
        }

        return builder.build();
    }

    public FuelType getFuelTypeFromItem(ItemStack item) {
        if (item == null) return null;

        String fuelId = NBTUtil.getString(item, "fuel_type");
        if (fuelId == null) return null;

        return plugin.getConfigManager().getFuelTypes().get(fuelId);
    }
}