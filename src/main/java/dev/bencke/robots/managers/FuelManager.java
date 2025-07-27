package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.config.FuelType;
import dev.bencke.robots.items.FuelItem;
import dev.bencke.robots.utils.NBTUtil;
import org.bukkit.inventory.ItemStack;

public class FuelManager {

    private final RobotPlugin plugin;

    public FuelManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createFuelItem(FuelType fuelType) {
        return createFuelItem(fuelType, (int) (fuelType.getDuration() * fuelType.getInternalAmount()));
    }

    public ItemStack createFuelItem(FuelType fuelType, int amount) {
        return FuelItem.create(fuelType, amount);
    }

    public FuelType getFuelTypeFromItem(ItemStack item) {
        if (item == null) return null;

        String fuelId = NBTUtil.getString(item, "fuel_type");
        if (fuelId == null) return null;

        return plugin.getConfigManager().getFuelTypes().get(fuelId);
    }

    public int getFuelAmount(ItemStack item) {
        return FuelItem.getFuelAmount(item);
    }
}