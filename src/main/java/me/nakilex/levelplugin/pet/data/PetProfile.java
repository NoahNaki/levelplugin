package me.nakilex.levelplugin.pet.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetProfile {
    private final UUID ownerId;
    private String activePetId;
    private final Map<String, Integer> petXp = new HashMap<>();

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

    public Map<String, Integer> petXp() {
        return Collections.unmodifiableMap(petXp);
    }
}
