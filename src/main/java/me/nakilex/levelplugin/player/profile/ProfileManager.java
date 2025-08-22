package me.nakilex.levelplugin.player.profile;

import java.util.*;
import org.bukkit.entity.Player;

import me.nakilex.levelplugin.utils.PotionEffectUtil;

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
        cfg.setProfileName(uuid, slot, name);
        cfg.setProfilePlayTime(uuid, slot, 0);
        cfg.saveConfigFile();
        return p;
    }

    /**
     * Reset all persistent data for the given player.
     */
    public void wipePlayer(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        me.nakilex.levelplugin.player.attributes.managers.StatsManager stats =
                me.nakilex.levelplugin.player.attributes.managers.StatsManager.getInstance();
        stats.resetPlayer(uuid);
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
        list.set(slot, null);
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        cfg.clearProfileData(uuid, slot);
        wipePlayer(player);
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
