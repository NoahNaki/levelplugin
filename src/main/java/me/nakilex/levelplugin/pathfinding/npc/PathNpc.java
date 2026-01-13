package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.utils.cooldowns.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Behavior contract for pathfinding NPC combat profiles.
 * Implementations can equip gear, define speed multipliers,
 * and handle combat ticks with custom spell logic.
 */
public interface PathNpc {
    /** Represents a combat skill and its cooldown. */
    record Skill(String name, double cooldown, SkillAction action) {
        Skill(String name, double cooldown) {
            this(name, cooldown, SkillAction.noop());
        }
    }

    @FunctionalInterface
    interface SkillAction {
        boolean cast(NPC npc, LivingEntity target);

        static SkillAction noop() {
            return (npc, target) -> false;
        }
    }

    /** Multiplier applied to the NPC's base walking speed. */
    float speedMultiplier();

    /** Equip any weapons or armor on the NPC after spawning. */
    void equip(NPC npc);

    /** Called each tick while the NPC has a combat target. */
    void handleCombat(NPC npc, LivingEntity target, CooldownManager cooldowns);

    /** Name used when spawning the NPC. */
    default String name() {
        return "PathNPC";
    }

    /**
     * Primary skill this profile tries to cast. Used only for debug output so
     * server logs can easily confirm which ability is expected.
     */
    default String primarySkill() {
        return "";
    }

    /**
     * Entity type used for the NPC. Mercenaries default to player entities to
     * keep a player-like silhouette rather than native mob AI.
     */
    default EntityType type() {
        return EntityType.PLAYER;
    }

    /**
     * Utility to cast a skill if its cooldown has expired.
     */
    default boolean cast(NPC npc, Skill skill, LivingEntity target, CooldownManager cooldowns) {
        if (skill == null || target == null) {
            return false;
        }
        UUID id = npc.getEntity().getUniqueId();
        String skillName = skill.name();
        if (cooldowns.isOnCooldown(id, skillName)) {
            Bukkit.getLogger().info("[MercenaryDebug] " + skillName + " on cooldown for NPC " + id);
            return false;
        }
        Bukkit.getLogger().info("[MercenaryDebug] Attempting to cast '" + skillName + "' at " + target.getName());
        boolean success = skill.action() != null && skill.action().cast(npc, target);
        if (!success) {
            Bukkit.getLogger().warning("[MercenaryDebug] No handler for skill '" + skillName + "'");
            return false;
        }
        cooldowns.setCooldown(id, skillName, skill.cooldown());
        Bukkit.getLogger().info("[MercenaryDebug] Cast '" + skillName + "' successfully");
        return true;
    }
}
