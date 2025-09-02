package me.nakilex.levelplugin.horse.traits;

import org.bukkit.attribute.Attribute;

/**
 * Temporarily boosts the horse's movement speed.
 */
public class DashTrait extends AttributeBoostTrait {
    public DashTrait() {
        super("dash", Attribute.MOVEMENT_SPEED, 0.25, 20 * 3, 30);
    }
}
