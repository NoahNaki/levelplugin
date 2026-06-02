package me.nakilex.levelplugin.player.attributes.lifeskill;

import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.player.attributes.managers.LifeSkillRewardManager;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.player.fishing.data.FishingQuality;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persists and restores all runtime life-skill state as one reusable profile-scoped unit. */
public final class LifeSkillProfileDataUtil {
    private LifeSkillProfileDataUtil() {
    }

    /** Writes the current runtime state beneath a profile's life-skill root. */
    public static void save(UUID uuid, FileConfiguration config, String root) {
        if (uuid == null || config == null || root == null) return;
        config.set(root, null);
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            LifeSkillProgression progression = progression(discipline);
            String disciplineRoot = disciplineRoot(root, discipline);
            config.set(disciplineRoot + ".level", progression.getLevel(uuid));
            config.set(disciplineRoot + ".xp", progression.getXP(uuid));
            LifeSkillRewardManager rewardManager = LifeSkillRewardManager.getInstance();
            if (rewardManager != null) {
                config.set(disciplineRoot + ".claimed", new ArrayList<>(rewardManager.getClaimed(uuid, discipline)));
            }
        }
        saveFishingCollection(uuid, config, disciplineRoot(root, ToolDiscipline.FISHING));
    }

    /** Creates a blank profile-scoped life-skill payload without changing another active profile's runtime data. */
    public static void initializeBlank(FileConfiguration config, String root) {
        if (config == null || root == null) return;
        config.set(root, null);
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            String disciplineRoot = disciplineRoot(root, discipline);
            config.set(disciplineRoot + ".level", 1);
            config.set(disciplineRoot + ".xp", 0);
            config.set(disciplineRoot + ".claimed", new ArrayList<>());
        }
        config.set(disciplineRoot(root, ToolDiscipline.FISHING) + ".discovered", new ArrayList<>());
    }

    /** Loads a profile payload into the UUID-keyed runtime managers, resetting absent values to defaults. */
    public static void load(UUID uuid, FileConfiguration config, String root) {
        if (uuid == null || config == null || root == null) return;
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            LifeSkillProgression progression = progression(discipline);
            String disciplineRoot = disciplineRoot(root, discipline);
            progression.setLevel(uuid, config.getInt(disciplineRoot + ".level", 1));
            progression.addXP(uuid, config.getInt(disciplineRoot + ".xp", 0));
            LifeSkillRewardManager rewardManager = LifeSkillRewardManager.getInstance();
            if (rewardManager != null) {
                rewardManager.setClaimed(uuid, discipline,
                        new HashSet<>(config.getIntegerList(disciplineRoot + ".claimed")));
            }
        }
        MiningManager.getInstance().clearMomentum(uuid);
        loadFishingCollection(uuid, config, disciplineRoot(root, ToolDiscipline.FISHING));
    }

    /** Copies the legacy account-root life-skill payload into a profile before loading it. */
    public static void migrateLegacy(FileConfiguration config, String legacyRoot, String profileRoot) {
        if (config == null || legacyRoot == null || profileRoot == null) return;
        config.set(profileRoot, null);
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            String legacyDisciplineRoot = legacyRoot + "." + discipline.name().toLowerCase();
            String profileDisciplineRoot = disciplineRoot(profileRoot, discipline);
            config.set(profileDisciplineRoot + ".level", config.getInt(legacyDisciplineRoot + ".level", 1));
            config.set(profileDisciplineRoot + ".xp", config.getInt(legacyDisciplineRoot + ".xp", 0));
            config.set(profileDisciplineRoot + ".claimed",
                    config.getIntegerList(legacyRoot + ".lifeskills." + discipline.name().toLowerCase() + ".claimed"));
        }
        String legacyFishingRoot = legacyRoot + ".fishing";
        String profileFishingRoot = disciplineRoot(profileRoot, ToolDiscipline.FISHING);
        config.set(profileFishingRoot + ".discovered", config.getStringList(legacyFishingRoot + ".discovered"));
        ConfigurationSection records = config.getConfigurationSection(legacyFishingRoot + ".records");
        if (records != null) {
            for (String fishId : records.getKeys(false)) {
                String source = legacyFishingRoot + ".records." + fishId;
                String target = profileFishingRoot + ".records." + fishId;
                config.set(target + ".caught", config.getInt(source + ".caught", 0));
                config.set(target + ".largest", config.getDouble(source + ".largest", 0.0));
                config.set(target + ".quality", config.getString(source + ".quality", FishingQuality.NORMAL.name()));
            }
        }
    }

    /** Resets UUID-keyed runtime state when an active profile is deleted or wiped. */
    public static void resetRuntime(UUID uuid) {
        if (uuid == null) return;
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            LifeSkillProgression progression = progression(discipline);
            progression.setLevel(uuid, 1);
            progression.addXP(uuid, 0);
            LifeSkillRewardManager rewardManager = LifeSkillRewardManager.getInstance();
            if (rewardManager != null) rewardManager.setClaimed(uuid, discipline, Set.of());
        }
        MiningManager.getInstance().clearMomentum(uuid);
        FishingManager fishingManager = FishingManager.getInstance();
        fishingManager.setDiscoveredFish(uuid, Set.of());
        fishingManager.setFishRecords(uuid, Map.of());
    }

    private static LifeSkillProgression progression(ToolDiscipline discipline) {
        return switch (discipline) {
            case MINING -> MiningManager.getInstance();
            case FARMING -> FarmingManager.getInstance();
            case FISHING -> FishingManager.getInstance();
            case WOODCUTTING -> WoodcuttingManager.getInstance();
        };
    }

    private static String disciplineRoot(String root, ToolDiscipline discipline) {
        return root + "." + discipline.name().toLowerCase();
    }

    private static void saveFishingCollection(UUID uuid, FileConfiguration config, String root) {
        FishingManager fishingManager = FishingManager.getInstance();
        config.set(root + ".discovered", new ArrayList<>(fishingManager.getDiscoveredFish(uuid)));
        config.set(root + ".records", null);
        for (Map.Entry<String, FishingManager.FishRecord> entry : fishingManager.getFishRecords(uuid).entrySet()) {
            String recordRoot = root + ".records." + entry.getKey();
            FishingManager.FishRecord record = entry.getValue();
            config.set(recordRoot + ".caught", record.caughtCount());
            config.set(recordRoot + ".largest", record.largestSize());
            config.set(recordRoot + ".quality", record.bestQuality().name());
        }
    }

    private static void loadFishingCollection(UUID uuid, FileConfiguration config, String root) {
        FishingManager fishingManager = FishingManager.getInstance();
        fishingManager.setDiscoveredFish(uuid, new HashSet<>(config.getStringList(root + ".discovered")));
        Map<String, FishingManager.FishRecord> records = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection(root + ".records");
        if (section != null) {
            for (String fishId : section.getKeys(false)) {
                String recordRoot = root + ".records." + fishId;
                FishingQuality quality;
                try {
                    quality = FishingQuality.valueOf(config.getString(recordRoot + ".quality", FishingQuality.NORMAL.name()));
                } catch (IllegalArgumentException ignored) {
                    quality = FishingQuality.NORMAL;
                }
                records.put(fishId, new FishingManager.FishRecord(
                        config.getInt(recordRoot + ".caught", 0),
                        config.getDouble(recordRoot + ".largest", 0.0), quality));
            }
        }
        fishingManager.setFishRecords(uuid, records);
    }
}
