package me.nakilex.levelplugin.mob.utils;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Utility helpers for resolving Mythic event participants using reflection-friendly fallbacks. */
public final class MythicEventUtil {

    private MythicEventUtil() {}

    public static Player resolvePlayer(Object casterObj) {
        var entity = MythicMobModifier.toBukkitEntity(casterObj);
        return entity instanceof Player p ? p : null;
    }

    /**
     * Attempts to resolve the player responsible for a Mythic damage event using caster,
     * trigger, and shooter fallbacks.
     */
    public static Player resolvePlayer(MythicDamageEvent event) {
        Player player = resolvePlayer(event.getCaster());

        if (player == null) {
            try {
                Object trigger = event.getClass().getMethod("getTrigger").invoke(event);
                if (trigger != null) {
                    player = resolvePlayer(trigger);

                    if (player == null) {
                        Object trigCaster = trigger.getClass().getMethod("getCaster").invoke(trigger);
                        player = resolvePlayer(trigCaster);
                    }
                }
            } catch (Exception ignore) {
            }
        }

        if (player == null) {
            try {
                Object shooter = event.getClass().getMethod("getShooter").invoke(event);
                if (shooter instanceof Player p) player = p;
            } catch (Exception ignore) {
            }
        }

        return player;
    }

    /**
     * Attempts to resolve the Mythic damage target using common accessor names.
     */
    public static LivingEntity resolveTarget(MythicDamageEvent event) {
        LivingEntity target = null;
        try {
            Object maybe = event.getClass().getMethod("getEntity").invoke(event);
            if (maybe instanceof LivingEntity le) target = le;
        } catch (Exception ignore) {
        }

        if (target == null) {
            try {
                Object maybe = event.getClass().getMethod("getTarget").invoke(event);
                if (maybe instanceof LivingEntity le) target = le;
            } catch (Exception ignore) {
            }
        }

        return target;
    }

    /**
     * Attempts to resolve a Bukkit damager (if any) so listeners can avoid double-processing
     * events that will already be handled by EntityDamageByEntityEvent.
     */
    public static Entity resolveDamager(MythicDamageEvent event) {
        try {
            Object maybe = event.getClass().getMethod("getDamager").invoke(event);
            if (maybe instanceof Entity e) return e;
        } catch (Exception ignore) {
        }

        try {
            Object maybe = event.getClass().getMethod("getSource").invoke(event);
            if (maybe instanceof Entity e) return e;
        } catch (Exception ignore) {
        }

        try {
            Object maybe = event.getClass().getMethod("getAttacker").invoke(event);
            if (maybe instanceof Entity e) return e;
        } catch (Exception ignore) {
        }

        return null;
    }
}
