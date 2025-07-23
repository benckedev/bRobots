package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.menus.RobotMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuManager implements Listener {

    private final RobotPlugin plugin;
    private final Map<UUID, RobotMenu> openMenus = new HashMap<>();

    public MenuManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerMenu(Player player, RobotMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
    }

    public void unregisterMenu(Player player) {
        openMenus.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        RobotMenu menu = openMenus.get(player.getUniqueId());

        if (menu != null && event.getInventory().equals(menu.getInventory())) {
            event.setCancelled(true);

            if (event.getCurrentItem() != null) {
                menu.handleClick(event.getSlot(), player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            unregisterMenu((Player) event.getPlayer());
        }
    }
}