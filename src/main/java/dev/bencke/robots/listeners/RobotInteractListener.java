package dev.bencke.robots.listeners;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.config.FuelType;
import dev.bencke.robots.config.RobotType;
import dev.bencke.robots.menus.RobotMenu;
import dev.bencke.robots.models.Robot;
import dev.bencke.robots.utils.ColorUtil;
import dev.bencke.robots.utils.NBTUtil;
import lombok.var;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class RobotInteractListener implements Listener {

    private final RobotPlugin plugin;

    public RobotInteractListener(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRobotPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        String robotTypeId = NBTUtil.getString(item, "robot_type");
        if (robotTypeId == null) {
            return;
        }

        event.setCancelled(true);

        RobotType type = plugin.getConfigManager().getRobotTypes().get(robotTypeId);
        if (type == null) {
            return;
        }

        Block block = event.getClickedBlock();
        Location spawnLoc = block.getLocation().add(0.5, 1, 0.5);

        // Check world restrictions
        if (!isWorldAllowed(spawnLoc.getWorld().getName())) {
            player.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("world-not-allowed")
            ));
            return;
        }

        // Check max robots per player
        int maxRobots = plugin.getConfigManager().getMainConfig()
                .getInt("performance.max-robots-per-player", 0);

        if (maxRobots > 0) {
            int currentRobots = plugin.getRobotManager()
                    .getPlayerRobots(player.getUniqueId()).size();

            if (currentRobots >= maxRobots) {
                player.sendMessage(ColorUtil.colorize(
                        "&cYou have reached the maximum number of robots (" + maxRobots + ")!"
                ));
                return;
            }
        }

        // Check for existing robot data
        String robotData = NBTUtil.getString(item, "robot_data");
        Robot robot;

        if (robotData != null) {
            // Restore existing robot
            robot = deserializeRobotData(robotData, player, type, spawnLoc);
            if (robot == null) {
                return;
            }
        } else {
            // Create new robot
            robot = plugin.getRobotManager().createRobot(player, type, spawnLoc);
        }

        if (robot != null) {
            // Remove item from inventory
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.setItemInHand(null);
            }

            player.playSound(spawnLoc, Sound.ITEM_PICKUP, 1f, 0.5f);
            player.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("robot-placed")
            ));
        }
    }

    @EventHandler
    public void onRobotInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand)) {
            return;
        }

        Player player = event.getPlayer();
        Robot robot = plugin.getRobotManager().getRobotFromEntity(entity);

        if (robot == null) {
            return;
        }

        event.setCancelled(true);

        ItemStack hand = player.getItemInHand();

        // Check for fuel
        if (hand != null && hand.getType() != Material.AIR) {
            String fuelId = NBTUtil.getString(hand, "fuel_type");

            if (fuelId != null) {
                FuelType fuel = plugin.getConfigManager().getFuelTypes().get(fuelId);

                if (fuel != null) {
                    // Add fuel
                    robot.addFuel(fuel.getDuration());

                    // Remove fuel item
                    if (hand.getAmount() > 1) {
                        hand.setAmount(hand.getAmount() - 1);
                    } else {
                        player.setItemInHand(null);
                    }

                    player.sendMessage(ColorUtil.colorize(
                            plugin.getConfigManager().getMessage("fuel-added")
                                    .replace("%amount%", String.valueOf(fuel.getDuration()))
                    ));
                    return;
                }
            }
        }

        // Open robot menu
        if (robot.getOwnerId().equals(player.getUniqueId()) ||
                player.hasPermission("robots.admin")) {
            new RobotMenu(plugin, robot, player).open();
        }
    }

    @EventHandler
    public void onRobotBreak(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) {
            return;
        }

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        Robot robot = plugin.getRobotManager().getRobotFromEntity(event.getEntity());

        if (robot == null) {
            return;
        }

        event.setCancelled(true);

        // Check removal mode
        if (plugin.getPlayerManager().isInRemovalMode(player)) {
            if (player.hasPermission("robots.admin") ||
                    robot.getOwnerId().equals(player.getUniqueId())) {

                removeRobot(player, robot);
                plugin.getPlayerManager().setRemovalMode(player, false);
            } else {
                player.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getMessage("cannot-break-others")
                ));
            }
            return;
        }

        // Normal break - only owner can break
        if (!robot.getOwnerId().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("cannot-break-others")
            ));
            return;
        }

        // Give robot item back with data
        removeRobot(player, robot);
    }

    private void removeRobot(Player player, Robot robot) {
        ItemStack robotItem = plugin.getRobotManager().createRobotItemWithData(robot);

        // Give item to player
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(robotItem);
        } else {
            player.getWorld().dropItem(player.getLocation(), robotItem);
        }

        // Remove robot
        plugin.getRobotManager().removeRobot(robot);

        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);
        player.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getMessage("robot-pickup")
        ));
    }

    private boolean isWorldAllowed(String worldName) {
        var allowedWorlds = plugin.getConfigManager().getMainConfig()
                .getStringList("allowed-worlds");

        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName);
    }

    private Robot deserializeRobotData(String data, Player player, RobotType type, Location location) {
        // Implementation would deserialize the robot data
        // For now, create a new robot
        return plugin.getRobotManager().createRobot(player, type, location);
    }
}