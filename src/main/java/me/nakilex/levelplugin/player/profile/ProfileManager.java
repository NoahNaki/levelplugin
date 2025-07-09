package me.nakilex.levelplugin.player.profile;

import java.util.*;

public class ProfileManager {
    private static final ProfileManager instance = new ProfileManager();
    public static ProfileManager getInstance() { return instance; }

    private static final int TOTAL_SLOTS = 4;

    private final Map<java.util.UUID, java.util.List<PlayerProfile>> profiles = new HashMap<>();
    private final Map<java.util.UUID, Integer> unlocked = new HashMap<>();

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

    public PlayerProfile createProfile(UUID uuid, int slot) {
        if (slot >= getUnlockedSlots(uuid)) return null;
        List<PlayerProfile> list = getProfiles(uuid);
        if (list.get(slot) != null) return list.get(slot);
        PlayerProfile p = new PlayerProfile(slot, "Profile " + (slot + 1));
        list.set(slot, p);
        return p;
    }

    public void unlockNextSlot(UUID uuid) {
        int unlockedSlots = getUnlockedSlots(uuid);
        if (unlockedSlots < TOTAL_SLOTS) {
            unlocked.put(uuid, unlockedSlots + 1);
        }
    }
}
