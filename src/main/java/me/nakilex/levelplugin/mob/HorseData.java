package me.nakilex.levelplugin.mob;

import java.util.UUID;
import java.util.Random;

public class HorseData {

    private String type;
    private int speed;
    private int jumpHeight;
    private UUID ownerUUID;

    // Constructor
    public HorseData(String type, int speed, int jumpHeight, UUID ownerUUID) {
        this.type = type;
        this.speed = speed;
        this.jumpHeight = jumpHeight;
        this.ownerUUID = ownerUUID;
    }

    // Getters
    public String getType() {
        return type;
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
        String[] types = {"brown", "black", "white", "zombie", "skeleton"};

        String randomType = types[random.nextInt(types.length)];
        int randomSpeed = random.nextInt(10) + 1; // 1-10
        int randomJumpHeight = random.nextInt(10) + 1; // 1-10

        return new HorseData(randomType, randomSpeed, randomJumpHeight, ownerUUID);
    }

    // Display horse stats as a string
    @Override
    public String toString() {
        return "Type: " + type + ", Speed: " + speed + ", Jump Height: " + jumpHeight;
    }
}
