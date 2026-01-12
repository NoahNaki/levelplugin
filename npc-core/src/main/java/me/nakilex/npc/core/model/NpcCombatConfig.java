package me.nakilex.npc.core.model;

public class NpcCombatConfig {
    private double maxHealth = 20.0;
    private double damage = 2.0;
    private double armor = 0.0;
    private double aggroRange = 16.0;

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getArmor() {
        return armor;
    }

    public void setArmor(double armor) {
        this.armor = armor;
    }

    public double getAggroRange() {
        return aggroRange;
    }

    public void setAggroRange(double aggroRange) {
        this.aggroRange = aggroRange;
    }
}
