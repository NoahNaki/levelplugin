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

    private List<PlayerProfile> createList() {
        List<PlayerProfile> list = new ArrayList<>(Collections.nCopies(TOTAL_SLOTS, null));
        return list;
    }

    public List<PlayerProfile> getProfiles(UUID uuid) {
        return profiles.computeIfAbsent(uuid, k -> createList());
    }

    public int getUnlockedSlots(UUID uuid) {
        return unlocked.getOrDefault(uuid, 1);
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
        list.set(slot, p);
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
        cfg.saveConfigFile();
    }

    public void unlockNextSlot(UUID uuid) {
        int unlockedSlots = getUnlockedSlots(uuid);
        if (unlockedSlots < TOTAL_SLOTS) {
            unlocked.put(uuid, unlockedSlots + 1);
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
}
