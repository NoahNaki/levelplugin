package me.nakilex.levelplugin.horse.traits;

import org.bukkit.attribute.Attribute;

/**
 * Temporarily increases the horse's jump strength.
 */
public class LeapTrait extends AttributeBoostTrait {
    public LeapTrait() {
        super("leap", Attribute.HORSE_JUMP_STRENGTH, 0.3, 20 * 3, 30);
    }
}
