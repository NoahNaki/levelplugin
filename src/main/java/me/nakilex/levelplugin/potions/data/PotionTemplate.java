package me.nakilex.levelplugin.potions.data;

import org.bukkit.Material;

public class PotionTemplate {

    private final String id;
    private final String name;
    private final Material material;
    private final int charges;
    private final int cooldownSeconds;

    public PotionTemplate(String id, String name, Material material, int charges, int cooldownSeconds) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.charges = charges;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCharges() {
        return charges;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
}
