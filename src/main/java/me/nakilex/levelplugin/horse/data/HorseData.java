package me.nakilex.levelplugin.horse.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class HorseData {

    private String type;
    private boolean isVariant; // New field to distinguish variants
    private int speed;
    private int jumpHeight;
    private UUID ownerUUID;

    // Constructor
    public HorseData(String type, boolean isVariant, int speed, int jumpHeight, UUID ownerUUID) {
        this.type = type;
        this.isVariant = isVariant;
        this.speed = speed;
        this.jumpHeight = jumpHeight;
        this.ownerUUID = ownerUUID;
    }

    // Getters
    public String getType() {
        return type;
    }

    public boolean isVariant() { // New getter for variant check
        return isVariant;
    }

    public int getSpeed() {
        return speed;
    }

    public int getJumpHeight() {
        return jumpHeight;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    // Setters
    public void setType(String type) {
        this.type = type;
    }

    public void setVariant(boolean variant) {
        isVariant = variant;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setJumpHeight(int jumpHeight) {
        this.jumpHeight = jumpHeight;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    /** Pick a value based on weighted probabilities. */
    private static <T> T pickWeighted(Random random, Map<T, Double> weights) {
        double r = random.nextDouble();
        double cumulative = 0;
        for (var entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (r <= cumulative) return entry.getKey();
        }
        // Fallback to first entry if weights don't sum to 1.0
        return weights.keySet().iterator().next();
    }

    // Method to generate a random horse
    public static HorseData randomHorse(UUID ownerUUID) {
        Random random = new Random();

        // Weighted horse color selection
        Map<String, Double> colorWeights = new LinkedHashMap<>();
        colorWeights.put("WHITE", 0.20);
        colorWeights.put("CREAMY", 0.15);
        colorWeights.put("CHESTNUT", 0.15);
        colorWeights.put("BROWN", 0.15);
        colorWeights.put("GRAY", 0.10);
        colorWeights.put("BLACK", 0.10);
        colorWeights.put("DARK_BROWN", 0.05);
        colorWeights.put("ZOMBIE", 0.05);
        colorWeights.put("SKELETON", 0.05);

        String pickedType = pickWeighted(random, colorWeights);
        boolean isVariant = pickedType.equals("ZOMBIE") || pickedType.equals("SKELETON");

        // Weighted stat distribution: make high rolls rare
        Map<Integer, Double> statWeights = new LinkedHashMap<>();
        statWeights.put(1, 0.40);
        statWeights.put(2, 0.30);
        statWeights.put(3, 0.20);
        statWeights.put(4, 0.08);
        statWeights.put(5, 0.02);

        int baseSpeed = pickWeighted(random, statWeights);
        int baseJump  = pickWeighted(random, statWeights);

        return new HorseData(pickedType, isVariant, baseSpeed, baseJump, ownerUUID);
    }


    // Display horse stats as a string
    @Override
    public String toString() {
        return "Type: " + type + ", Variant: " + isVariant + ", Speed: " + speed + ", Jump Height: " + jumpHeight;
    }
}
