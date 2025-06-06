package me.nakilex.levelplugin.potions.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.utils.PotionCooldownManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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

    /**
     * Retrieve the PotionInstance associated with an ItemStack.
     * Returns null if the stack is not one of our custom potions.
     */
    public PotionInstance getInstanceFromItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;

        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "potion_uuid");

        if (!pdc.has(key, PersistentDataType.STRING)) return null;

        try {
            UUID uuid = UUID.fromString(pdc.get(key, PersistentDataType.STRING));
            return instances.get(uuid);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
