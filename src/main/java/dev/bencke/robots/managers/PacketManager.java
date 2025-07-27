package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.utils.PacketUtils;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Location;

public class PacketManager {

    private final RobotPlugin plugin;

    public PacketManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendParticlePacket(Location location, EnumParticle particle, int count) {
        PacketUtils.sendParticles(location, particle, count, 0.5f, 0.5f, 0.5f, 0.1f);
    }

    public void sendParticleToNearby(Location location, EnumParticle particle, int count, double range) {
        PacketUtils.sendParticles(location, particle, count, 0.5f, 0.5f, 0.5f, 0.1f);
    }
}