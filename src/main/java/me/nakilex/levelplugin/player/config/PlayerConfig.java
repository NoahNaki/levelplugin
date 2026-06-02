package me.nakilex.levelplugin.player.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.player.attributes.managers.LifeSkillRewardManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.fishing.data.FishingQuality;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.spells.input.SpellKeybindSlot;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
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
        me.nakilex.levelplugin.player.farming.managers.FarmingManager farmingManager = me.nakilex.levelplugin.player.farming.managers.FarmingManager.getInstance();
        me.nakilex.levelplugin.player.fishing.managers.FishingManager fishingManager = me.nakilex.levelplugin.player.fishing.managers.FishingManager.getInstance();
        me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager woodcuttingManager = me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager.getInstance();

        String path = "players." + uuid.toString();
        // Use UUID-based lookups so offline players save correctly
        config.set(path + ".level", levelManager.getLevel(uuid));
        config.set(path + ".xp",    levelManager.getXP(uuid));
        config.set(path + ".mining.level", miningManager.getLevel(uuid));
        config.set(path + ".mining.xp",    miningManager.getXP(uuid));
        config.set(path + ".farming.level", farmingManager.getLevel(uuid));
        config.set(path + ".farming.xp",    farmingManager.getXP(uuid));
        config.set(path + ".fishing.level", fishingManager.getLevel(uuid));
        config.set(path + ".fishing.xp",    fishingManager.getXP(uuid));
        config.set(path + ".woodcutting.level", woodcuttingManager.getLevel(uuid));
        config.set(path + ".woodcutting.xp",    woodcuttingManager.getXP(uuid));
        config.set(path + ".fishing.discovered", new ArrayList<>(fishingManager.getDiscoveredFish(uuid)));
        config.set(path + ".fishing.records", null);
        for (Map.Entry<String, FishingManager.FishRecord> entry : fishingManager.getFishRecords(uuid).entrySet()) {
            String recordPath = path + ".fishing.records." + entry.getKey();
            FishingManager.FishRecord record = entry.getValue();
            config.set(recordPath + ".caught", record.caughtCount());
            config.set(recordPath + ".largest", record.largestSize());
            config.set(recordPath + ".quality", record.bestQuality().name());
        }
        LifeSkillRewardManager rewardManager = LifeSkillRewardManager.getInstance();
        if (rewardManager != null) {
            for (ToolDiscipline discipline : ToolDiscipline.values()) {
                config.set(path + ".lifeskills." + discipline.name().toLowerCase() + ".claimed",
                        new ArrayList<>(rewardManager.getClaimed(uuid, discipline)));
            }
        }
        config.set(path + ".skill_points", stats.skillPoints);
        SpellProgressionManager progressionManager = SpellProgressionManager.getInstance();
        Integer activeSlot = me.nakilex.levelplugin.player.profile.ProfileManager.getInstance().getActiveSlot(uuid);
        if (activeSlot != null && activeSlot >= 0) {
            setProfileSpellPoints(uuid, activeSlot, progressionManager.getSpellPoints(uuid));
            setProfileSpellLevels(uuid, activeSlot, progressionManager.serializeSpellLevels(uuid, activeSlot));
        }
        config.set(path + ".stats.base_strength", stats.baseStrength);
        config.set(path + ".stats.base_agility", stats.baseAgility);
        config.set(path + ".stats.base_intelligence", stats.baseIntelligence);
        config.set(path + ".stats.base_dexterity", stats.baseDexterity);
        config.set(path + ".stats.base_vitality", stats.baseVitality);
        config.set(path + ".stats.base_will", stats.baseWill);
        config.set(path + ".stats.base_technique", stats.baseTechnique);
        config.set(path + ".class", stats.playerClass.name());
        List<String> unlocked = new ArrayList<>();
        for (PlayerClass pc : stats.unlockedClasses) unlocked.add(pc.name());
        config.set(path + ".unlocked_classes", unlocked);

        me.nakilex.levelplugin.fasttravel.FastTravelManager ftm = plugin.getFastTravelManager();
        if (ftm != null) {
            config.set(path + ".fasttravel", new ArrayList<>(ftm.getUnlocked(uuid)));
        }

        me.nakilex.levelplugin.transmog.TransmogManager tm = plugin.getTransmogManager();
        if (tm != null) {
            config.set(path + ".transmog.weapon", new ArrayList<>(tm.getUnlocked(uuid, true)));
            config.set(path + ".transmog.armor", new ArrayList<>(tm.getUnlocked(uuid, false)));
        }

        // Persist essence slots and which ones are equipped
        List<ItemStack> essenceList = new ArrayList<>();
        for (ItemStack stack : stats.essenceSlots) {
            essenceList.add(stack);
        }
        config.set(path + ".essences.slots", essenceList);

        List<Boolean> equippedList = new ArrayList<>();
        for (boolean equipped : stats.equippedEssences) {
            equippedList.add(equipped);
        }
        config.set(path + ".essences.equipped", equippedList);
        config.set(path + ".essences.slot2_unlocked", stats.secondEssenceSlotUnlocked);

        BattlePassManager battlePass = Main.getInstance().getBattlePassManager();
        if (battlePass != null) {
            battlePass.saveProgress(uuid, config, path + ".battlepass");
        }

        saveConfig();
    }

    /** Loads stats and level for one player. */
    public void loadPlayerData(UUID uuid) {
        String root = "players." + uuid.toString();
        if (!config.contains(root)) return;

        PlayerClass playerClass = PlayerClass.fromString(
            config.getString(root + ".class", PlayerClass.VILLAGER.name())
        );
        if (playerClass == null) playerClass = PlayerClass.VILLAGER;
        int level = config.getInt(root + ".level", 1);
        int xp = config.getInt(root + ".xp", 0);
        int miningLevel = config.getInt(root + ".mining.level", 1);
        int miningXp = config.getInt(root + ".mining.xp", 0);
        int farmingLevel = config.getInt(root + ".farming.level", 1);
        int farmingXp = config.getInt(root + ".farming.xp", 0);
        int fishingLevel = config.getInt(root + ".fishing.level", 1);
        int fishingXp = config.getInt(root + ".fishing.xp", 0);
        int woodcuttingLevel = config.getInt(root + ".woodcutting.level", 1);
        int woodcuttingXp = config.getInt(root + ".woodcutting.xp", 0);
        List<String> discoveredFish = config.getStringList(root + ".fishing.discovered");
        Map<String, FishingManager.FishRecord> fishRecords = new HashMap<>();
        ConfigurationSection fishingRecords = config.getConfigurationSection(root + ".fishing.records");
        if (fishingRecords != null) {
            for (String fishId : fishingRecords.getKeys(false)) {
                String recordPath = root + ".fishing.records." + fishId;
                FishingQuality quality;
                try {
                    quality = FishingQuality.valueOf(config.getString(recordPath + ".quality", "NORMAL"));
                } catch (IllegalArgumentException ignored) {
                    quality = FishingQuality.NORMAL;
                }
                fishRecords.put(fishId, new FishingManager.FishRecord(
                        config.getInt(recordPath + ".caught", 0),
                        config.getDouble(recordPath + ".largest", 0.0), quality));
            }
        }
        LifeSkillRewardManager rewardManager = LifeSkillRewardManager.getInstance();
        int skillPoints = config.getInt(root + ".skill_points", 0);
        // Spell progression is profile-scoped and loaded on profile selection.
        List<String> unlockedList = config.getStringList(root + ".unlocked_classes");

        LevelManager lm = LevelManager.getInstance();
        lm.setLevel(uuid, level);
        lm.addXP(uuid, xp);
        me.nakilex.levelplugin.player.mining.managers.MiningManager mm = me.nakilex.levelplugin.player.mining.managers.MiningManager.getInstance();
        mm.setLevel(uuid, miningLevel);
        mm.addXP(uuid, miningXp);
        me.nakilex.levelplugin.player.farming.managers.FarmingManager fm = me.nakilex.levelplugin.player.farming.managers.FarmingManager.getInstance();
        fm.setLevel(uuid, farmingLevel);
        fm.addXP(uuid, farmingXp);
        me.nakilex.levelplugin.player.fishing.managers.FishingManager fim = me.nakilex.levelplugin.player.fishing.managers.FishingManager.getInstance();
        fim.setLevel(uuid, fishingLevel);
        fim.addXP(uuid, fishingXp);
        fim.setDiscoveredFish(uuid, new HashSet<>(discoveredFish));
        fim.setFishRecords(uuid, fishRecords);
        me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager wcm = me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager.getInstance();
        wcm.setLevel(uuid, woodcuttingLevel);
        wcm.addXP(uuid, woodcuttingXp);
        if (rewardManager != null) {
            for (ToolDiscipline discipline : ToolDiscipline.values()) {
                List<Integer> claimed = config.getIntegerList(root + ".lifeskills." + discipline.name().toLowerCase() + ".claimed");
                rewardManager.setClaimed(uuid, discipline, new HashSet<>(claimed));
            }
        }

        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(uuid);
        stats.playerClass = playerClass;
        stats.skillPoints = skillPoints;
        stats.baseStrength      = config.getInt(root + ".stats.base_strength", 0);
        stats.baseAgility       = config.getInt(root + ".stats.base_agility", 0);
        stats.baseIntelligence  = config.getInt(root + ".stats.base_intelligence", 0);
        stats.baseDexterity     = config.getInt(root + ".stats.base_dexterity", 0);
        // Support migration from old keys
        int oldHealth = config.getInt(root + ".stats.base_health", 0);
        int oldDef = config.getInt(root + ".stats.base_defense", 0);
        stats.baseVitality = config.getInt(root + ".stats.base_vitality", oldHealth + oldDef);
        stats.baseWill     = config.getInt(root + ".stats.base_will", 0);
        stats.baseTechnique = config.getInt(root + ".stats.base_technique", 0);
        stats.unlockedClasses.clear();
        stats.unlockedClasses.add(playerClass);
        for (String s : unlockedList) {
            PlayerClass cls = PlayerClass.fromString(s);
            if (cls != null) stats.unlockedClasses.add(cls);
        }

        List<String> ft = config.getStringList(root + ".fasttravel");
        plugin.getFastTravelManager().setUnlocked(uuid, new HashSet<>(ft));

        java.util.List<String> wModels = config.getStringList(root + ".transmog.weapon");
        java.util.List<String> aModels = config.getStringList(root + ".transmog.armor");
        plugin.getTransmogManager().setUnlocked(uuid, new HashSet<>(wModels), new HashSet<>(aModels));

        // Restore essence slots and equipped state
        List<ItemStack> essences = (List<ItemStack>) config.getList(root + ".essences.slots");
        if (essences != null) {
            for (int i = 0; i < Math.min(stats.essenceSlots.length, essences.size()); i++) {
                stats.essenceSlots[i] = essences.get(i);
            }
        }

        List<Boolean> equipped = config.getBooleanList(root + ".essences.equipped");
        for (int i = 0; i < Math.min(stats.equippedEssences.length, equipped.size()); i++) {
            stats.equippedEssences[i] = equipped.get(i);
        }
        stats.secondEssenceSlotUnlocked = config.getBoolean(root + ".essences.slot2_unlocked", false);

        BattlePassManager battlePass = Main.getInstance().getBattlePassManager();
        if (battlePass != null) {
            battlePass.loadProgress(uuid, config, root + ".battlepass");
        }
    }


    public SpellInputMode getProfileSpellInputMode(UUID uuid, int slot) {
        String raw = config.getString("players." + uuid + ".profiles." + slot + ".spells.input_mode");
        if (raw == null || raw.isBlank()) {
            return SpellInputMode.MOUSE_COMBO;
        }
        try {
            return SpellInputMode.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return SpellInputMode.MOUSE_COMBO;
        }
    }

    public void setProfileSpellInputMode(UUID uuid, int slot, SpellInputMode mode) {
        String path = "players." + uuid + ".profiles." + slot + ".spells.input_mode";
        config.set(path, mode == null ? SpellInputMode.MOUSE_COMBO.name() : mode.name());
    }

    public EnumMap<SpellKeybindSlot, SpellInputType> getProfileSpellKeybinds(UUID uuid, int slot,
                                                                              PlayerClass playerClass,
                                                                              SpellInputMode mode) {
        EnumMap<SpellKeybindSlot, SpellInputType> bindings = new EnumMap<>(SpellKeybindSlot.class);
        if (playerClass == null || mode == null) {
            return bindings;
        }
        String path = "players." + uuid + ".profiles." + slot + ".spells.keybinds."
                + playerClass.name() + "." + mode.name();
        List<String> entries = config.getStringList(path);
        for (String entry : entries) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                SpellKeybindSlot bindSlot = SpellKeybindSlot.valueOf(parts[0]);
                SpellInputType inputType = SpellInputType.valueOf(parts[1]);
                bindings.put(bindSlot, inputType);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return bindings;
    }

    public void setProfileSpellKeybinds(UUID uuid, int slot,
                                        PlayerClass playerClass,
                                        SpellInputMode mode,
                                        java.util.Map<SpellKeybindSlot, SpellInputType> bindings) {
        if (playerClass == null || mode == null) {
            return;
        }
        String path = "players." + uuid + ".profiles." + slot + ".spells.keybinds."
                + playerClass.name() + "." + mode.name();
        List<String> entries = new ArrayList<>();
        if (bindings != null) {
            for (java.util.Map.Entry<SpellKeybindSlot, SpellInputType> entry : bindings.entrySet()) {
                SpellKeybindSlot bindSlot = entry.getKey();
                SpellInputType inputType = entry.getValue();
                if (bindSlot == null || inputType == null) {
                    continue;
                }
                entries.add(bindSlot.name() + ":" + inputType.name());
            }
        }
        config.set(path, entries);
    }

    public int getProfileSpellPoints(UUID uuid, int slot) {
        return config.getInt("players." + uuid + ".profiles." + slot + ".spells.points", 0);
    }

    public void setProfileSpellPoints(UUID uuid, int slot, int points) {
        config.set("players." + uuid + ".profiles." + slot + ".spells.points", Math.max(0, points));
    }

    public List<String> getProfileSpellLevels(UUID uuid, int slot) {
        return config.getStringList("players." + uuid + ".profiles." + slot + ".spells.levels");
    }

    public void setProfileSpellLevels(UUID uuid, int slot, List<String> levels) {
        config.set("players." + uuid + ".profiles." + slot + ".spells.levels", levels == null ? List.of() : new ArrayList<>(levels));
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
        String worldName = config.getString(base + "world");
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null && "world2".equalsIgnoreCase(worldName)) {
            world = Bukkit.getWorld("world");
            if (world != null) {
                // update stored world name so we don't have to check again
                config.set(base + "world", "world");
            }
        }
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
        String base = "players." + uuid + ".environment.buildings." + building + ".";
        config.set(base + "stage", stage);
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

    public void clearFastTravelData(UUID uuid) {
        config.set("players." + uuid + ".fasttravel", null);
    }

    // ----- Catacombs Progress -----

    public int getCatacombsBestStage(UUID uuid, int slot) {
        String path = "players." + uuid + ".profiles." + slot + ".catacombs.best_stage";
        return config.getInt(path, 0);
    }

    public void setCatacombsBestStage(UUID uuid, int slot, int stage) {
        String base = "players." + uuid + ".profiles." + slot + ".catacombs.";
        config.set(base + "best_stage", Math.max(0, stage));
    }

    // ----- Generic Stage Dungeon Progress -----

    private String stagedDungeonPath(UUID uuid, int slot, String dungeonId) {
        String safeId = dungeonId == null || dungeonId.isBlank() ? "default" : dungeonId.toLowerCase(java.util.Locale.ROOT);
        return "players." + uuid + ".profiles." + slot + ".stage_dungeons." + safeId + ".";
    }

    public int getStagedDungeonBestStage(UUID uuid, int slot, String dungeonId) {
        return config.getInt(stagedDungeonPath(uuid, slot, dungeonId) + "best_stage", 0);
    }

    public void setStagedDungeonBestStage(UUID uuid, int slot, String dungeonId, int stage) {
        config.set(stagedDungeonPath(uuid, slot, dungeonId) + "best_stage", Math.max(0, stage));
    }

    public int getStagedDungeonSweepsUsed(UUID uuid, int slot, String dungeonId) {
        return config.getInt(stagedDungeonPath(uuid, slot, dungeonId) + "sweeps.used", 0);
    }

    public String getStagedDungeonSweepResetKey(UUID uuid, int slot, String dungeonId) {
        return config.getString(stagedDungeonPath(uuid, slot, dungeonId) + "sweeps.reset_key", "");
    }

    public void setStagedDungeonSweeps(UUID uuid, int slot, String dungeonId, int used, String resetKey) {
        String base = stagedDungeonPath(uuid, slot, dungeonId) + "sweeps.";
        config.set(base + "used", Math.max(0, used));
        config.set(base + "reset_key", resetKey == null ? "" : resetKey);
    }

    // ----- Global Town Ownership -----

    /** Get the UUID of the player who owns the specified global town. */
    public java.util.UUID getTownOwner(String town) {
        String path = "global_towns." + town.toLowerCase() + ".owner";
        String val = config.getString(path, null);
        return val != null ? java.util.UUID.fromString(val) : null;
    }

    /** Set or clear the owner UUID for a global town. */
    public void setTownOwner(String town, java.util.UUID owner) {
        String path = "global_towns." + town.toLowerCase() + ".owner";
        config.set(path, owner != null ? owner.toString() : null);
    }

    /** Remove the owner entry for a town. */
    public void clearTownOwner(String town) {
        setTownOwner(town, null);
    }

    /** All town names that currently have a registered owner. */
    public java.util.Set<String> getGlobalTownNames() {
        if (!config.isConfigurationSection("global_towns"))
            return java.util.Collections.emptySet();
        return config.getConfigurationSection("global_towns").getKeys(false);
    }

    // ----- Profile Data -----

    /** Get the stored profile name for a slot or null if not set. */
    public String getProfileName(UUID uuid, int slot) {
        String path = "players." + uuid + ".profiles." + slot + ".name";
        return config.getString(path, null);
    }

    /** Store the profile name for a slot. */
    public void setProfileName(UUID uuid, int slot, String name) {
        String path = "players." + uuid + ".profiles." + slot + ".name";
        config.set(path, name);
    }

    /** Remove all data for the profile slot. */
    public void clearProfileData(UUID uuid, int slot) {
        String path = "players." + uuid + ".profiles." + slot;
        config.set(path, null);
    }

    public org.bukkit.Location getProfileLocation(UUID uuid, int slot) {
        String base = "players." + uuid + ".profiles." + slot + ".";
        if (!config.contains(base + "world")) return null;
        String worldName = config.getString(base + "world");
        if (worldName == null) return null;

        org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName);
        if (w == null) {
            // Attempt to lazily load the world so stored locations remain valid
            me.nakilex.levelplugin.Main.getInstance()
                    .getWorldManager().ensureWorldsLoaded(worldName);
            w = org.bukkit.Bukkit.getWorld(worldName);
            if (w == null && "world2".equalsIgnoreCase(worldName)) {
                // Legacy world name fallback
                w = org.bukkit.Bukkit.getWorld("world");
                if (w != null) {
                    config.set(base + "world", "world");
                }
            }
        } else if ("world2".equalsIgnoreCase(worldName)) {
            // Normalize legacy name when world already loaded
            config.set(base + "world", "world");
        }
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

    public int getProfilePlayTime(java.util.UUID uuid, int slot) {
        String path = "players." + uuid + ".profiles." + slot + ".playtime";
        return config.getInt(path, 0);
    }

    public void setProfilePlayTime(java.util.UUID uuid, int slot, int minutes) {
        String path = "players." + uuid + ".profiles." + slot + ".playtime";
        config.set(path, minutes);
    }

    public int getUnlockedProfiles(UUID uuid) {
        return config.getInt("players." + uuid + ".profiles.unlocked", 1);
    }

    public void setUnlockedProfiles(UUID uuid, int count) {
        config.set("players." + uuid + ".profiles.unlocked", count);
    }

    public java.util.Set<String> getClearedDungeons(UUID uuid) {
        java.util.List<String> stored = config.getStringList("players." + uuid + ".cleared_dungeons");
        return new java.util.LinkedHashSet<>(stored);
    }

    public void addClearedDungeon(UUID uuid, String dungeonKey) {
        if (dungeonKey == null || dungeonKey.isBlank()) return;
        java.util.Set<String> cleared = getClearedDungeons(uuid);
        String normalized = dungeonKey.toLowerCase(java.util.Locale.ENGLISH);
        if (cleared.add(normalized)) {
            config.set("players." + uuid + ".cleared_dungeons", new java.util.ArrayList<>(cleared));
            saveConfig();
        }
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
