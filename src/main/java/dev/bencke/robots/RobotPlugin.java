package dev.bencke.robots;

import dev.bencke.robots.commands.CommandManager;
import dev.bencke.robots.config.ConfigManager;
import dev.bencke.robots.database.DatabaseManager;
import dev.bencke.robots.listeners.ListenerManager;
import dev.bencke.robots.managers.*;
import dev.bencke.robots.utils.Logger;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class RobotPlugin extends JavaPlugin {

    @Getter
    private static RobotPlugin instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RobotManager robotManager;
    private RewardManager rewardManager;
    private FuelManager fuelManager;
    private MenuManager menuManager;
    private PacketManager packetManager;
    private CommandManager commandManager;
    private ListenerManager listenerManager;
    private PlayerManager playerManager;

    private Economy economy;
    private ExecutorService executorService;

    @Override
    public void onEnable() {
        instance = this;

        Logger.info("Initializing RobotPlugin v" + getDescription().getVersion());

        // Initialize thread pool
        executorService = Executors.newFixedThreadPool(4);

        // Load configurations
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // Initialize managers
        packetManager = new PacketManager(this);
        rewardManager = new RewardManager(this);
        fuelManager = new FuelManager(this);
        robotManager = new RobotManager(this);
        menuManager = new MenuManager(this);
        playerManager = new PlayerManager(this);

        // Register commands and listeners
        commandManager = new CommandManager(this);
        commandManager.registerCommands();

        listenerManager = new ListenerManager(this);
        listenerManager.registerListeners();

        // Start robot tick task
        robotManager.startTickTask();

        Logger.info("RobotPlugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        Logger.info("Disabling RobotPlugin...");

        // Save all robots
        if (robotManager != null) {
            robotManager.saveAll();
            robotManager.removeAll();
        }

        // Close database connections
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        // Shutdown executor
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        Logger.info("RobotPlugin disabled!");
    }

    public void runAsync(Runnable task) {
        executorService.execute(task);
    }
}