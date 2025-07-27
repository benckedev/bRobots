package dev.bencke.robots.menus;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.models.Robot;
import dev.bencke.robots.utils.ColorUtil;
import dev.bencke.robots.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RobotMenu {

    private final RobotPlugin plugin;
    private final Robot robot;
    private final Player player;

    @Getter
    private final Inventory inventory;
    private final Map<Integer, Consumer<Player>> clickHandlers = new HashMap<>();

    public RobotMenu(RobotPlugin plugin, Robot robot, Player player) {
        this.plugin = plugin;
        this.robot = robot;
        this.player = player;

        ConfigurationSection config = plugin.getConfigManager().getMenuConfig()
                .getConfigurationSection("robot-menu");

        String title = ColorUtil.colorize(config.getString("title", "&8Robot Menu")
                .replace("%robot%", robot.getType().getDisplayName())
                .replace("%level%", String.valueOf(robot.getLevel())));

        this.inventory = Bukkit.createInventory(null, config.getInt("size", 54), title);

        setupMenu(config);
    }

    private void setupMenu(ConfigurationSection config) {
        // Info item
        setupInfoItem(config.getConfigurationSection("items.info"));

        // Storage button
        setupStorageButton(config.getConfigurationSection("items.storage-button"));

        // Level up button
        setupLevelUpButton(config.getConfigurationSection("items.level-up"));

        // Fuel indicator
        setupFuelIndicator(config.getConfigurationSection("items.fuel"));
    }

    private void setupInfoItem(ConfigurationSection section) {
        if (section == null) return;

        int slot = section.getInt("slot");

        List<String> lore = section.getStringList("lore").stream()
                .map(line -> line
                        .replace("%level%", String.valueOf(robot.getLevel()))
                        .replace("%fuel%", String.valueOf(robot.getFuel()))
                        .replace("%max_fuel%", String.valueOf(robot.getType().getFuelCapacity()))
                        .replace("%storage%", String.valueOf(getStorageCount()))
                        .replace("%max_storage%", String.valueOf(getMaxStorage()))
                        .replace("%owner%", robot.getOwnerName())
                )
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());

        ItemStack item = new ItemBuilder(Material.valueOf(section.getString("material", "PAPER")))
                .name(ColorUtil.colorize(section.getString("name")))
                .lore(lore)
                .build();

        inventory.setItem(slot, item);
    }

    private void setupStorageButton(ConfigurationSection section) {
        if (section == null) return;

        int slot = section.getInt("slot");

        List<String> lore = section.getStringList("lore").stream()
                .map(line -> line
                        .replace("%storage%", String.valueOf(getStorageCount()))
                        .replace("%max_storage%", String.valueOf(getMaxStorage()))
                )
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());

        ItemStack item = new ItemBuilder(Material.valueOf(section.getString("material", "CHEST")))
                .name(ColorUtil.colorize(section.getString("name")))
                .lore(lore)
                .build();

        inventory.setItem(slot, item);

        clickHandlers.put(slot, p -> {
            p.playSound(p.getLocation(), Sound.CHEST_OPEN, 1f, 1f);
            new RobotStorageMenu(plugin, robot, p).open();
        });
    }

    private void setupLevelUpButton(ConfigurationSection section) {
        if (section == null) return;

        int slot = section.getInt("slot");

        if (robot.canLevelUp()) {
            double cost = robot.getType().getLevels().get(robot.getLevel() + 1).getCost();

            List<String> lore = section.getStringList("lore").stream()
                    .map(line -> line
                            .replace("%next_level%", String.valueOf(robot.getLevel() + 1))
                            .replace("%cost%", String.format("%.2f", cost))
                    )
                    .map(ColorUtil::colorize)
                    .collect(Collectors.toList());

            ItemStack item = new ItemBuilder(Material.valueOf(section.getString("material", "EXP_BOTTLE")))
                    .name(ColorUtil.colorize(section.getString("name")))
                    .lore(lore)
                    .build();

            inventory.setItem(slot, item);

            clickHandlers.put(slot, p -> {
                if (robot.levelUp(p)) {
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 1f, 1f);
                    p.sendMessage(ColorUtil.colorize(
                            plugin.getConfigManager().getMessage("robot-leveled-up")
                                    .replace("%level%", String.valueOf(robot.getLevel()))
                    ));

                    // Refresh menu
                    new RobotMenu(plugin, robot, p).open();
                } else {
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                    p.sendMessage(ColorUtil.colorize(
                            plugin.getConfigManager().getMessage("insufficient-funds")
                    ));
                }
            });
        } else {
            ItemStack item = new ItemBuilder(Material.BARRIER)
                    .name(ColorUtil.colorize(section.getString("max-level-name", "&cMax Level")))
                    .lore(ColorUtil.colorize(section.getStringList("max-level-lore")))
                    .build();

            inventory.setItem(slot, item);
        }
    }

    private void setupFuelIndicator(ConfigurationSection section) {
        if (section == null) return;

        int slot = section.getInt("slot");
        long fuel = robot.getFuel();
        long maxFuel = robot.getType().getFuelCapacity();
        double percentage = (double) fuel / maxFuel * 100;

        Material material = Material.valueOf(section.getString("material", "COAL"));
        short data = 0;

        if (percentage > 75) {
            data = 5; // Green
        } else if (percentage > 50) {
            data = 4; // Yellow
        } else if (percentage > 25) {
            data = 1; // Orange
        } else {
            data = 14; // Red
        }

        List<String> lore = section.getStringList("lore").stream()
                .map(line -> line
                        .replace("%fuel%", String.valueOf(fuel))
                        .replace("%max_fuel%", String.valueOf(maxFuel))
                        .replace("%percentage%", String.format("%.1f", percentage))
                )
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());

        ItemStack item = new ItemBuilder(Material.INK_SACK)
                .durability(data)
                .name(ColorUtil.colorize(section.getString("name")))
                .lore(lore)
                .build();

        inventory.setItem(slot, item);
    }

    private int getStorageCount() {
        return robot.getStorage().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int getMaxStorage() {
        return robot.getType().getLevels().get(robot.getLevel()).getStorageLimit();
    }

    public void handleClick(int slot, Player player) {
        Consumer<Player> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(player);
        }
    }

    public void open() {
        player.openInventory(inventory);
        plugin.getMenuManager().registerMenu(player, this);
    }

    private ItemStack createItem(ConfigurationSection section) {
        if (section == null) {
            return new ItemStack(Material.AIR);
        }

        return new ItemBuilder(Material.valueOf(section.getString("material", "STAINED_GLASS_PANE")))
                .durability((short) section.getInt("data", 0))
                .name(ColorUtil.colorize(section.getString("name", " ")))
                .lore(ColorUtil.colorize(section.getStringList("lore")))
                .build();
    }
}