package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.config.RobotType;
import dev.bencke.robots.models.Robot;
import dev.bencke.robots.utils.ItemBuilder;
import dev.bencke.robots.utils.Logger;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RobotManager {

    private final RobotPlugin plugin;

    @Getter
    private final Map<UUID, Robot> robots = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerRobots = new ConcurrentHashMap<>();
    @Getter
    private final Map<UUID, Robot> entityToRobot = new ConcurrentHashMap<>();

    private BukkitRunnable tickTask;

    public RobotManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTickTask() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                robots.values().parallelStream().forEach(Robot::tick);
            }
        };

        tickTask.runTaskTimerAsynchronously(plugin, 20L, 20L); // Every second
    }

    public Robot createRobot(Player player, RobotType type, Location location) {
        // Check world restrictions
        List<String> allowedWorlds = plugin.getConfigManager().getMainConfig()
                .getStringList("allowed-worlds");

        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(location.getWorld().getName())) {
            return null;
        }

        Robot robot = new Robot(player.getUniqueId(), player.getName(), type, location);

        // Register robot
        robots.put(robot.getId(), robot);
        playerRobots.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(robot.getId());

        // Spawn in world
        robot.spawn();

        if (robot.getEntity() != null) {
            entityToRobot.put(robot.getEntity().getUniqueId(), robot);
        }

        // Save to database
        plugin.getDatabaseManager().saveRobot(robot);

        return robot;
    }

    public void removeRobot(Robot robot) {
        robot.remove();

        robots.remove(robot.getId());

        Set<UUID> ownerRobots = playerRobots.get(robot.getOwnerId());
        if (ownerRobots != null) {
            ownerRobots.remove(robot.getId());
        }

        if (robot.getEntity() != null) {
            entityToRobot.remove(robot.getEntity().getUniqueId());
        }

        // Delete from database
        plugin.getDatabaseManager().deleteRobot(robot.getId());
    }

    public void savePlayerRobots(Player player) {
        Set<Robot> playerRobotSet = getPlayerRobots(player.getUniqueId());

        for (Robot robot : playerRobotSet) {
            // Save to database
            plugin.getDatabaseManager().saveRobot(robot);
        }
    }

    public Robot getRobotFromEntity(Entity entity) {
        if (!(entity instanceof ArmorStand)) {
            return null;
        }

        return entityToRobot.get(entity.getUniqueId());
    }

    public Set<Robot> getPlayerRobots(UUID playerId) {
        Set<UUID> robotIds = playerRobots.get(playerId);
        if (robotIds == null) {
            return new HashSet<>();
        }

        return robotIds.stream()
                .map(robots::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void loadPlayerRobots(Player player) {
        plugin.getDatabaseManager().loadRobots(player.getUniqueId())
                .thenAccept(loadedRobots -> {
                    for (Robot robot : loadedRobots) {
                        // Check if robot is already loaded
                        if (robots.containsKey(robot.getId())) {
                            // Robot already active, just update owner data if needed
                            Robot existingRobot = robots.get(robot.getId());
                            existingRobot.setOwnerName(player.getName());
                            continue;
                        }

                        // New robot, load it
                        robots.put(robot.getId(), robot);
                        playerRobots.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                                .add(robot.getId());

                        // Spawn if in loaded chunk
                        if (robot.getLocation().getChunk().isLoaded()) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                robot.spawn();
                                if (robot.getEntity() != null) {
                                    entityToRobot.put(robot.getEntity().getUniqueId(), robot);
                                }
                            });
                        }
                    }

                    Logger.info("Loaded " + loadedRobots.size() + " robots for " + player.getName());
                });
    }

    public void unloadPlayerRobots(Player player) {
        Set<Robot> playerRobotSet = getPlayerRobots(player.getUniqueId());

        for (Robot robot : playerRobotSet) {
            // Save to database
            plugin.getDatabaseManager().saveRobot(robot);

            // Remove from world
            robot.remove();

            // Remove from memory
            robots.remove(robot.getId());
            if (robot.getEntity() != null) {
                entityToRobot.remove(robot.getEntity().getUniqueId());
            }
        }

        playerRobots.remove(player.getUniqueId());
    }

    public ItemStack createRobotItem(RobotType type) {
        return new ItemBuilder(type.getMaterial())
                .name(type.getDisplayName())
                .lore(type.getDescription())
                .nbt("robot_type", type.getId())
                .glow(true)
                .build();
    }

    public ItemStack createRobotItemWithData(Robot robot) {
        RobotType type = robot.getType();

        List<String> lore = new ArrayList<>(type.getDescription());
        lore.add("");
        lore.add("&7Level: &e" + robot.getLevel());
        lore.add("&7Fuel: &e" + robot.getFuel() + "/" + type.getFuelCapacity());
        lore.add("&7Storage: &e" + getStorageCount(robot) + "/" +
                type.getLevels().get(robot.getLevel()).getStorageLimit());

        return new ItemBuilder(type.getMaterial())
                .name(type.getDisplayName())
                .lore(lore)
                .nbt("robot_type", type.getId())
                .nbt("robot_data", serializeRobotData(robot))
                .glow(true)
                .build();
    }

    private int getStorageCount(Robot robot) {
        return robot.getStorage().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    private String serializeRobotData(Robot robot) {
        Map<String, Object> data = robot.serialize();

        // Criar um formato simples de serialização
        StringBuilder sb = new StringBuilder();
        sb.append("level=").append(robot.getLevel()).append(";");
        sb.append("fuel=").append(robot.getFuel()).append(";");

        // Serializar storage
        robot.getStorage().forEach((item, amount) -> {
            sb.append("item_").append(item.getType().name())
                    .append("_").append(amount).append(";");
        });

        return Base64.getEncoder().encodeToString(sb.toString().getBytes());
    }

    public void saveAll() {
        robots.values().forEach(robot ->
                plugin.getDatabaseManager().saveRobot(robot)
        );
    }

    public void removeAll() {
        robots.values().forEach(Robot::remove);
        robots.clear();
        playerRobots.clear();
        entityToRobot.clear();
    }

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }
    }
}