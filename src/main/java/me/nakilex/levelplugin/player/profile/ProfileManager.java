package me.nakilex.levelplugin.player.profile;

import java.util.*;
import org.bukkit.entity.Player;

import me.nakilex.levelplugin.utils.PotionEffectUtil;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.input.SpellKeybindManager;

public class ProfileManager {
    private static final ProfileManager instance = new ProfileManager();
    public static ProfileManager getInstance() { return instance; }

    private static final int TOTAL_SLOTS = 4;

    private final Map<java.util.UUID, java.util.List<PlayerProfile>> profiles = new HashMap<>();
    private final Map<java.util.UUID, Integer> unlocked = new HashMap<>();
    private final Map<java.util.UUID, Integer> activeSlot = new HashMap<>();

    private ProfileManager() {}

    private List<PlayerProfile> loadProfiles(UUID uuid) {
        List<PlayerProfile> list = new ArrayList<>(Collections.nCopies(TOTAL_SLOTS, null));
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            String name = cfg.getProfileName(uuid, i);
            if (name != null) {
                PlayerProfile prof = new PlayerProfile(i, name);
                prof.setPlayMinutes(cfg.getProfilePlayTime(uuid, i));
                list.set(i, prof);
            }
        }
        int un = cfg.getUnlockedProfiles(uuid);
        unlocked.put(uuid, un);
        return list;
    }

    public List<PlayerProfile> getProfiles(UUID uuid) {
        return profiles.computeIfAbsent(uuid, this::loadProfiles);
    }

    public int getUnlockedSlots(UUID uuid) {
        return unlocked.computeIfAbsent(uuid, id -> {
            me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                    me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
            return cfg.getUnlockedProfiles(id);
        });
    }

    public PlayerProfile getProfile(UUID uuid, int slot) {
        List<PlayerProfile> list = getProfiles(uuid);
        if (slot < 0 || slot >= list.size()) return null;
        return list.get(slot);
    }

    public PlayerProfile createProfile(UUID uuid, int slot, String name) {
        if (slot >= getUnlockedSlots(uuid)) return null;
        List<PlayerProfile> list = getProfiles(uuid);
        if (list.get(slot) != null) return list.get(slot);
        if (name == null || name.isBlank()) name = "Profile " + (slot + 1);
        PlayerProfile p = new PlayerProfile(slot, name);
        p.setPlayMinutes(0);
        list.set(slot, p);
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        cfg.clearProfileData(uuid, slot);
        cfg.setProfileName(uuid, slot, name);
        cfg.setProfilePlayTime(uuid, slot, 0);
        cfg.saveConfigFile();
        return p;
    }

    /** Rename an existing profile. */
    public boolean renameProfile(UUID uuid, int slot, String name) {
        PlayerProfile prof = getProfile(uuid, slot);
        if (prof == null || name == null || name.isBlank()) return false;
        prof.setName(name);
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        cfg.setProfileName(uuid, slot, name);
        cfg.saveConfigFile();
        return true;
    }

    /**
     * Reset all persistent data for the given player.
     */
    public void wipePlayer(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        me.nakilex.levelplugin.player.attributes.managers.StatsManager stats =
                me.nakilex.levelplugin.player.attributes.managers.StatsManager.getInstance();
        stats.resetPlayer(uuid);
        SpellProgressionManager.getInstance().clearPlayer(uuid);
        me.nakilex.levelplugin.player.level.managers.LevelManager lm =
                me.nakilex.levelplugin.player.level.managers.LevelManager.getInstance();
        lm.setLevel(uuid, 1);
        me.nakilex.levelplugin.Main plugin = me.nakilex.levelplugin.Main.getInstance();
        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().setBalance(uuid, 0);
        }
        if (plugin.getGemsManager() != null) {
            plugin.getGemsManager().setTotalUnits(player, 0);
        }
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().clearPlayerData(uuid);
        }
        if (plugin.getStorageManager() != null) {
            plugin.getStorageManager().deleteStorage(uuid);
        }
        if (plugin.getCodexManager() != null) {
            plugin.getCodexManager().clearPlayerData(uuid);
        }
        if (plugin.getMiningManager() != null) {
            plugin.getMiningManager().clearPlayerData(uuid);
        }
        if (plugin.getWoodcuttingManager() != null) {
            plugin.getWoodcuttingManager().clearPlayerData(uuid);
        }
        if (plugin.getHorseManager() != null) {
            plugin.getHorseManager().clearPlayerData(uuid);
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        PotionEffectUtil.clearAllEffects(player);
        stats.recalcDerivedStats(player);
        // Reset the player's visible XP bar to match the wiped profile
        me.nakilex.levelplugin.player.level.managers.XPBarHandler.updateXPBar(player, lm);

        me.nakilex.levelplugin.player.config.PlayerConfig cfg = plugin.getPlayerConfig();
        cfg.clearEnvironmentData(uuid);
        cfg.clearFastTravelData(uuid);
        plugin.getFastTravelManager().clearUnlocked(uuid);
        cfg.savePlayer(uuid);
    }

    /**
     * Remove the profile from the given slot and clear any stored location.
     */
    public void deleteProfile(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        List<PlayerProfile> list = getProfiles(uuid);
        if (slot < 0 || slot >= list.size()) {
            return;
        }
        boolean wasActive = Objects.equals(activeSlot.get(uuid), slot);
        me.nakilex.levelplugin.guild.GuildManager guildManager = me.nakilex.levelplugin.guild.GuildManager.getInstance();
        guildManager.handleProfileDeletion(uuid);
        me.nakilex.levelplugin.party.PartyManager partyManager = me.nakilex.levelplugin.Main.getInstance().getPartyManager();
        if (partyManager != null) {
            partyManager.leaveParty(uuid);
        }
        me.nakilex.levelplugin.pet.PetManager petManager = me.nakilex.levelplugin.Main.getInstance().getPetManager();
        if (petManager != null) {
            petManager.handleProfileDeletion(uuid);
        }
        if (wasActive) {
            wipePlayer(player);
            clearActiveSlot(uuid);
        }
        list.set(slot, null);
        SpellProgressionManager.getInstance().clearProfile(uuid, slot);
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        cfg.setProfileSpellPoints(uuid, slot, 0);
        cfg.setProfileSpellLevels(uuid, slot, List.of());
        cfg.clearProfileData(uuid, slot);
        cfg.saveConfigFile();
    }

    public void unlockNextSlot(UUID uuid) {
        int unlockedSlots = getUnlockedSlots(uuid);
        if (unlockedSlots < TOTAL_SLOTS) {
            unlockedSlots++;
            unlocked.put(uuid, unlockedSlots);
            me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                    me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
            cfg.setUnlockedProfiles(uuid, unlockedSlots);
            cfg.saveConfigFile();
        }
    }

    /** Set the number of unlocked profile slots for a player. */
    public void setUnlockedSlots(UUID uuid, int count) {
        if (count < 1) count = 1;
        if (count > TOTAL_SLOTS) count = TOTAL_SLOTS;
        unlocked.put(uuid, count);
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        cfg.setUnlockedProfiles(uuid, count);
        cfg.saveConfigFile();
    }

    public void setActiveSlot(UUID uuid, int slot) {
        activeSlot.put(uuid, slot);
    }

    public void clearActiveSlot(UUID uuid) {
        activeSlot.remove(uuid);
    }

    public Integer getActiveSlot(UUID uuid) {
        return activeSlot.get(uuid);
    }

    public void saveProfile(org.bukkit.entity.Player player, int slot) {
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        java.util.UUID id = player.getUniqueId();
        cfg.setProfileInventory(id, slot, player.getInventory().getContents());
        cfg.setProfileArmor(id, slot, player.getInventory().getArmorContents());
        cfg.setProfileLocation(id, slot, player.getLocation());
        SpellProgressionManager progressionManager = SpellProgressionManager.getInstance();
        cfg.setProfileSpellPoints(id, slot, progressionManager.getSpellPoints(id));
        cfg.setProfileSpellLevels(id, slot, progressionManager.serializeSpellLevels(id, slot));
        SettingsManager settingsManager = me.nakilex.levelplugin.Main.getInstance().getSettingsManager();
        if (settingsManager != null) {
            settingsManager.saveProfileSettings(id, slot);
        }
        SpellKeybindManager.getInstance().saveProfileBindings(id, slot);
        cfg.saveConfigFile();
    }

    public void saveActiveProfile(org.bukkit.entity.Player player) {
        Integer slot = activeSlot.get(player.getUniqueId());
        if (slot != null) {
            saveProfile(player, slot);
        }
    }

    public void addPlayMinutes(java.util.UUID uuid, int minutes) {
        Integer slot = activeSlot.get(uuid);
        if (slot == null) return;
        PlayerProfile prof = getProfile(uuid, slot);
        if (prof == null) return;
        prof.addPlayMinutes(minutes);
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        cfg.setProfilePlayTime(uuid, slot, prof.getPlayMinutes());
        cfg.saveConfigFile();
    }
}
