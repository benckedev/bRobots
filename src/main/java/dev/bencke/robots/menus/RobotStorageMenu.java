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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RobotStorageMenu {

    private final RobotPlugin plugin;
    private final Robot robot;
    private final Player player;

    @Getter
    private final Inventory inventory;
    private final Map<Integer, Consumer<Player>> clickHandlers = new HashMap<>();

    public RobotStorageMenu(RobotPlugin plugin, Robot robot, Player player) {
        this.plugin = plugin;
        this.robot = robot;
        this.player = player;

        ConfigurationSection config = plugin.getConfigManager().getMenuConfig()
                .getConfigurationSection("storage-menu");

        if (config == null) {
            this.inventory = Bukkit.createInventory(null, 54, "Robot Storage");
            return;
        }

        String title = ColorUtil.colorize(config.getString("title", "&8Robot Storage")
                .replace("%robot%", robot.getType().getDisplayName())
                .replace("%storage%", String.valueOf(getStorageCount()))
                .replace("%max_storage%", String.valueOf(getMaxStorage())));

        this.inventory = Bukkit.createInventory(null, config.getInt("size", 54), title);
        setupMenu(config);
    }

    private void setupMenu(ConfigurationSection config) {
        // Info item
        setupInfoItem(config.getConfigurationSection("items.info"));

        // Storage button
        setupStorageDisplay(config.getConfigurationSection("items.storage"));

        // Collect all button
        setupCollectAllButton(config.getConfigurationSection("items.collect-all"));

        // Back button
        setupBackButton(config.getConfigurationSection("items.back"));
    }

    private void setupInfoItem(ConfigurationSection section) {
        if (section == null) return;

        int slot = section.getInt("slot");

        List<String> lore = section.getStringList("lore").stream()
                .map(line -> line
                        .replace("%storage%", String.valueOf(getStorageCount()))
                        .replace("%max_storage%", String.valueOf(getMaxStorage()))
                        .replace("%robot%", robot.getType().getDisplayName())
                        .replace("%level%", String.valueOf(robot.getLevel()))
                )
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());

        ItemStack item = new ItemBuilder(Material.valueOf(section.getString("material", "CHEST")))
                .name(ColorUtil.colorize(section.getString("name")))
                .lore(lore)
                .build();

        inventory.setItem(slot, item);
    }

    private void setupStorageDisplay(ConfigurationSection section) {
        if (section == null) return;

        List<Integer> slots = section.getIntegerList("slots");
        Map<ItemStack, Integer> robotStorage = robot.getStorage();

        int index = 0;

        for (Map.Entry<ItemStack, Integer> entry : robotStorage.entrySet()) {
            if (index >= slots.size()) break;

            ItemStack originalItem = entry.getKey();
            Integer amount = entry.getValue();

            if (originalItem == null || originalItem.getType() == Material.AIR) continue;

            // Create display item
            ItemStack display = originalItem.clone();
            display.setAmount(Math.min(amount, 64)); // Max stack size for display

            // Add amount to lore
            List<String> lore = new ArrayList<>();
            if (display.hasItemMeta() && display.getItemMeta().hasLore()) {
                lore.addAll(display.getItemMeta().getLore());
            }

            lore.add("");
            lore.add(ColorUtil.colorize("&7Total Amount: &e" + amount));
            lore.add(ColorUtil.colorize("&7Click to collect!"));

            display = new ItemBuilder(display).lore(lore).build();

            int slotIndex = slots.get(index);
            inventory.setItem(slotIndex, display);

            // Add click handler to collect specific item
            final ItemStack finalItem = originalItem.clone();
            final int finalAmount = amount;
            clickHandlers.put(slotIndex, p -> collectSpecificItem(p, finalItem, finalAmount));

            index++;
        }
    }

    private void setupCollectAllButton(ConfigurationSection section) {
        if (section == null) return;

        int slot = section.getInt("slot");

        if (!robot.getStorage().isEmpty()) {
            List<String> lore = section.getStringList("lore").stream()
                    .map(line -> line
                            .replace("%storage%", String.valueOf(getStorageCount()))
                    )
                    .map(ColorUtil::colorize)
                    .collect(Collectors.toList());

            ItemStack item = new ItemBuilder(Material.valueOf(section.getString("material", "HOPPER")))
                    .name(ColorUtil.colorize(section.getString("name")))
                    .lore(lore)
                    .build();

            inventory.setItem(slot, item);

            clickHandlers.put(slot, p -> {
                collectAllItems(p);
                p.playSound(p.getLocation(), Sound.CHEST_OPEN, 1f, 1f);
                // Refresh menu
                new RobotStorageMenu(plugin, robot, p).open();
            });
        } else {
            ItemStack item = new ItemBuilder(Material.BARRIER)
                    .name(ColorUtil.colorize(section.getString("empty-name", "&cNo Items")))
                    .lore(ColorUtil.colorize(section.getStringList("empty-lore")))
                    .build();

            inventory.setItem(slot, item);
        }
    }

    private void setupBackButton(ConfigurationSection section) {
        if (section == null) return;

        int slot = section.getInt("slot");

        ItemStack item = new ItemBuilder(Material.valueOf(section.getString("material", "ARROW")))
                .name(ColorUtil.colorize(section.getString("name")))
                .lore(ColorUtil.colorize(section.getStringList("lore")))
                .build();

        inventory.setItem(slot, item);

        clickHandlers.put(slot, p -> {
            p.playSound(p.getLocation(), Sound.CLICK, 1f, 1f);
            new RobotMenu(plugin, robot, p).open();
        });
    }

    private void collectSpecificItem(Player player, ItemStack item, int amount) {
        // Find the exact key in storage
        ItemStack storageKey = null;
        for (ItemStack key : robot.getStorage().keySet()) {
            if (isSimilarItem(key, item)) {
                storageKey = key;
                break;
            }
        }

        if (storageKey == null) {
            player.sendMessage(ColorUtil.colorize("&cItem not found in storage!"));
            return;
        }

        ItemStack toGive = item.clone();
        toGive.setAmount(amount);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);

        if (leftover.isEmpty()) {
            // All items collected
            robot.getStorage().remove(storageKey);
            player.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("specific-item-collected")
                            .replace("%amount%", String.valueOf(amount))
                            .replace("%item%", item.getType().name())
            ));
        } else {
            // Some items couldn't fit
            int collected = amount;
            for (ItemStack left : leftover.values()) {
                collected -= left.getAmount();
            }

            if (collected > 0) {
                // Update storage with remaining amount
                int remaining = amount - collected;
                robot.getStorage().put(storageKey, remaining);

                player.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getMessage("partial-item-collected")
                                .replace("%collected%", String.valueOf(collected))
                                .replace("%remaining%", String.valueOf(remaining))
                                .replace("%item%", item.getType().name())
                ));
            } else {
                player.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getMessage("inventory-full")
                ));
            }
        }

        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);

        // Refresh menu
        new RobotStorageMenu(plugin, robot, player).open();
    }

    private boolean isSimilarItem(ItemStack item1, ItemStack item2) {
        if (item1.getType() != item2.getType()) return false;
        if (item1.getDurability() != item2.getDurability()) return false;

        // Check metadata
        if (item1.hasItemMeta() != item2.hasItemMeta()) return false;
        if (item1.hasItemMeta()) {
            return item1.getItemMeta().equals(item2.getItemMeta());
        }

        return true;
    }

    private void collectAllItems(Player player) {
        Map<ItemStack, Integer> storage = new HashMap<>(robot.getStorage());
        robot.getStorage().clear();

        int totalCollected = 0;
        int totalRemaining = 0;

        for (Map.Entry<ItemStack, Integer> entry : storage.entrySet()) {
            ItemStack item = entry.getKey().clone();
            item.setAmount(entry.getValue());

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);

            int itemsCollected = entry.getValue();

            if (!leftover.isEmpty()) {
                // Add back items that couldn't fit
                for (ItemStack left : leftover.values()) {
                    robot.getStorage().put(entry.getKey(), left.getAmount());
                    totalRemaining += left.getAmount();
                    itemsCollected -= left.getAmount();
                }
            }

            totalCollected += itemsCollected;
        }

        if (totalCollected > 0) {
            player.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("items-collected")
                            .replace("%collected%", String.valueOf(totalCollected))
            ));
        }

        if (totalRemaining > 0) {
            player.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getMessage("some-items-remaining")
                            .replace("%remaining%", String.valueOf(totalRemaining))
            ));
        }
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