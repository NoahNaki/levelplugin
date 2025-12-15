package me.nakilex.levelplugin.mob.utils;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/** Utility helpers for resolving Mythic event participants using reflection-friendly fallbacks. */
public final class MythicEventUtil {

    private static final NamespacedKey MYTHIC_OWNER_KEY = new NamespacedKey("mythicmobs", "owner");

    private MythicEventUtil() {}

    public static Player resolvePlayer(Object casterObj) {
        var entity = MythicMobModifier.toBukkitEntity(casterObj);
        if (entity instanceof Player p) return p;

        return resolveOwnerPlayer(entity);
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
     * Mythic projectiles/anchors often carry the owning player's UUID in PDC under mythicmobs:owner.
     * This attempts to resolve that owner to a Bukkit player.
     */
    public static Player resolveOwnerPlayer(Entity entity) {
        if (entity == null) return null;

        try {
            var pdc = entity.getPersistentDataContainer();
            String ownerId = pdc.get(MYTHIC_OWNER_KEY, PersistentDataType.STRING);
            if (ownerId != null) {
                try {
                    return Bukkit.getPlayer(java.util.UUID.fromString(ownerId));
                } catch (IllegalArgumentException ignored) {
                    // fall through if the stored value isn't a UUID
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /** Returns true if the entity declares a MythicMobs owner in PDC. */
    public static boolean hasMythicOwner(Entity entity) {
        if (entity == null) return false;
        try {
            return entity.getPersistentDataContainer()
                    .has(MYTHIC_OWNER_KEY, PersistentDataType.STRING);
        } catch (Exception ignored) {
            return false;
        }
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
