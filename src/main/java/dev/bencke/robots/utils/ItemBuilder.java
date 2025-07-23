package dev.bencke.robots.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder durability(short durability) {
        item.setDurability(durability);
        return this;
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(ColorUtil.colorize(name));
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<String> lore) {
        meta.setLore(lore.stream()
                .map(ColorUtil::colorize)
                .collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(ColorUtil.colorize(line));
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder flag(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow) {
            enchant(Enchantment.DURABILITY, 1);
            flag(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder nbt(String key, String value) {
        item.setItemMeta(meta);
        NBTUtil.setString(item, key, value);
        return this;
    }

    public ItemBuilder skullTexture(String base64) {
        if (!(meta instanceof SkullMeta)) {
            return this;
        }

        SkullMeta skullMeta = (SkullMeta) meta;
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", base64));

        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public ItemBuilder skullOwner(String owner) {
        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwner(owner);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}