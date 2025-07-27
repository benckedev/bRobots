package dev.bencke.robots.models;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.config.RobotType;
import dev.bencke.robots.rewards.Reward;
import dev.bencke.robots.utils.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Data
public class Robot {

    private final UUID id;
    private final UUID ownerId;
    private final RobotType type;
    private final Location location;
    private final Map<ItemStack, Integer> storage = new ConcurrentHashMap<>();
    private final Set<String> upgrades = ConcurrentHashMap.newKeySet();
    @Setter
    private String ownerName;
    @Getter
    private ArmorStand entity;
    private int level;
    private long fuel;
    private long lastGeneration;
    private boolean active = true;
    private boolean generating = false;

    public Robot(UUID ownerId, String ownerName, RobotType type, Location location) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.type = type;
        this.location = location;
        this.level = 1;
        this.fuel = 0;
        this.lastGeneration = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    public static Robot deserialize(Map<String, Object> data) {
        try {
            UUID id = UUID.fromString((String) data.get("id"));
            UUID ownerId = UUID.fromString((String) data.get("owner"));
            String ownerName = (String) data.get("ownerName");
            String typeId = (String) data.get("type");
            Location location = LocationSerializer.deserialize((String) data.get("location"));

            RobotType type = RobotPlugin.getInstance().getConfigManager()
                    .getRobotTypes().get(typeId);

            if (type == null) {
                return null;
            }

            Robot robot = new Robot(ownerId, ownerName, type, location);
            robot.level = (int) data.get("level");
            robot.fuel = ((Number) data.get("fuel")).longValue();

            // Load upgrades
            List<String> upgradeList = (List<String>) data.get("upgrades");
            if (upgradeList != null) {
                robot.upgrades.addAll(upgradeList);
            }

            // Load storage
            Map<String, Integer> storageData = (Map<String, Integer>) data.get("storage");
            if (storageData != null) {
                for (Map.Entry<String, Integer> entry : storageData.entrySet()) {
                    ItemStack item = ItemSerializer.deserialize(entry.getKey());
                    if (item != null) {
                        robot.storage.put(item, entry.getValue());
                    }
                }
            }
            return robot;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void spawn() {
        if (entity != null && !entity.isDead()) {
            return;
        }


        entity = location.getWorld().spawn(location, ArmorStand.class);
        entity.setSmall(true);
        entity.setArms(true);
        entity.setBasePlate(false);
        entity.setGravity(true);
        entity.setCanPickupItems(false);
        entity.setCustomName(ColorUtil.colorize(
                type.getDisplayName()
                        .replace("%owner%", ownerName)
                        .replace("%level%", String.valueOf(level))
                        .replace("%fuel%", String.valueOf(fuel))));
        entity.setCustomNameVisible(true);
        entity.setVisible(true);
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);
        entity.setMetadata("robot", new FixedMetadataValue(RobotPlugin.getInstance(), id.toString()));

        if (type.getHeadTexture() != null) {
            entity.setHelmet(createSkullItem(type.getHeadTexture()));
        }

        if (type.getChestplate() != null) {
            entity.setChestplate(type.getChestplate());
        }

        if (type.getLeggings() != null) {
            entity.setLeggings(type.getLeggings());
        }

        if (type.getBoots() != null) {
            entity.setBoots(type.getBoots());
        }

        if (type.getItemInHand() != null) {
            entity.setItemInHand(type.getItemInHand());
        }

        // Spawn particles
        spawnParticles(EnumParticle.VILLAGER_HAPPY, 10);
    }

    public void remove() {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        active = false;
    }

    public void tick() {
        if (!active || entity == null || entity.isDead()) {
            return;
        }

        // Update display name with fuel status
        updateDisplayName();

        // Check generation conditions
        if (canGenerate()) {
            generate();
        }
    }

    private boolean canGenerate() {
        if (fuel <= 0) return false;
        if (isStorageFull()) return false;

        long delay = type.getLevels().get(level).getGenerationDelay() * 1000L;
        return System.currentTimeMillis() - lastGeneration >= delay;
    }

    private void generate() {
        generating = true;
        lastGeneration = System.currentTimeMillis();

        // Consume fuel
        fuel--;

        // Get rewards for current level
        List<Reward> rewards = RobotPlugin.getInstance().getRewardManager()
                .getRewardsForLevel(type, level);

        if (rewards.isEmpty()) {
            System.out.println("DEBUG: No rewards found for robot type " + type.getId() + " level " + level);
            generating = false;
            return;
        }

        // Select random reward based on chances
        Reward reward = selectRandomReward(rewards);

        if (reward != null) {

            RobotPlugin.getInstance().runAsync(() -> {
                // Generate reward items
                List<ItemStack> items = reward.generate();

                // Add to storage
                Bukkit.getScheduler().runTask(RobotPlugin.getInstance(), () -> {
                    for (ItemStack item : items) {
                        if (item != null && item.getType() != Material.AIR) {
                            addToStorage(item);
                        }
                    }

                    // Effects
                    playGenerationEffects();
                    generating = false;
                });
            });
        } else {
            System.out.println("DEBUG: No reward selected");
            generating = false;
        }
    }

    private Reward selectRandomReward(List<Reward> rewards) {
        double totalChance = rewards.stream()
                .mapToDouble(Reward::getChance)
                .sum();

        double random = ThreadLocalRandom.current().nextDouble() * totalChance;
        double current = 0;

        for (Reward reward : rewards) {
            current += reward.getChance();
            if (random <= current) {
                return reward;
            }
        }

        return rewards.get(rewards.size() - 1);
    }

    private void addToStorage(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        // Find existing item that matches (same type, durability, and meta)
        ItemStack existingKey = null;
        for (ItemStack key : storage.keySet()) {
            if (isSimilarItem(key, item)) {
                existingKey = key;
                break;
            }
        }

        if (existingKey != null) {
            // Add to existing stack
            storage.put(existingKey, storage.get(existingKey) + item.getAmount());
        } else {
            // Create new entry
            ItemStack newKey = item.clone();
            newKey.setAmount(1); // Use amount 1 as key, store actual amount in map
            storage.put(newKey, item.getAmount());
        }

        System.out.println("DEBUG: Added " + item.getAmount() + "x " + item.getType() + " to storage");
        System.out.println("DEBUG: Storage now has " + storage.size() + " different item types");
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

    public boolean isStorageFull() {
        int maxStorage = type.getLevels().get(level).getStorageLimit();
        int currentItems = storage.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        return currentItems >= maxStorage;
    }

    public void addFuel(long amount) {
        long maxFuel = type.getFuelCapacity();
        fuel = Math.min(fuel + amount, maxFuel);

        // Visual feedback
        spawnParticles(EnumParticle.FLAME, 5);
        playSound(Sound.FIZZ, 0.5f, 1.5f);
    }

    public boolean levelUp(Player player) {
        if (!canLevelUp()) {
            return false;
        }

        int nextLevel = level + 1;
        double cost = type.getLevels().get(nextLevel).getCost();

        if (RobotPlugin.getInstance().getEconomy().getBalance(player) < cost) {
            return false;
        }

        RobotPlugin.getInstance().getEconomy().withdrawPlayer(player, cost);
        level = nextLevel;

        // Effects
        playLevelUpEffects();

        return true;
    }

    public boolean canLevelUp() {
        return type.getLevels().containsKey(level + 1);
    }

    private void updateDisplayName() {
        if (entity == null) return;

        String name = ColorUtil.colorize(type.getDisplayName()
                .replace("%owner%", ownerName)
                .replace("%level%", String.valueOf(level))
                .replace("%fuel%", String.valueOf(fuel)));

        entity.setCustomName(name);
    }

    private void playGenerationEffects() {
        spawnParticles(EnumParticle.SPELL_WITCH, 15);
    }

    private void playLevelUpEffects() {
        spawnParticles(EnumParticle.FIREWORKS_SPARK, 30);
        playSound(Sound.ENDERDRAGON_GROWL, 1f, 1.5f);
    }

    private void spawnParticles(EnumParticle particle, int count) {
        Location loc = entity.getLocation().add(0, 1, 0);
        PacketUtils.sendParticles(loc, particle, count, 0.5f, 0.5f, 0.5f, 0.1f);
    }

    private void spawnParticle(Location loc, EnumParticle particle) {
        PacketUtils.sendParticles(loc, particle, 1, 0, 0, 0, 0);
    }

    private void playSound(Sound sound, float volume, float pitch) {
        entity.getWorld().playSound(entity.getLocation(), sound, volume, pitch);
    }

    private ItemStack createSkullItem(String texture) {
        return new ItemBuilder(Material.SKULL_ITEM)
                .durability((short) 3)
                .skullTexture(texture)
                .build();
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id.toString());
        data.put("owner", ownerId.toString());
        data.put("ownerName", ownerName);
        data.put("type", type.getId());
        data.put("location", LocationSerializer.serialize(location));
        data.put("level", level);
        data.put("fuel", fuel);
        data.put("upgrades", new ArrayList<>(upgrades));

        // Serialize storage properly
        Map<String, Integer> storageData = new HashMap<>();
        for (Map.Entry<ItemStack, Integer> entry : storage.entrySet()) {
            String serialized = ItemSerializer.serialize(entry.getKey());
            if (serialized != null) {
                storageData.put(serialized, entry.getValue());
            }
        }
        data.put("storage", storageData);

        return data;
    }
}