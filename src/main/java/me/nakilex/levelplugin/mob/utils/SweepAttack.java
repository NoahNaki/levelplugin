package me.nakilex.levelplugin.mob.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Utility to perform a sword or shovel sweep attack damaging nearby mobs.
 */
public final class SweepAttack {
    public static final String SWEEP_META = "SweepAttack";

    private SweepAttack() {}

    /**
     * Damages nearby non-player mobs for the given amount and plays sweep effects.
     */
    public static void perform(Player player, double damage) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 1);

        double radius = 3.0;
        Entity mainTarget = player.getTargetEntity((int) radius);
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.equals(mainTarget)) continue;
            if (e instanceof Player) continue;

            player.setMetadata(SWEEP_META, new FixedMetadataValue(Main.getInstance(), true));
            le.damage(damage, player);
            player.removeMetadata(SWEEP_META, Main.getInstance());
        }
    }
}
