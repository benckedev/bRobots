package dev.bencke.robots.listeners;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.config.FuelType;
import dev.bencke.robots.config.RobotType;
import dev.bencke.robots.menus.RobotMenu;
import dev.bencke.robots.models.Robot;
import dev.bencke.robots.utils.ColorUtil;
import dev.bencke.robots.utils.Logger;
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

import java.util.Base64;

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
                    int fuelAmount = plugin.getFuelManager().getFuelAmount(hand);
                    if (fuelAmount <= 0) {
                        fuelAmount = (int) fuel.getDuration(); // Fallback para combustível antigo
                    }

                    // Calculate how much fuel can be added
                    long currentFuel = robot.getFuel();
                    long maxFuel = robot.getType().getFuelCapacity();
                    long canAdd = maxFuel - currentFuel;

                    if (canAdd <= 0) {
                        player.sendMessage(ColorUtil.colorize(
                                "&cThe robot's fuel tank is already full!"
                        ));
                        return;
                    }

                    // Calculate actual amount to add and remainder
                    long toAdd = Math.min(canAdd, fuelAmount);
                    long remainder = fuelAmount - toAdd;

                    // Add fuel to robot
                    robot.addFuel(toAdd);

                    // Handle item update
                    if (remainder > 0) {
                        // Create new fuel item with remainder
                        ItemStack newFuel = plugin.getFuelManager().createFuelItem(fuel, (int) remainder);
                        player.setItemInHand(newFuel);
                    } else {
                        // Remove fuel item completely
                        if (hand.getAmount() > 1) {
                            hand.setAmount(hand.getAmount() - 1);
                        } else {
                            player.setItemInHand(null);
                        }
                    }

                    player.sendMessage(ColorUtil.colorize(
                            plugin.getConfigManager().getMessage("fuel-added")
                                    .replace("%amount%", String.valueOf(toAdd))
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
        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            String jsonData = new String(decoded);

            // Parse o JSON manualmente ou use uma biblioteca
            // Por simplicidade, vamos criar um novo robot
            // Em produção, você deveria parsear os dados salvos

            Robot robot = plugin.getRobotManager().createRobot(player, type, location);

            // TODO: Aplicar os dados salvos (level, fuel, storage, etc)
            // robot.setLevel(savedLevel);
            // robot.setFuel(savedFuel);
            // etc...

            return robot;
        } catch (Exception e) {
            Logger.warning("Failed to deserialize robot data: " + e.getMessage());
            return plugin.getRobotManager().createRobot(player, type, location);
        }
    }
}