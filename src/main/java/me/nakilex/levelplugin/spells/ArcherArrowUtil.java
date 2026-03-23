package me.nakilex.levelplugin.spells;

import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

/** Shared utility for spawning and configuring class-driven archer arrows. */
public final class ArcherArrowUtil {
    private static final String BASIC_ATTACK_META = "BasicAttack";

    private ArcherArrowUtil() {
    }

    public static Arrow launchClassArrow(Plugin plugin,
                                         Player caster,
                                         Vector direction,
                                         double speed,
                                         double damage) {
        if (plugin == null || caster == null || direction == null || direction.lengthSquared() <= 0.000001) {
            return null;
        }
        Vector velocity = direction.clone().normalize().multiply(Math.max(0.1, speed));
        return spawnConfiguredArrow(plugin, caster, caster.getEyeLocation(), velocity, damage);
    }

    public static Arrow spawnClassArrow(Plugin plugin,
                                        Player caster,
                                        Location spawn,
                                        Vector velocity,
                                        double damage) {
        if (plugin == null || caster == null || spawn == null || spawn.getWorld() == null || velocity == null
                || velocity.lengthSquared() <= 0.000001) {
            return null;
        }
        return spawnConfiguredArrow(plugin, caster, spawn, velocity, damage);
    }

    private static Arrow spawnConfiguredArrow(Plugin plugin,
                                              Player caster,
                                              Location spawn,
                                              Vector velocity,
                                              double damage) {
        Arrow arrow = spawn.getWorld().spawnArrow(spawn, velocity.clone().normalize(), (float) velocity.length(), 0.0f);
        arrow.setShooter(caster);
        arrow.setCritical(true);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setDamage(Math.max(0.1, damage));
        arrow.setMetadata(BASIC_ATTACK_META, new FixedMetadataValue(plugin, caster.getUniqueId()));
        return arrow;
    }
}
