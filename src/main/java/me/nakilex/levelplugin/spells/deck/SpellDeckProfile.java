package me.nakilex.levelplugin.spells.deck;

import me.nakilex.levelplugin.spells.input.SpellInputType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpellDeckProfile {
    private final UUID ownerId;
    private final Map<String, Integer> ownedCopies = new HashMap<>();
    private final Map<SpellInputType, String> equippedCards = new EnumMap<>(SpellInputType.class);
    private final Map<String, Integer> investedCopies = new HashMap<>();
    private int pityPullsSinceLegendary;

    public SpellDeckProfile(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public Map<String, Integer> ownedCopies() {
        return ownedCopies;
    }

    public Map<SpellInputType, String> equippedCards() {
        return equippedCards;
    }

    public Map<String, Integer> investedCopies() {
        return investedCopies;
    }

    public int getCopies(String cardId) {
        if (cardId == null) {
            return 0;
        }
        return Math.max(0, ownedCopies.getOrDefault(cardId.toLowerCase(java.util.Locale.ROOT), 0));
    }

    public void setCopies(String cardId, int copies) {
        if (cardId == null || cardId.isBlank()) {
            return;
        }
        String normalized = cardId.toLowerCase(java.util.Locale.ROOT);
        if (copies <= 0) {
            ownedCopies.remove(normalized);
            return;
        }
        ownedCopies.put(normalized, copies);
    }

    public void addCopies(String cardId, int amount) {
        if (cardId == null || cardId.isBlank() || amount <= 0) {
            return;
        }
        setCopies(cardId, getCopies(cardId) + amount);
    }

    public String getEquippedCardId(SpellInputType inputType) {
        return inputType == null ? null : equippedCards.get(inputType);
    }


    public SpellInputType getEquippedSlot(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            return null;
        }
        String normalized = cardId.toLowerCase(java.util.Locale.ROOT);
        for (Map.Entry<SpellInputType, String> entry : equippedCards.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(normalized)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void equip(SpellInputType inputType, String cardId) {
        if (inputType == null) {
            return;
        }
        if (cardId == null || cardId.isBlank()) {
            equippedCards.remove(inputType);
            return;
        }
        equippedCards.put(inputType, cardId.toLowerCase(java.util.Locale.ROOT));
    }


    public int getInvestedCopies(String cardId) {
        if (cardId == null) {
            return 0;
        }
        return Math.max(0, investedCopies.getOrDefault(cardId.toLowerCase(java.util.Locale.ROOT), 0));
    }

    public void setInvestedCopies(String cardId, int copies) {
        if (cardId == null || cardId.isBlank()) {
            return;
        }
        String normalized = cardId.toLowerCase(java.util.Locale.ROOT);
        if (copies <= 0) {
            investedCopies.remove(normalized);
            return;
        }
        investedCopies.put(normalized, copies);
    }

    public void addInvestedCopies(String cardId, int amount) {
        if (cardId == null || cardId.isBlank() || amount <= 0) {
            return;
        }
        setInvestedCopies(cardId, getInvestedCopies(cardId) + amount);
    }

    public int pityPullsSinceLegendary() {
        return Math.max(0, pityPullsSinceLegendary);
    }

    public void setPityPullsSinceLegendary(int pityPullsSinceLegendary) {
        this.pityPullsSinceLegendary = Math.max(0, pityPullsSinceLegendary);
    }
}
