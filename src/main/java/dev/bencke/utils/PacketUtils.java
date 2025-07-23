package dev.bencke.utils;

import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PacketUtils {

    public static void sendParticles(Location loc, EnumParticle particle, int count,
                                     float offsetX, float offsetY, float offsetZ, float speed) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                particle,
                true,
                (float) loc.getX(),
                (float) loc.getY(),
                (float) loc.getZ(),
                offsetX,
                offsetY,
                offsetZ,
                speed,
                count
        );

        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) < 256) { // 16 blocks
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
            }
        }
    }
}