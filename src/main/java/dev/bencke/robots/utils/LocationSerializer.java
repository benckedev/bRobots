package dev.bencke.robots.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationSerializer {

    public static String serialize(Location loc) {
        if (loc == null) return null;

        return loc.getWorld().getName() + ";" +
                loc.getX() + ";" +
                loc.getY() + ";" +
                loc.getZ() + ";" +
                loc.getYaw() + ";" +
                loc.getPitch();
    }

    public static Location deserialize(String str) {
        if (str == null || str.isEmpty()) return null;

        String[] parts = str.split(";");
        if (parts.length < 6) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}