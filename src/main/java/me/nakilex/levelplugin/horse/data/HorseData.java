package me.nakilex.levelplugin.horse.data;

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
        String[] colors = { "WHITE", "CREAMY", "CHESTNUT", "BROWN", "BLACK", "GRAY", "DARK_BROWN" }; // Colors
        String[] variants = { "ZOMBIE", "SKELETON" }; // Variants

        boolean isVariant = random.nextBoolean(); // Randomly decide if it's a variant
        String randomType;

        if (isVariant) {
            randomType = variants[random.nextInt(variants.length)];
        } else {
            randomType = colors[random.nextInt(colors.length)];
        }

        int randomSpeed = random.nextInt(10) + 1; // 1-10
        int randomJumpHeight = random.nextInt(10) + 1; // 1-10

        return new HorseData(randomType, isVariant, randomSpeed, randomJumpHeight, ownerUUID);
    }

    // Display horse stats as a string
    @Override
    public String toString() {
        return "Type: " + type + ", Variant: " + isVariant + ", Speed: " + speed + ", Jump Height: " + jumpHeight;
    }
}
