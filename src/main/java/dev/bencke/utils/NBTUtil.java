package dev.bencke.utils;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NBTUtil {

    public static ItemStack setString(ItemStack item, String key, String value) {
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsItem.hasTag() ? nmsItem.getTag() : new NBTTagCompound();
        tag.setString(key, value);
        nmsItem.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    public static String getString(ItemStack item, String key) {
        if (item == null) return null;

        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        if (nmsItem == null || !nmsItem.hasTag()) return null;

        NBTTagCompound tag = nmsItem.getTag();
        return tag.hasKey(key) ? tag.getString(key) : null;
    }

    public static ItemStack setInt(ItemStack item, String key, int value) {
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsItem.hasTag() ? nmsItem.getTag() : new NBTTagCompound();
        tag.setInt(key, value);
        nmsItem.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    public static int getInt(ItemStack item, String key) {
        if (item == null) return 0;

        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        if (nmsItem == null || !nmsItem.hasTag()) return 0;

        NBTTagCompound tag = nmsItem.getTag();
        return tag.hasKey(key) ? tag.getInt(key) : 0;
    }
}