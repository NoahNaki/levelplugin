package me.nakilex.levelplugin.horse.traits;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

/**
 * Temporarily boosts the horse's movement speed.
 */
public class DashTrait implements HorseTrait {
    private static final String ID = "dash";
    private static final double SPEED_BONUS = 0.25; // added to base speed
    private static final int DURATION_TICKS = 20 * 3; // 3 seconds
    private static final int COOLDOWN_SECONDS = 30;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getCooldownSeconds() {
        return COOLDOWN_SECONDS;
    }

    @Override
    public void apply(Player player, AbstractHorse horse) {
        AttributeInstance attr = horse.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        double original = attr.getBaseValue();
        attr.setBaseValue(original + SPEED_BONUS);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> attr.setBaseValue(original), DURATION_TICKS);
    }
}
