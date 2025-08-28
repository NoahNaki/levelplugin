package me.nakilex.levelplugin.mob.config;

import me.nakilex.levelplugin.items.data.ArmorType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModelSetManager {

    private final Map<String, Map<Material, String>> materialSets = new HashMap<>();
    private final Map<String, Map<ArmorType, String>> armorSets = new HashMap<>();
    private final Map<String, Map<me.nakilex.levelplugin.items.data.WeaponType, String>> weaponSets = new HashMap<>();

    public ModelSetManager(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "model_sets.yml");
        if (!file.exists()) {
            plugin.saveResource("model_sets.yml", true);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("sets");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(key);
                if (sec == null) continue;

                Map<Material, String> matMap = new HashMap<>();
                ConfigurationSection matSec = sec.getConfigurationSection("materials");
                if (matSec != null) {
                    for (String matKey : matSec.getKeys(false)) {
                        try {
                            Material mat = Material.valueOf(matKey);
                            matMap.put(mat, matSec.getString(matKey));
                        } catch (IllegalArgumentException ignore) {
                        }
                    }
                }
                if (!matMap.isEmpty()) {
                    materialSets.put(key.toLowerCase(), matMap);
                }

                Map<ArmorType, String> armorMap = new HashMap<>();
                ConfigurationSection armorSec = sec.getConfigurationSection("armor");
                if (armorSec != null) {
                    for (String typeKey : armorSec.getKeys(false)) {
                        try {
                            ArmorType at = ArmorType.valueOf(typeKey.toUpperCase());
                            armorMap.put(at, armorSec.getString(typeKey));
                        } catch (IllegalArgumentException ignore) {
                        }
                    }
                }
                if (!armorMap.isEmpty()) {
                    armorSets.put(key.toLowerCase(), armorMap);
                }

                Map<me.nakilex.levelplugin.items.data.WeaponType, String> weaponMap = new HashMap<>();
                ConfigurationSection weapSec = sec.getConfigurationSection("weapon_types");
                if (weapSec != null) {
                    for (String typeKey : weapSec.getKeys(false)) {
                        try {
                            me.nakilex.levelplugin.items.data.WeaponType wt = me.nakilex.levelplugin.items.data.WeaponType.valueOf(typeKey.toUpperCase());
                            weaponMap.put(wt, weapSec.getString(typeKey));
                        } catch (IllegalArgumentException ignore) {
                        }
                    }
                }
                if (!weaponMap.isEmpty()) {
                    weaponSets.put(key.toLowerCase(), weaponMap);
                }
            }
        }
    }

    public String getModelId(String setName, Material mat) {
        String key = setName.toLowerCase();
        Map<Material, String> matMap = materialSets.get(key);
        if (matMap != null) {
            String value = matMap.get(mat);
            if (value != null) {
                return value;
            }
        }

        Map<ArmorType, String> armorMap = armorSets.get(key);
        if (armorMap != null) {
            ArmorType type = ArmorType.matchType(new org.bukkit.inventory.ItemStack(mat));
            if (type != null) {
                return armorMap.get(type);
            }
        }

        Map<me.nakilex.levelplugin.items.data.WeaponType, String> weaponMap = weaponSets.get(key);
        if (weaponMap != null) {
            me.nakilex.levelplugin.items.data.WeaponType wt = me.nakilex.levelplugin.items.data.WeaponType.matchType(new org.bukkit.inventory.ItemStack(mat));
            if (wt != null) {
                return weaponMap.get(wt);
            }
        }
        return null;
    }

    /**
     * @return all model ids referenced by armor mappings across every set.
     */
    public java.util.Set<String> getAllArmorModelIds() {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (Map<ArmorType, String> map : armorSets.values()) {
            out.addAll(map.values());
        }
        return out;
    }

    /**
     * @return all model ids referenced by weapon mappings across every set.
     */
    public java.util.Set<String> getAllWeaponModelIds() {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (Map<me.nakilex.levelplugin.items.data.WeaponType, String> map : weaponSets.values()) {
            out.addAll(map.values());
        }
        return out;
    }
}
