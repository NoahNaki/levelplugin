package me.nakilex.levelplugin.player.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Manages persistence of per-player data, including stats and level.
 */
public class PlayerConfig {

    private final Main plugin;
    private final File file;
    private final FileConfiguration config;

    public PlayerConfig(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player_data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create player_data.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }
    public FileConfiguration getConfig() {
        return config;
    }


    /** Saves stats and level for one player. */
    public void savePlayerData(UUID uuid) {
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(uuid);
        LevelManager levelManager = LevelManager.getInstance();
        me.nakilex.levelplugin.player.mining.managers.MiningManager miningManager = me.nakilex.levelplugin.player.mining.managers.MiningManager.getInstance();

        String path = "players." + uuid.toString();
        // Use UUID-based lookups so offline players save correctly
        config.set(path + ".level", levelManager.getLevel(uuid));
        config.set(path + ".xp",    levelManager.getXP(uuid));
        config.set(path + ".mining.level", miningManager.getLevel(uuid));
        config.set(path + ".mining.xp",    miningManager.getXP(uuid));
        config.set(path + ".skill_points", stats.skillPoints);
        config.set(path + ".stats.base_strength", stats.baseStrength);
        config.set(path + ".stats.base_agility", stats.baseAgility);
        config.set(path + ".stats.base_intelligence", stats.baseIntelligence);
        config.set(path + ".stats.base_dexterity", stats.baseDexterity);
        config.set(path + ".stats.base_health", stats.baseHealthStat);
        config.set(path + ".stats.base_defense", stats.baseDefenceStat);
        config.set(path + ".class", stats.playerClass.name());
        List<String> unlocked = new ArrayList<>();
        for (PlayerClass pc : stats.unlockedClasses) unlocked.add(pc.name());
        config.set(path + ".unlocked_classes", unlocked);

        saveConfig();
    }

    /** Loads stats and level for one player. */
    public void loadPlayerData(UUID uuid) {
        String root = "players." + uuid.toString();
        if (!config.contains(root)) return;

        PlayerClass playerClass = PlayerClass.valueOf(
            config.getString(root + ".class", PlayerClass.VILLAGER.name())
        );
        int level = config.getInt(root + ".level", 1);
        int xp = config.getInt(root + ".xp", 0);
        int miningLevel = config.getInt(root + ".mining.level", 1);
        int miningXp = config.getInt(root + ".mining.xp", 0);
        int skillPoints = config.getInt(root + ".skill_points", 0);
        List<String> unlockedList = config.getStringList(root + ".unlocked_classes");

        LevelManager lm = LevelManager.getInstance();
        lm.setLevel(uuid, level);
        lm.addXP(uuid, xp);
        me.nakilex.levelplugin.player.mining.managers.MiningManager mm = me.nakilex.levelplugin.player.mining.managers.MiningManager.getInstance();
        mm.setLevel(uuid, miningLevel);
        mm.addXP(uuid, miningXp);

        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(uuid);
        stats.playerClass = playerClass;
        stats.skillPoints = skillPoints;
        stats.baseStrength      = config.getInt(root + ".stats.base_strength", 0);
        stats.baseAgility       = config.getInt(root + ".stats.base_agility", 0);
        stats.baseIntelligence  = config.getInt(root + ".stats.base_intelligence", 0);
        stats.baseDexterity     = config.getInt(root + ".stats.base_dexterity", 0);
        stats.baseHealthStat    = config.getInt(root + ".stats.base_health", 0);
        stats.baseDefenceStat   = config.getInt(root + ".stats.base_defense", 0);
        stats.unlockedClasses.clear();
        stats.unlockedClasses.add(playerClass);
        for (String s : unlockedList) {
            try {
                stats.unlockedClasses.add(PlayerClass.valueOf(s));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /** Saves data for all players. */
    public void saveAllPlayers() {
        for (UUID uuid : StatsManager.getInstance().getAllPlayerUUIDs()) {
            savePlayerData(uuid);
        }
    }

    /** Loads data for all players. */
    public void loadAllPlayers() {
        if (!config.contains("players")) return;
        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            loadPlayerData(uuid);
        }
    }



    // ----- Environment Progress -----

    public int getEnvironmentLevel(UUID uuid) {
        String path = "players." + uuid + ".environment.level";
        return config.getInt(path, 1);
    }

    public int getEnvironmentStage(UUID uuid) {
        String path = "players." + uuid + ".environment.stage";
        return config.getInt(path, 1);
    }

    public void setEnvironmentState(UUID uuid, int level, int stage) {
        String base = "players." + uuid + ".environment.";
        config.set(base + "level", level);
        config.set(base + "stage", stage);
    }

    /** Get the stored origin location of a player's settlement or null if not set. */
    public org.bukkit.Location getEnvironmentOrigin(UUID uuid) {
        String base = "players." + uuid + ".environment.origin.";
        if (!config.contains(base + "world")) return null;
        org.bukkit.World world = Bukkit.getWorld(config.getString(base + "world"));
        if (world == null) return null;
        int x = config.getInt(base + "x", 0);
        int y = config.getInt(base + "y", 0);
        int z = config.getInt(base + "z", 0);
        return new org.bukkit.Location(world, x, y, z);
    }

    /** Store the origin location of a player's settlement. */
    public void setEnvironmentOrigin(UUID uuid, org.bukkit.Location loc) {
        String base = "players." + uuid + ".environment.origin.";
        if (loc == null) {
            config.set(base + "world", null);
            config.set(base + "x", null);
            config.set(base + "y", null);
            config.set(base + "z", null);
        } else {
            config.set(base + "world", loc.getWorld().getName());
            config.set(base + "x", loc.getBlockX());
            config.set(base + "y", loc.getBlockY());
            config.set(base + "z", loc.getBlockZ());
        }
    }

    public String getEnvironmentTown(UUID uuid) {
        return config.getString("players." + uuid + ".environment.town", null);
    }

    public void setEnvironmentTown(UUID uuid, String town) {
        config.set("players." + uuid + ".environment.town", town);
    }

    public int getBuildingStage(UUID uuid, String building) {
        String path = "players." + uuid + ".environment.buildings." + building + ".stage";
        return config.getInt(path, 1);
    }

    public void setBuildingStage(UUID uuid, String building, int stage) {
        String path = "players." + uuid + ".environment.buildings." + building + ".stage";
        config.set(path, stage);
    }

    public java.util.Set<String> getStoredBuildings(UUID uuid) {
        String base = "players." + uuid + ".environment.buildings";
        if (!config.isConfigurationSection(base)) return java.util.Collections.emptySet();
        return config.getConfigurationSection(base).getKeys(false);
    }

    public java.util.UUID getCoopOwner(UUID uuid) {
        String path = "players." + uuid + ".environment.coop.owner";
        String val = config.getString(path, null);
        return val != null ? java.util.UUID.fromString(val) : null;
    }

    public void setCoopOwner(UUID uuid, java.util.UUID owner) {
        String path = "players." + uuid + ".environment.coop.owner";
        config.set(path, owner != null ? owner.toString() : null);
    }

    public java.util.UUID getCoopPartner(UUID uuid) {
        String path = "players." + uuid + ".environment.coop.partner";
        String val = config.getString(path, null);
        return val != null ? java.util.UUID.fromString(val) : null;
    }

    public void setCoopPartner(UUID uuid, java.util.UUID partner) {
        String path = "players." + uuid + ".environment.coop.partner";
        config.set(path, partner != null ? partner.toString() : null);
    }

    public void clearCoop(UUID uuid) {
        setCoopOwner(uuid, null);
        setCoopPartner(uuid, null);
    }

    public void clearEnvironmentData(UUID uuid) {
        String base = "players." + uuid + ".environment.";
        config.set(base + "level", null);
        config.set(base + "stage", null);
        config.set(base + "town", null);
        config.set(base + "origin.world", null);
        config.set(base + "origin.x", null);
        config.set(base + "origin.y", null);
        config.set(base + "buildings", null);
    }

    // ----- Profile Data -----

    public org.bukkit.Location getProfileLocation(UUID uuid, int slot) {
        String base = "players." + uuid + ".profiles." + slot + ".";
        if (!config.contains(base + "world")) return null;
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(config.getString(base + "world"));
        if (w == null) return null;
        double x = config.getDouble(base + "x");
        double y = config.getDouble(base + "y");
        double z = config.getDouble(base + "z");
        float yaw = (float) config.getDouble(base + "yaw");
        float pitch = (float) config.getDouble(base + "pitch");
        return new org.bukkit.Location(w, x, y, z, yaw, pitch);
    }

    public void setProfileLocation(UUID uuid, int slot, org.bukkit.Location loc) {
        String base = "players." + uuid + ".profiles." + slot + ".";
        if (loc == null) {
            config.set(base + "world", null);
            config.set(base + "x", null);
            config.set(base + "y", null);
            config.set(base + "z", null);
            config.set(base + "yaw", null);
            config.set(base + "pitch", null);
        } else {
            config.set(base + "world", loc.getWorld().getName());
            config.set(base + "x", loc.getX());
            config.set(base + "y", loc.getY());
            config.set(base + "z", loc.getZ());
            config.set(base + "yaw", loc.getYaw());
            config.set(base + "pitch", loc.getPitch());
        }
    }

    public org.bukkit.inventory.ItemStack[] getProfileInventory(java.util.UUID uuid, int slot) {
        String path = "players." + uuid + ".profiles." + slot + ".inventory";
        String data = config.getString(path, "");
        return me.nakilex.levelplugin.utils.InventorySerialUtil.itemStackArrayFromBase64(data);
    }

    public void setProfileInventory(java.util.UUID uuid, int slot, org.bukkit.inventory.ItemStack[] items) {
        String path = "players." + uuid + ".profiles." + slot + ".inventory";
        String data = me.nakilex.levelplugin.utils.InventorySerialUtil.itemStackArrayToBase64(items);
        config.set(path, data);
    }

    public org.bukkit.inventory.ItemStack[] getProfileArmor(java.util.UUID uuid, int slot) {
        String path = "players." + uuid + ".profiles." + slot + ".armor";
        String data = config.getString(path, "");
        return me.nakilex.levelplugin.utils.InventorySerialUtil.itemStackArrayFromBase64(data);
    }

    public void setProfileArmor(java.util.UUID uuid, int slot, org.bukkit.inventory.ItemStack[] items) {
        String path = "players." + uuid + ".profiles." + slot + ".armor";
        String data = me.nakilex.levelplugin.utils.InventorySerialUtil.itemStackArrayToBase64(items);
        config.set(path, data);
    }

    public int getUnlockedProfiles(UUID uuid) {
        return config.getInt("players." + uuid + ".profiles.unlocked", 1);
    }

    public void setUnlockedProfiles(UUID uuid, int count) {
        config.set("players." + uuid + ".profiles.unlocked", count);
    }

    /** Allows external classes to persist config changes. */
    public void saveConfigFile() {
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player_data.yml: " + e.getMessage());
        }
    }

    public void savePlayer(UUID playerUuid) {
        // Persist stats
        savePlayerData(playerUuid);
        saveConfig();
    }
}
