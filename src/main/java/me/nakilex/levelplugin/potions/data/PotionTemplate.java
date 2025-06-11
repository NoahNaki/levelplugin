package me.nakilex.levelplugin.potions.data;

import org.bukkit.Material;

public class PotionTemplate {

    private final String id;
    private final String name;
    private final Material material;
    private final String nexoId;
    private final int charges;
    private final int cooldownSeconds;
    private final int tier;
    private final double healAmount;
    private final double healPercent;

    public PotionTemplate(String id, String name, Material material, String nexoId,
                          int charges, int cooldownSeconds,
                          int tier, double healAmount, double healPercent) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.nexoId = nexoId;
        this.charges = charges;
        this.cooldownSeconds = cooldownSeconds;
        this.tier = tier;
        this.healAmount = healAmount;
        this.healPercent = healPercent;
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

    public String getNexoId() {
        return nexoId;
    }

    public int getCharges() {
        return charges;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getTier() {
        return tier;
    }

    public double getHealAmount() {
        return healAmount;
    }

    public double getHealPercent() {
        return healPercent;
    }
}
