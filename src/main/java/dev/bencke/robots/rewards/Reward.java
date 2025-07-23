package dev.bencke.robots.rewards;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.utils.ColorUtil;
import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class Reward {

    private final String id;
    private final String name;
    private final double chance;
    private final RewardType type;
    @Getter
    private final List<RewardAction> actions;

    public Reward(String id, ConfigurationSection section) {
        this.id = id;
        this.name = ColorUtil.colorize(section.getString("name", id));
        this.chance = section.getDouble("chance", 100.0);
        this.type = RewardType.valueOf(section.getString("type", "ITEM").toUpperCase());
        this.actions = new ArrayList<>();

        List<String> actionList = section.getStringList("actions");
        for (String actionStr : actionList) {
            actions.add(parseAction(actionStr));
        }
    }

    public Reward(String id, double chance, RewardType type, List<RewardAction> actions) {
        this.id = id;
        this.name = id;
        this.chance = chance;
        this.type = type;
        this.actions = actions;
    }

    private RewardAction parseAction(String actionStr) {
        if (type == RewardType.ITEM) {
            return new ItemRewardAction(actionStr);
        } else if (type == RewardType.COMMAND) {
            return new CommandRewardAction(actionStr);
        }
        return null;
    }

    public List<ItemStack> generate() {
        List<ItemStack> items = new ArrayList<>();

        for (RewardAction action : actions) {
            if (action instanceof ItemRewardAction) {
                ItemStack item = ((ItemRewardAction) action).generate();
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return items;
    }

    public void executeCommands(Player player) {
        for (RewardAction action : actions) {
            if (action instanceof CommandRewardAction) {
                ((CommandRewardAction) action).execute(player);
            }
        }
    }

    public ItemStack getDisplayItem() {
        if (type == RewardType.ITEM && !actions.isEmpty()) {
            RewardAction firstAction = actions.get(0);
            if (firstAction instanceof ItemRewardAction) {
                return ((ItemRewardAction) firstAction).getDisplayItem();
            }
        }

        // Default display item
        return new ItemStack(Material.CHEST);
    }

    public enum RewardType {
        ITEM,
        COMMAND
    }

    private interface RewardAction {
        // Marker interface
    }

    @Data
    private static class ItemRewardAction implements RewardAction {
        private final Material material;
        private final int amount;
        private final short data;
        private final String displayName;
        private final List<String> lore;

        public ItemRewardAction(String config) {
            String[] parts = config.split(",");

            // Format: MATERIAL,amount,data,name,lore1|lore2
            this.material = Material.valueOf(parts[0]);
            this.amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            this.data = parts.length > 2 ? Short.parseShort(parts[2]) : 0;
            this.displayName = parts.length > 3 ? ColorUtil.colorize(parts[3]) : null;

            this.lore = new ArrayList<>();
            if (parts.length > 4) {
                String[] loreArray = parts[4].split("\\|");
                for (String line : loreArray) {
                    lore.add(ColorUtil.colorize(line));
                }
            }
        }

        public ItemStack generate() {
            ItemStack item = new ItemStack(material, amount, data);

            if (displayName != null || !lore.isEmpty()) {
                ItemMeta meta = item.getItemMeta();

                if (displayName != null) {
                    meta.setDisplayName(displayName);
                }

                if (!lore.isEmpty()) {
                    meta.setLore(lore);
                }

                item.setItemMeta(meta);
            }

            return item;
        }

        public ItemStack getDisplayItem() {
            ItemStack item = new ItemStack(material, 1, data);
            ItemMeta meta = item.getItemMeta();

            if (displayName != null) {
                meta.setDisplayName(displayName);
            }

            List<String> displayLore = new ArrayList<>(lore);
            displayLore.add("");
            displayLore.add("&7Amount: &e" + amount);

            meta.setLore(displayLore.stream()
                    .map(ColorUtil::colorize)
                    .collect(Collectors.toList()));

            item.setItemMeta(meta);
            return item;
        }
    }

    @Data
    private static class CommandRewardAction implements RewardAction {
        private final String command;
        private final boolean console;

        public CommandRewardAction(String config) {
            if (config.startsWith("[CONSOLE]")) {
                this.console = true;
                this.command = config.substring(9).trim();
            } else {
                this.console = false;
                this.command = config;
            }
        }

        public void execute(Player player) {
            String finalCommand = command.replace("%player%", player.getName());

            Bukkit.getScheduler().runTask(RobotPlugin.getInstance(), () -> {
                if (console) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                } else {
                    player.performCommand(finalCommand);
                }
            });
        }
    }
}