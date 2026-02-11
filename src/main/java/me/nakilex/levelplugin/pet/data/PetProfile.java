package me.nakilex.levelplugin.pet.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.Location;

public class PetProfile {
    private final UUID ownerId;
    private String activePetId;
    private ItemRarity autoDiscardRarity;
    private Location pendingSummonReturn;
    private boolean autoSkipSummonAnimation;
    private int pityPullsSinceLegendary;
    private final Map<String, Integer> petXp = new HashMap<>();
    private final Map<String, Integer> petTiers = new HashMap<>();
    private final Map<String, Integer> petCopies = new HashMap<>();
    private final Map<String, Long> lastAcquiredAt = new HashMap<>();
    private final java.util.Set<String> mergeLockedPets = new java.util.HashSet<>();

    public PetProfile(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String activePetId() {
        return activePetId;
    }

    public void setActivePetId(String activePetId) {
        this.activePetId = activePetId;
    }

    public ItemRarity autoDiscardRarity() {
        return autoDiscardRarity;
    }

    public void setAutoDiscardRarity(ItemRarity autoDiscardRarity) {
        this.autoDiscardRarity = autoDiscardRarity;
    }

    public boolean autoSkipSummonAnimation() {
        return autoSkipSummonAnimation;
    }

    public void setAutoSkipSummonAnimation(boolean autoSkipSummonAnimation) {
        this.autoSkipSummonAnimation = autoSkipSummonAnimation;
    }

    public int pityPullsSinceLegendary() {
        return Math.max(0, pityPullsSinceLegendary);
    }

    public void setPityPullsSinceLegendary(int pityPullsSinceLegendary) {
        this.pityPullsSinceLegendary = Math.max(0, pityPullsSinceLegendary);
    }

    public Location pendingSummonReturn() {
        return pendingSummonReturn == null ? null : pendingSummonReturn.clone();
    }

    public void setPendingSummonReturn(Location location) {
        pendingSummonReturn = location == null ? null : location.clone();
    }

    public void clearPendingSummonReturn() {
        pendingSummonReturn = null;
    }

    public int getPetXp(String petId) {
        return petXp.getOrDefault(petId, 0);
    }

    public void setPetXp(String petId, int xp) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        petXp.put(petId, Math.max(0, xp));
    }

    public int getPetTier(String petId) {
        return petTiers.getOrDefault(petId, 1);
    }

    public void setPetTier(String petId, int tier) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        petTiers.put(petId, Math.max(1, tier));
    }

    public int getPetCopies(String petId) {
        return petCopies.getOrDefault(petId, 0);
    }

    public void setPetCopies(String petId, int amount) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        petCopies.put(petId, Math.max(0, amount));
    }

    public void addPetCopies(String petId, int amount) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        int current = getPetCopies(petId);
        petCopies.put(petId, Math.max(0, current + amount));
        if (amount > 0) {
            lastAcquiredAt.put(petId, System.currentTimeMillis());
        }
    }

    public long getLastAcquiredAt(String petId) {
        return lastAcquiredAt.getOrDefault(petId, 0L);
    }

    public void setLastAcquiredAt(String petId, long timestamp) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        lastAcquiredAt.put(petId, Math.max(0L, timestamp));
    }

    public boolean isMergeLocked(String petId) {
        if (petId == null || petId.isBlank()) {
            return false;
        }
        return mergeLockedPets.contains(petId.toLowerCase(java.util.Locale.ROOT));
    }

    public void setMergeLocked(String petId, boolean locked) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        String key = petId.toLowerCase(java.util.Locale.ROOT);
        if (locked) {
            mergeLockedPets.add(key);
        } else {
            mergeLockedPets.remove(key);
        }
    }

    public java.util.Set<String> mergeLockedPets() {
        return java.util.Collections.unmodifiableSet(mergeLockedPets);
    }

    public java.util.Map<String, Long> lastAcquiredAt() {
        return java.util.Collections.unmodifiableMap(lastAcquiredAt);
    }

    public Map<String, Integer> petTiers() {
        return Collections.unmodifiableMap(petTiers);
    }

    public Map<String, Integer> petCopies() {
        return Collections.unmodifiableMap(petCopies);
    }

    public Map<String, Integer> petXp() {
        return Collections.unmodifiableMap(petXp);
    }
}
