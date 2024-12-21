package me.nakilex.levelplugin.mob;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class MobManager {

    private Main plugin;
    private File customMobsFile;
    private FileConfiguration mobConfig;
    private Map<String, MobConfig> mobConfigs = new HashMap<>();

    public MobManager(Main plugin) {
        this.plugin = plugin;
        createMobConfig(); // Load or save the mob config during initialization
        loadCustomMobsYML();
        loadMobConfigs();
    }

    private void loadCustomMobsYML() {
        customMobsFile = new File(plugin.getDataFolder(), "custommobs.yml");
        if (!customMobsFile.exists()) {
            plugin.getLogger().info("[Debug] custommobs.yml not found, copying from resources...");
            plugin.saveResource("custommobs.yml", true);
        }
        mobConfig = YamlConfiguration.loadConfiguration(customMobsFile);
        plugin.getLogger().info("[Debug] custommobs.yml loaded or created successfully.");
    }

    private void loadMobConfigs() {
        ConfigurationSection mobsSec = mobConfig.getConfigurationSection("mobs");
        if (mobsSec == null) {
            plugin.getLogger().warning("[Debug] No 'mobs' section found in custommobs.yml!");
            return;
        }

        for (String key : mobsSec.getKeys(false)) {
            ConfigurationSection mobSec = mobsSec.getConfigurationSection(key);
            if (mobSec == null) continue;

            String id = mobSec.getString("id", key);
            String entityTypeStr = mobSec.getString("entitytype", "ZOMBIE");
            String name = mobSec.getString("name", "&fMob");
            int minLevel = mobSec.getInt("minlevel", 1);
            int maxLevel = mobSec.getInt("maxlevel", 1);

            double baseHealth = 20.0;
            double healthMultiplier = 1.0;
            double baseDamage = 2.0;
            double damageMultiplier = 1.0;
            double movementSpeed = 0.2;

            int xpDrop = 0;

            ConfigurationSection statsSec = mobSec.getConfigurationSection("stats");
            if (statsSec != null) {
                ConfigurationSection healthSec = statsSec.getConfigurationSection("health");
                if (healthSec != null) {
                    baseHealth = healthSec.getDouble("base", 20.0);
                    healthMultiplier = healthSec.getDouble("multiplier", 1.0);
                }
                ConfigurationSection damageSec = statsSec.getConfigurationSection("damage");
                if (damageSec != null) {
                    baseDamage = damageSec.getDouble("base", 2.0);
                    damageMultiplier = damageSec.getDouble("multiplier", 1.0);
                }
                movementSpeed = statsSec.getDouble("movementspeed", 0.2);
                xpDrop = statsSec.getInt("xp", 0);
            }

            List<MobConfig.LootEntry> lootTable = new ArrayList<>();
            if (mobSec.isList("loot")) {
                List<Map<?, ?>> lootList = mobSec.getMapList("loot");
                for (Map<?, ?> map : lootList) {
                    String item = map.containsKey("item") ? (String) map.get("item") : "COINS";
                    int dropRate = map.containsKey("drop_rate") ? ((Number) map.get("drop_rate")).intValue() : 0;
                    int dropMin = map.containsKey("drop_min") ? ((Number) map.get("drop_min")).intValue() : 0;
                    int dropMax = map.containsKey("drop_max") ? ((Number) map.get("drop_max")).intValue() : 0;
                    MobConfig.LootEntry le = new MobConfig.LootEntry(item, dropRate, dropMin, dropMax);
                    lootTable.add(le);
                }
            }

            MobConfig mobCfg = new MobConfig(
                id, entityTypeStr, name, minLevel, maxLevel, baseHealth,
                healthMultiplier, baseDamage, damageMultiplier, movementSpeed, xpDrop, lootTable
            );
            mobConfigs.put(id.toLowerCase(), mobCfg);
            plugin.getLogger().info("[Debug] Loaded mob config for id=" + id + " entityType=" + entityTypeStr);
        }
        plugin.getLogger().info("[Debug] Finished loading " + mobConfigs.size() + " custom mobs from custommobs.yml");
    }

    public MobConfig getMobConfig(String mobId) {
        return mobConfigs.get(mobId.toLowerCase());
    }

    public void spawnMob(String mobId, Location loc, int amount) {
        MobConfig cfg = getMobConfig(mobId);
        if (cfg == null) {
            plugin.getLogger().warning("No mob config found for ID: " + mobId);
            return;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(cfg.getEntityType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type: " + cfg.getEntityType() + ", defaulting to ZOMBIE");
            type = EntityType.ZOMBIE;
        }

        Random random = new Random();
        for (int i = 0; i < amount; i++) {
            LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

            int levelRange = cfg.getMaxLevel() - cfg.getMinLevel() + 1;
            int level = cfg.getMinLevel() + random.nextInt(levelRange);

            double scaledHealth = cfg.getBaseHealth() + (level * cfg.getHealthMultiplier());
            double scaledDamage = cfg.getBaseDamage() + (level * cfg.getDamageMultiplier());

            // Format the custom mob's name
            String levelPrefix = ChatColor.GRAY + "[Lv " + level + "]  ";
            String mobName = ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', cfg.getName()) + "  ";
            String healthText = ChatColor.RED + "" + (int) scaledHealth + "/" + (int) scaledHealth + " ♥";

            entity.setCustomName(levelPrefix + mobName + healthText);
            entity.setCustomNameVisible(true);

            entity.setMaxHealth(scaledHealth);
            entity.setHealth(scaledHealth);

            // Store metadata
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            pdc.set(CustomMob.MOB_ID_KEY, PersistentDataType.STRING, cfg.getId());
            pdc.set(CustomMob.LEVEL_KEY, PersistentDataType.INTEGER, level);
            pdc.set(CustomMob.XP_KEY, PersistentDataType.INTEGER, cfg.getXpDrop());
        }
    }

    private void createMobConfig() {
        File mobFile = new File(plugin.getDataFolder(), "custommobs.yml");
        plugin.getLogger().info("[Debug] Path for custommobs.yml: " + mobFile.getAbsolutePath());

        if (!mobFile.exists()) {
            plugin.getLogger().info("[Debug] custommobs.yml not found. Saving default...");
            plugin.saveResource("custommobs.yml", false); // Save from resources
        }

        mobConfig = YamlConfiguration.loadConfiguration(mobFile);
        plugin.getLogger().info("[Debug] Loaded custommobs.yml successfully.");
    }

    public FileConfiguration getMobConfig() {
        return mobConfig;
    }
}