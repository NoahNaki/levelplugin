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

    // Method to generate a random horse
    public static HorseData randomHorse(UUID ownerUUID) {
        Random random = new Random();

        // Define your weights (must sum to 1.0)
        Map<String, Double> weights = new LinkedHashMap<>();
        // Common colors
        weights.put("WHITE", 0.20);
        weights.put("CREAMY", 0.15);
        weights.put("CHESTNUT", 0.15);
        weights.put("BROWN", 0.15);
        // Rarer colors
        weights.put("GRAY", 0.10);
        weights.put("BLACK", 0.10);
        weights.put("DARK_BROWN", 0.05);
        // Very rare variants
        weights.put("ZOMBIE", 0.05);
        weights.put("SKELETON", 0.05);

        // Pick one entry by weight
        double r = random.nextDouble();
        double cum = 0;
        String pickedType = null;
        for (var entry : weights.entrySet()) {
            cum += entry.getValue();
            if (r <= cum) {
                pickedType = entry.getKey();
                break;
            }
        }
        // Fallback (shouldn’t happen if weights sum to 1): pick WHITE
        if (pickedType == null) pickedType = "WHITE";

        boolean isVariant = pickedType.equals("ZOMBIE") || pickedType.equals("SKELETON");

        // Speed & jump still 1–10
        int baseSpeed = random.nextInt(10) + 1;
        int baseJump  = random.nextInt(10) + 1;

        return new HorseData(pickedType, isVariant, baseSpeed, baseJump, ownerUUID);
    }


    // Display horse stats as a string
    @Override
    public String toString() {
        return "Type: " + type + ", Variant: " + isVariant + ", Speed: " + speed + ", Jump Height: " + jumpHeight;
    }
}
