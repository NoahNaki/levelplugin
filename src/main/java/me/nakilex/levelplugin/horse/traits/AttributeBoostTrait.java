package me.nakilex.levelplugin.horse.traits;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

/**
 * Generic trait that temporarily increases a horse attribute.
 * Allows different attribute boost traits to reuse the same logic.
 */
public abstract class AttributeBoostTrait implements HorseTrait {
    private final String id;
    private final Attribute attribute;
    private final double bonus;
    private final int durationTicks;
    private final int cooldownSeconds;

    protected AttributeBoostTrait(String id, Attribute attribute, double bonus, int durationTicks, int cooldownSeconds) {
        this.id = id;
        this.attribute = attribute;
        this.bonus = bonus;
        this.durationTicks = durationTicks;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public void apply(Player player, AbstractHorse horse) {
        AttributeInstance attr = horse.getAttribute(attribute);
        if (attr == null) return;
        double original = attr.getBaseValue();
        attr.setBaseValue(original + bonus);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> attr.setBaseValue(original), durationTicks);
    }
}
