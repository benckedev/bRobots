package dev.bencke.robots.items;

import dev.bencke.robots.config.FuelType;
import dev.bencke.robots.utils.ItemBuilder;
import dev.bencke.robots.utils.NBTUtil;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FuelItem {

    public static ItemStack create(FuelType fuelType, int amount) {
        List<String> lore = new ArrayList<>(fuelType.getDescription());
        lore.add("");
        lore.add("&7Fuel Amount: &e" + amount + " units");

        ItemBuilder builder = new ItemBuilder(fuelType.getMaterial())
                .durability(fuelType.getData())
                .name(fuelType.getDisplayName() + " &7(" + amount + " units)")
                .lore(lore)
                .nbt("fuel_type", fuelType.getId())
                .nbt("fuel_amount", String.valueOf(amount));

        if (fuelType.isGlow()) {
            builder.glow(true);
        }

        return builder.build();
    }

    public static int getFuelAmount(ItemStack item) {
        String amount = NBTUtil.getString(item, "fuel_amount");
        if (amount == null) return 0;

        try {
            return Integer.parseInt(amount);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}