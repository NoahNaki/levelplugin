package me.nakilex.levelplugin.player.profile;

import java.util.*;

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
     * Remove the profile from the given slot and clear any stored location.
     */
    public void deleteProfile(UUID uuid, int slot) {
        List<PlayerProfile> list = getProfiles(uuid);
        if (slot < 0 || slot >= list.size()) {
            return;
        }
        list.set(slot, null);
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        cfg.setProfileLocation(uuid, slot, null);
        cfg.setProfileName(uuid, slot, null);
        cfg.setProfilePlayTime(uuid, slot, 0);
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

    public void saveActiveLocation(org.bukkit.entity.Player player) {
        Integer slot = activeSlot.get(player.getUniqueId());
        if (slot == null) return;
        me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                me.nakilex.levelplugin.Main.getInstance().getPlayerConfig();
        cfg.setProfileLocation(player.getUniqueId(), slot, player.getLocation());
        cfg.saveConfigFile();
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
