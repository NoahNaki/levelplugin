package me.nakilex.levelplugin.spells.listener;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

/**
 * Applies a pull toward the caster when a specific MythicMobs skill hits.
 * Skips pulling players who aren't dueling the caster.
 */
public class MythicSkillPullListener implements Listener {

    private final String skillName;
    private final double speed;
    private final boolean debug = Main.getInstance()
            .getCustomConfig().getBoolean("debug.mythic-skill-pull", false);

    /**
     * @param skillName internal name of the Mythic skill to hook into
     * @param speed     velocity multiplier applied toward the caster
     */
    public MythicSkillPullListener(String skillName, double speed) {
        this.skillName = skillName;
        this.speed = speed;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillDamage(MythicDamageEvent event) {
        debug("Damage event from MythicMobs detected");
        String internal = extractSkillName(event);
        if (internal == null) {
            debug("Unknown skill for MythicDamageEvent");
            return;
        }
        if (!internal.equalsIgnoreCase(skillName)) {
            debug("Skipping skill " + internal);
            return;
        }

        Entity casterEnt = MythicMobModifier.toBukkitEntity(event.getCaster());
        if (!(casterEnt instanceof Player caster)) {
            debug("Caster not player: " + casterEnt);
            return;
        }

        Entity targetEnt = extractTarget(event);
        if (!(targetEnt instanceof LivingEntity target)) {
            debug("No living target resolved");
            return;
        }

        if (target instanceof Player victim &&
                !DuelManager.getInstance().areInDuel(caster.getUniqueId(), victim.getUniqueId())) {
            debug("Players not in duel: " + caster.getName() + " -> " + victim.getName());
            return; // don't pull players who aren't dueling the caster
        }

        Vector pull = caster.getLocation().toVector()
                .subtract(target.getLocation().toVector())
                .normalize().multiply(speed);
        pull.setY(0.2);
        target.setVelocity(pull);
        debug("Pulled " + target.getName() + " toward " + caster.getName());
    }

    private String extractSkillName(MythicDamageEvent event) {
        // Newer MythicMobs versions expose the internal name directly.
        try {
            return (String) event.getClass().getMethod("getSkillName").invoke(event);
        } catch (Exception ignored) {
        }
        // Older versions provide metadata with the skill instance.
        try {
            Object meta = event.getClass().getMethod("getMetadata").invoke(event);
            if (meta != null) {
                Object skill = meta.getClass().getMethod("getSkill").invoke(meta);
                if (skill != null) {
                    return (String) skill.getClass().getMethod("getInternalName").invoke(skill);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Entity extractTarget(MythicDamageEvent event) {
        try {
            Object raw = event.getClass().getMethod("getEntity").invoke(event);
            Entity resolved = MythicMobModifier.toBukkitEntity(raw);
            if (resolved != null) return resolved;
        } catch (Exception ignored) {
        }
        try {
            Object raw = event.getClass().getMethod("getTarget").invoke(event);
            Entity resolved = MythicMobModifier.toBukkitEntity(raw);
            if (resolved != null) return resolved;
        } catch (Exception ignored) {
        }
        try {
            Object raw = event.getClass().getMethod("getDamagee").invoke(event);
            Entity resolved = MythicMobModifier.toBukkitEntity(raw);
            if (resolved != null) return resolved;
        } catch (Exception ignored) {
        }
        return null;
    }

    private void debug(String msg) {
        if (debug) Main.getInstance().getLogger().info("[MythicSkillPull] " + msg);
    }
}

