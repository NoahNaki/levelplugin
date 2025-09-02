package me.nakilex.levelplugin.horse.traits;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Knocks back nearby entities around the horse.
 */
public class KickbackTrait implements HorseTrait {
    private static final String ID = "kickback";
    private static final double RADIUS = 3.0;
    private static final double FORCE = 1.2;
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
        horse.getWorld().getNearbyEntities(horse.getLocation(), RADIUS, RADIUS, RADIUS).stream()
                .filter(e -> e instanceof LivingEntity && e != player && e != horse)
                .map(e -> (LivingEntity) e)
                .forEach(e -> {
                    Vector dir = e.getLocation().toVector().subtract(horse.getLocation().toVector()).normalize().multiply(FORCE);
                    e.setVelocity(dir.setY(0.4));
                });
    }
}
