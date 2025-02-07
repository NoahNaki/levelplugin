package me.nakilex.levelplugin.potions.managers;

import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.utils.PotionCooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class PotionManager {

    private final Map<String, PotionTemplate> templates = new HashMap<>();
    private final Map<UUID, PotionInstance> instances = new HashMap<>();
    private final PotionCooldownManager cooldownManager = new PotionCooldownManager();

    public PotionManager(FileConfiguration config) {
        loadPotions(config);
    }

    private void loadPotions(FileConfiguration config) {
        if (!config.contains("potions")) {
            Bukkit.getLogger().severe("No potions found in potions.yml!");
            return;
        }

        for (String key : config.getConfigurationSection("potions").getKeys(false)) {
            String id = config.getString("potions." + key + ".id");
            String name = config.getString("potions." + key + ".name");
            Material material = Material.valueOf(config.getString("potions." + key + ".material"));
            int charges = config.getInt("potions." + key + ".charges");
            int cooldownSeconds = config.getInt("potions." + key + ".cooldownSeconds");
            templates.put(id, new PotionTemplate(id, name, material, charges, cooldownSeconds));
        }
    }

    public PotionTemplate getTemplate(String id) {
        return templates.get(id);
    }

    public Collection<PotionTemplate> getAllTemplates() {
        return templates.values();
    }

    public PotionInstance getPotionInstance(UUID uuid) {
        return instances.get(uuid);
    }


    public PotionInstance createInstance(PotionTemplate template) {
        PotionInstance instance = new PotionInstance(template);
        instances.put(instance.getUuid(), instance);
        return instance;
    }

    public boolean isOnCooldown(UUID uuid) {
        return cooldownManager.isOnCooldown(uuid);
    }

    public void startCooldown(UUID uuid, int seconds) {
        cooldownManager.startCooldown(uuid, seconds);
    }
}
