package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class TipsConfigManager {
    private final Main plugin;
    private final File configFile;
    private FileConfiguration config;
    private FileConfiguration stateConfig;
    private File stateFile;
    private List<String> tips;
    private final Map<String, String> tipById = new LinkedHashMap<>();
    private final List<String> rotationOrder = new ArrayList<>();
    private int nextIndex;
    private int delaySeconds;

    public TipsConfigManager(Main plugin) {
        this.plugin = plugin;
        this.tips = new ArrayList<>();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        ensureStateFile();
    }

    /**
     * Load tips and delay from config.yml.
     */
    public void load() {
        config = YamlConfiguration.loadConfiguration(configFile);
        tips = config.getStringList("tips.messages");
        delaySeconds = config.getInt("tips.delay", 120);
        rebuildEntries();
        saveState();
        plugin.getLogger().info("[Tips] Loaded " + tips.size() + " tips, interval: " + delaySeconds + " seconds.");
    }

    public List<String> getTips() {
        return tips;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public synchronized boolean hasTips() {
        return !rotationOrder.isEmpty();
    }

    public synchronized String nextTip() {
        if (rotationOrder.isEmpty()) {
            return null;
        }
        if (nextIndex >= rotationOrder.size()) {
            reshuffle();
        }
        String id = rotationOrder.get(nextIndex++);
        if (nextIndex >= rotationOrder.size()) {
            reshuffle();
        }
        saveState();
        return tipById.getOrDefault(id, "");
    }

    public synchronized void resetRotation() {
        reshuffle();
        saveState();
    }

    private void rebuildEntries() {
        ensureStateFile();
        tipById.clear();
        for (int i = 0; i < tips.size(); i++) {
            String text = tips.get(i);
            if (text == null) continue;
            String id = generateId(text, i);
            tipById.put(id, text);
        }

        stateConfig = YamlConfiguration.loadConfiguration(stateFile);
        List<String> savedOrder = stateConfig.getStringList("rotation");
        rotationOrder.clear();
        Set<String> seen = new HashSet<>();
        for (String id : savedOrder) {
            if (tipById.containsKey(id) && seen.add(id)) {
                rotationOrder.add(id);
            }
        }

        // Append any new tips that were not previously seen.
        List<String> remaining = new ArrayList<>();
        for (String id : tipById.keySet()) {
            if (seen.add(id)) {
                remaining.add(id);
            }
        }
        if (!remaining.isEmpty()) {
            Collections.shuffle(remaining, ThreadLocalRandom.current());
            rotationOrder.addAll(remaining);
        }

        if (rotationOrder.isEmpty()) {
            nextIndex = 0;
        } else {
            int storedIndex = stateConfig.getInt("next-index", 0);
            nextIndex = Math.max(0, Math.min(storedIndex, rotationOrder.size() - 1));
        }
    }

    private void reshuffle() {
        if (rotationOrder.isEmpty()) {
            nextIndex = 0;
            return;
        }
        Collections.shuffle(rotationOrder, ThreadLocalRandom.current());
        nextIndex = 0;
    }

    private void ensureStateFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (stateFile == null) {
            stateFile = new File(plugin.getDataFolder(), "tips_state.yml");
        }
        if (!stateFile.exists()) {
            try {
                stateFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("[Tips] Failed to create tips_state.yml: " + e.getMessage());
            }
        }
    }

    private void saveState() {
        if (stateFile == null) return;
        if (stateConfig == null) {
            stateConfig = new YamlConfiguration();
        }
        stateConfig.set("rotation", rotationOrder);
        stateConfig.set("next-index", nextIndex);
        try {
            stateConfig.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Tips] Failed to save tips_state.yml: " + e.getMessage());
        }
    }

    private String generateId(String text, int index) {
        return UUID.nameUUIDFromBytes((text + "#" + index).getBytes(StandardCharsets.UTF_8)).toString();
    }
}