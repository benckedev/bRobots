package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.menus.RobotMenu;
import dev.bencke.robots.menus.RobotStorageMenu;
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
    private final Map<UUID, Object> openMenus = new HashMap<>();

    public MenuManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerMenu(Player player, RobotMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
    }

    public void registerMenu(Player player, RobotStorageMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
    }

    public void unregisterMenu(Player player) {
        openMenus.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Object menu = openMenus.get(player.getUniqueId());

        if (menu != null) {
            boolean isHandled = false;

            if (menu instanceof RobotMenu) {
                RobotMenu robotMenu = (RobotMenu) menu;
                if (event.getInventory().equals(robotMenu.getInventory())) {
                    event.setCancelled(true);
                    if (event.getCurrentItem() != null) {
                        robotMenu.handleClick(event.getSlot(), player);
                    }
                    isHandled = true;
                }
            } else if (menu instanceof RobotStorageMenu) {
                RobotStorageMenu storageMenu = (RobotStorageMenu) menu;
                if (event.getInventory().equals(storageMenu.getInventory())) {
                    event.setCancelled(true);
                    if (event.getCurrentItem() != null) {
                        storageMenu.handleClick(event.getSlot(), player);
                    }
                    isHandled = true;
                }
            }

            // Cancel all clicks in robot menus
            if (isHandled) {
                event.setCancelled(true);
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