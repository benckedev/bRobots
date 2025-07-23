package dev.bencke.robots.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Base64;

public class ItemSerializer {

    public static String serialize(ItemStack item) {
        if (item == null) return null;

        StringBuilder builder = new StringBuilder();
        builder.append(item.getType().name());
        builder.append(";").append(item.getAmount());
        builder.append(";").append(item.getDurability());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                builder.append(";name:").append(Base64.getEncoder().encodeToString(meta.getDisplayName().getBytes()));
            }
            if (meta.hasLore()) {
                builder.append(";lore:").append(Base64.getEncoder().encodeToString(meta.getLore().toString().getBytes()));
            }
        }

        return builder.toString();
    }

    public static ItemStack deserialize(String serialized) {
        if (serialized == null || serialized.isEmpty()) return null;

        String[] parts = serialized.split(";");
        if (parts.length < 3) return null;

        try {
            Material material = Material.valueOf(parts[0]);
            int amount = Integer.parseInt(parts[1]);
            short durability = Short.parseShort(parts[2]);

            ItemStack item = new ItemStack(material, amount, durability);

            // Parse metadata if present
            if (parts.length > 3) {
                ItemMeta meta = item.getItemMeta();

                for (int i = 3; i < parts.length; i++) {
                    String part = parts[i];
                    if (part.startsWith("name:")) {
                        String name = new String(Base64.getDecoder().decode(part.substring(5)));
                        meta.setDisplayName(name);
                    } else if (part.startsWith("lore:")) {
                        // Simple lore handling - in production you'd want more robust parsing
                        String lore = new String(Base64.getDecoder().decode(part.substring(5)));
                        // Set lore from string (simplified)
                    }
                }

                item.setItemMeta(meta);
            }

            return item;
        } catch (Exception e) {
            return null;
        }
    }
}