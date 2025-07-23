package dev.bencke.robots.managers;

import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.config.RobotType;
import dev.bencke.robots.rewards.Reward;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.stream.Collectors;

public class RewardManager {

    private final RobotPlugin plugin;

    @Getter
    private final Map<String, Reward> rewards = new HashMap<>();

    public RewardManager(RobotPlugin plugin) {
        this.plugin = plugin;
        loadRewards();
    }

    private void loadRewards() {
        rewards.clear();
        ConfigurationSection section = plugin.getConfigManager().getRewardsConfig()
                .getConfigurationSection("rewards");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection rewardSection = section.getConfigurationSection(key);
                Reward reward = new Reward(key, rewardSection);
                rewards.put(key, reward);
            }
        }
    }

    public List<Reward> getRewardsForLevel(RobotType robotType, int level) {
        RobotType.LevelData levelData = robotType.getLevels().get(level);
        if (levelData == null) return new ArrayList<>();

        return levelData.getRewards().stream()
                .map(this::parseRewardEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Reward parseRewardEntry(String entry) {
        // Format: "chance,reward_id"
        String[] parts = entry.split(",", 2);
        if (parts.length != 2) return null;

        try {
            double chance = Double.parseDouble(parts[0]);
            String rewardId = parts[1];

            Reward reward = rewards.get(rewardId);
            if (reward != null) {
                // Create a copy with modified chance
                return new Reward(reward.getId(), chance, reward.getType(), reward.getActions());
            }
        } catch (NumberFormatException ignored) {}

        return null;
    }

    public void reload() {
        loadRewards();
    }
}