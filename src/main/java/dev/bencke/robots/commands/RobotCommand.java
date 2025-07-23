package dev.bencke.robots.commands;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.config.FuelType;
import dev.bencke.robots.config.RobotType;
import dev.bencke.robots.utils.ColorUtil;
import lombok.var;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RobotCommand implements CommandExecutor, TabCompleter {

    private final RobotPlugin plugin;

    public RobotCommand(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                handleGive(sender, args);
                break;
            case "fuel":
                handleFuel(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "list":
                handleList(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("robots.give")) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("no-permission")
            ));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /robot give <player> <type> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("player-not-found")
                            .replace("%player%", args[1])
            ));
            return;
        }

        String typeId = args[2];
        RobotType type = plugin.getConfigManager().getRobotTypes().get(typeId);

        if (type == null) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("invalid-robot-type")
                            .replace("%type%", typeId)
            ));
            return;
        }

        int amount = args.length > 3 ? parseInt(args[3], 1) : 1;

        for (int i = 0; i < amount; i++) {
            ItemStack robotItem = plugin.getRobotManager().createRobotItem(type);

            if (target.getInventory().firstEmpty() != -1) {
                target.getInventory().addItem(robotItem);
            } else {
                target.getWorld().dropItem(target.getLocation(), robotItem);
            }
        }

        sender.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getMessage("robot-given")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%type%", type.getDisplayName())
                        .replace("%player%", target.getName())
        ));

        target.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getMessage("robot-received")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%type%", type.getDisplayName())
        ));
    }

    private void handleFuel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("robots.fuel")) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("no-permission")
            ));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /robot fuel <player> <type> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("player-not-found")
                            .replace("%player%", args[1])
            ));
            return;
        }

        String fuelId = args[2];
        FuelType fuel = plugin.getConfigManager().getFuelTypes().get(fuelId);

        if (fuel == null) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("invalid-fuel-type")
                            .replace("%type%", fuelId)
            ));
            return;
        }

        int amount = parseInt(args[3], 1);

        for (int i = 0; i < amount; i++) {
            ItemStack fuelItem = plugin.getFuelManager().createFuelItem(fuel);

            if (target.getInventory().firstEmpty() != -1) {
                target.getInventory().addItem(fuelItem);
            } else {
                target.getWorld().dropItem(target.getLocation(), fuelItem);
            }
        }

        sender.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getMessage("fuel-given")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%type%", fuel.getDisplayName())
                        .replace("%player%", target.getName())
        ));

        target.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getMessage("fuel-received")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%type%", fuel.getDisplayName())
        ));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("robots.reload")) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("no-permission")
            ));
            return;
        }

        plugin.getConfigManager().loadAll();
        sender.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getMessage("config-reloaded")
        ));
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("robots.list")) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("no-permission")
            ));
            return;
        }

        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getMessage("player-not-found")
                                .replace("%player%", args[1])
                ));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ColorUtil.colorize("&cConsole must specify a player!"));
            return;
        }

        var robots = plugin.getRobotManager().getPlayerRobots(target.getUniqueId());

        if (robots.isEmpty()) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("no-robots")
                            .replace("%player%", target.getName())
            ));
            return;
        }

        sender.sendMessage(ColorUtil.colorize(
                "&7&m-----&r &eRobots of " + target.getName() + " &7&m-----"
        ));

        robots.forEach(robot -> {
            sender.sendMessage(ColorUtil.colorize(
                    "&7- &f" + robot.getType().getDisplayName() +
                            " &7(Level " + robot.getLevel() +
                            ", Fuel: " + robot.getFuel() + "/" + robot.getType().getFuelCapacity() + ")"
            ));
        });
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("robots.remove")) {
            sender.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("no-permission")
            ));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.colorize("&cOnly players can use this command!"));
            return;
        }

        Player player = (Player) sender;

        // Implementation would include targeting mechanism to select and remove robots
        sender.sendMessage(ColorUtil.colorize("&eRight-click a robot to remove it."));

        // Store player in removal mode
        plugin.getPlayerManager().setRemovalMode(player, true);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&7&m-----&r &eRobot Commands &7&m-----"));

        if (sender.hasPermission("robots.give")) {
            sender.sendMessage(ColorUtil.colorize("&e/robot give <player> <type> [amount] &7- Give robot items"));
        }

        if (sender.hasPermission("robots.fuel")) {
            sender.sendMessage(ColorUtil.colorize("&e/robot fuel <player> <type> <amount> &7- Give fuel items"));
        }

        if (sender.hasPermission("robots.list")) {
            sender.sendMessage(ColorUtil.colorize("&e/robot list [player] &7- List robots"));
        }

        if (sender.hasPermission("robots.remove")) {
            sender.sendMessage(ColorUtil.colorize("&e/robot remove &7- Remove a robot"));
        }

        if (sender.hasPermission("robots.reload")) {
            sender.sendMessage(ColorUtil.colorize("&e/robot reload &7- Reload configuration"));
        }
    }

    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "fuel", "list", "remove", "reload"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("fuel") ||
                    args[0].equalsIgnoreCase("list")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(plugin.getConfigManager().getRobotTypes().keySet());
            } else if (args[0].equalsIgnoreCase("fuel")) {
                completions.addAll(plugin.getConfigManager().getFuelTypes().keySet());
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}