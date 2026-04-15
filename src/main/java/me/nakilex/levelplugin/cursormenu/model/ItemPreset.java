package me.nakilex.levelplugin.cursormenu.model;

public class ItemPreset {
    private final String material;
    private final Integer customModelData;
    private final float scale;
    private final double distance;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final boolean glowEnabled;
    private final boolean rotateEnabled;
    private final float rotateSpeed;

    public ItemPreset(String material,
                      Integer customModelData,
                      float scale,
                      double distance,
                      double offsetX,
                      double offsetY,
                      double offsetZ,
                      boolean glowEnabled,
                      boolean rotateEnabled,
                      float rotateSpeed) {
        this.material = material;
        this.customModelData = customModelData;
        this.scale = scale;
        this.distance = distance;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.glowEnabled = glowEnabled;
        this.rotateEnabled = rotateEnabled;
        this.rotateSpeed = rotateSpeed;
    }

    public String material() { return material; }
    public Integer customModelData() { return customModelData; }
    public float scale() { return scale; }
    public double distance() { return distance; }
    public double offsetX() { return offsetX; }
    public double offsetY() { return offsetY; }
    public double offsetZ() { return offsetZ; }
    public boolean glowEnabled() { return glowEnabled; }
    public boolean rotateEnabled() { return rotateEnabled; }
    public float rotateSpeed() { return rotateSpeed; }
}
