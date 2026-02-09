package me.nakilex.levelplugin.pet.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetProfile {
    private final UUID ownerId;
    private String activePetId;
    private final Map<String, Integer> petXp = new HashMap<>();
    private final Map<String, Integer> petTiers = new HashMap<>();
    private final Map<String, Integer> petCopies = new HashMap<>();

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
        return petTiers.getOrDefault(petId, 0);
    }

    public void setPetTier(String petId, int tier) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        petTiers.put(petId, Math.max(0, tier));
    }

    public int getPetCopies(String petId) {
        return petCopies.getOrDefault(petId, 1);
    }

    public void addPetCopies(String petId, int amount) {
        if (petId == null || petId.isBlank()) {
            return;
        }
        int current = getPetCopies(petId);
        petCopies.put(petId, Math.max(1, current + amount));
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
