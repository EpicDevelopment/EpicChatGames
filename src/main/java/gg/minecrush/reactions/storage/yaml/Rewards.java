package gg.minecrush.reactions.storage.yaml;

import gg.minecrush.reactions.util.color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Rewards {
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private String filePath = "rewards.yml";

    public Rewards(Plugin plugin) {
        this.plugin = plugin;
        createConfig();
    }

    public String getFilePath() {
        return filePath;
    }

    private void createConfig() {
        configFile = new File(plugin.getDataFolder(), filePath);
        if (!configFile.exists()) {
            plugin.saveResource(filePath, false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public String getValue(String key) {
        String message = config.getString(key);
        if (message == null) {
            return "";
        }
        return message;
    }

    public boolean getBoolean(String key) {
        Boolean message = config.getBoolean(key);
        if (message == null) {
            return false;
        }
        return message;
    }

    public List<String> getRewardList() {
        ConfigurationSection section = config.getConfigurationSection("rewards");
        List<String> rewardKeys = new ArrayList<>();

        if (section != null) {
            Set<String> keys = section.getKeys(false);
            rewardKeys.addAll(keys);
        }

        return rewardKeys;
    }

    public String getRandomReward() {
        List<String> rewardKeys = getRewardList();
        if (rewardKeys.isEmpty()) {
            return null;
        }
        Random random = new Random();
        int randomIndex = random.nextInt(rewardKeys.size());
        return rewardKeys.get(randomIndex);
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), filePath);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConfigurationSection getConfigurationSection(String s) {
        return config.getConfigurationSection(s);
    }
}
