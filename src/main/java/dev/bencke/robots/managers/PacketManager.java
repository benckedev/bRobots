package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PacketManager {

    private final RobotPlugin plugin;

    public PacketManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendParticlePacket(Player player, Location location, EnumParticle particle, int count) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                particle,
                true,
                (float) location.getX(),
                (float) location.getY(),
                (float) location.getZ(),
                0.5f, 0.5f, 0.5f,
                0.1f,
                count
        );

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public void sendParticleToNearby(Location location, EnumParticle particle, int count, double range) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= range * range) {
                sendParticlePacket(player, location, particle, count);
            }
        }
    }
}