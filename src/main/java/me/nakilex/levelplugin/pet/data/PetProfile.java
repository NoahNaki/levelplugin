package me.nakilex.levelplugin.pet.data;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.Location;

public class PetProfile {
    private final UUID ownerId;
    private String activePetId;
    private ItemRarity autoDiscardRarity;
    private Location pendingSummonReturn;
    private boolean autoSkipSummonAnimation;
    private PetVisibility petVisibility = PetVisibility.ALL;
    private int pityPullsSinceLegendary;
    private int bannerPulls;
    private final Map<String, Integer> petXp = new HashMap<>();
    private final Map<String, Integer> petTiers = new HashMap<>();
    private final Map<String, Integer> petCopies = new HashMap<>();
    private final Map<String, Long> lastAcquiredAt = new HashMap<>();
    private final Map<String, List<Long>> petCopyAcquiredAt = new HashMap<>();
    private final Map<String, List<String>> petCopyIds = new HashMap<>();
    private final Set<String> mergeLockedCopyIds = new HashSet<>();

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

    public PetVisibility petVisibility() {
        return petVisibility == null ? PetVisibility.ALL : petVisibility;
    }

    public void setPetVisibility(PetVisibility petVisibility) {
        this.petVisibility = petVisibility == null ? PetVisibility.ALL : petVisibility;
    }

    public int pityPullsSinceLegendary() {
        return Math.max(0, pityPullsSinceLegendary);
    }

    public void setPityPullsSinceLegendary(int pityPullsSinceLegendary) {
        this.pityPullsSinceLegendary = Math.max(0, pityPullsSinceLegendary);
    }

    public int bannerPulls() {
        return Math.max(0, bannerPulls);
    }

    public void setBannerPulls(int bannerPulls) {
        this.bannerPulls = Math.max(0, bannerPulls);
    }

    public void addBannerPulls(int amount) {
        if (amount <= 0) {
            return;
        }
        setBannerPulls(bannerPulls() + amount);
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
        int clamped = Math.max(0, amount);
        petCopies.put(petId, clamped);
        syncCopyStateSize(petId, clamped);
    }

    public void addPetCopies(String petId, int amount) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        int current = getPetCopies(petId);
        int newAmount = Math.max(0, current + amount);
        petCopies.put(petId, newAmount);
        if (amount > 0) {
            long now = System.currentTimeMillis();
            lastAcquiredAt.put(petId, now);
            List<Long> history = petCopyAcquiredAt.computeIfAbsent(petId, key -> new ArrayList<>());
            List<String> ids = petCopyIds.computeIfAbsent(petId, key -> new ArrayList<>());
            for (int i = 0; i < amount; i++) {
                history.add(now);
                ids.add(UUID.randomUUID().toString());
            }
        }
        syncCopyStateSize(petId, newAmount);
    }

    public int removePetCopies(String petId, int amount) {
        if (petId == null || petId.isBlank() || amount <= 0) {
            return 0;
        }
        int current = getPetCopies(petId);
        int removed = Math.min(current, amount);
        int remaining = Math.max(0, current - removed);
        petCopies.put(petId, remaining);
        List<Long> history = petCopyAcquiredAt.get(petId);
        List<String> ids = petCopyIds.get(petId);
        if (history != null && !history.isEmpty()) {
            int trim = Math.min(removed, history.size());
            history.subList(0, trim).clear();
        }
        if (ids != null && !ids.isEmpty()) {
            int trim = Math.min(removed, ids.size());
            List<String> removedIds = new ArrayList<>(ids.subList(0, trim));
            ids.subList(0, trim).clear();
            mergeLockedCopyIds.removeAll(removedIds);
        }
        syncCopyStateSize(petId, remaining);
        return removed;
    }

    public int removePetCopiesByIds(String petId, List<String> copyIdsToRemove) {
        if (petId == null || petId.isBlank() || copyIdsToRemove == null || copyIdsToRemove.isEmpty()) {
            return 0;
        }
        List<String> ids = petCopyIds.get(petId);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Set<String> targets = new HashSet<>(copyIdsToRemove);
        List<Long> history = petCopyAcquiredAt.getOrDefault(petId, new ArrayList<>());
        int removed = 0;
        for (int i = ids.size() - 1; i >= 0; i--) {
            String id = ids.get(i);
            if (!targets.contains(id)) {
                continue;
            }
            ids.remove(i);
            if (i < history.size()) {
                history.remove(i);
            }
            mergeLockedCopyIds.remove(id);
            removed++;
        }
        petCopies.put(petId, Math.max(0, getPetCopies(petId) - removed));
        syncCopyStateSize(petId, getPetCopies(petId));
        return removed;
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

    public List<Long> getPetCopyAcquiredHistory(String petId) {
        List<Long> history = petCopyAcquiredAt.get(petId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(history);
    }

    public void setPetCopyAcquiredHistory(String petId, List<Long> timestamps) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        List<Long> values = new ArrayList<>();
        if (timestamps != null) {
            for (Long value : timestamps) {
                if (value != null && value > 0) {
                    values.add(value);
                }
            }
        }
        petCopyAcquiredAt.put(petId, values);
        syncCopyStateSize(petId, getPetCopies(petId));
    }

    public List<String> getPetCopyIds(String petId) {
        List<String> ids = petCopyIds.get(petId);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(ids);
    }

    public void setPetCopyIds(String petId, List<String> ids) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        List<String> values = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    values.add(id);
                }
            }
        }
        petCopyIds.put(petId, values);
        syncCopyStateSize(petId, getPetCopies(petId));
    }

    public Map<String, List<Long>> petCopyAcquiredAt() {
        Map<String, List<Long>> snapshot = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : petCopyAcquiredAt.entrySet()) {
            snapshot.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private void syncCopyStateSize(String petId, int targetCopies) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        int expected = Math.max(0, targetCopies);
        List<Long> history = petCopyAcquiredAt.computeIfAbsent(petId, key -> new ArrayList<>());
        List<String> ids = petCopyIds.computeIfAbsent(petId, key -> new ArrayList<>());
        if (history.size() > expected) {
            history.subList(expected, history.size()).clear();
        }
        if (ids.size() > expected) {
            List<String> removedIds = new ArrayList<>(ids.subList(expected, ids.size()));
            ids.subList(expected, ids.size()).clear();
            mergeLockedCopyIds.removeAll(removedIds);
        }
        if (history.size() < expected) {
            long fallback = getLastAcquiredAt(petId);
            if (fallback <= 0L) {
                fallback = System.currentTimeMillis();
                lastAcquiredAt.put(petId, fallback);
            }
            while (history.size() < expected) {
                history.add(fallback);
            }
        }
        while (ids.size() < expected) {
            ids.add(UUID.randomUUID().toString());
        }
    }

    public boolean isMergeLockedCopy(String copyId) {
        if (copyId == null || copyId.isBlank()) {
            return false;
        }
        return mergeLockedCopyIds.contains(copyId);
    }

    public void setMergeLockedCopy(String copyId, boolean locked) {
        if (copyId == null || copyId.isBlank()) {
            return;
        }
        if (locked) {
            mergeLockedCopyIds.add(copyId);
        } else {
            mergeLockedCopyIds.remove(copyId);
        }
    }

    public Set<String> mergeLockedCopyIds() {
        return Collections.unmodifiableSet(mergeLockedCopyIds);
    }

    public int getUnlockedCopyCount(String petId) {
        if (petId == null || petId.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String copyId : getPetCopyIds(petId)) {
            if (!isMergeLockedCopy(copyId)) {
                count++;
            }
        }
        return count;
    }

    public List<String> getFirstUnlockedCopyIds(String petId, int amount) {
        List<String> result = new ArrayList<>();
        if (petId == null || petId.isBlank() || amount <= 0) {
            return result;
        }
        for (String copyId : getPetCopyIds(petId)) {
            if (isMergeLockedCopy(copyId)) {
                continue;
            }
            result.add(copyId);
            if (result.size() >= amount) {
                break;
            }
        }
        return result;
    }

    public String findPetIdByCopyId(String copyId) {
        if (copyId == null || copyId.isBlank()) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : petCopyIds.entrySet()) {
            if (entry.getValue().contains(copyId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public java.util.Map<String, Long> lastAcquiredAt() {
        return java.util.Collections.unmodifiableMap(lastAcquiredAt);
    }

    public Map<String, List<String>> petCopyIds() {
        Map<String, List<String>> snapshot = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : petCopyIds.entrySet()) {
            snapshot.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(snapshot);
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
