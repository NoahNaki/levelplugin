package me.nakilex.levelplugin.pathfinding.npc;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Behavior contract for pathfinding NPC combat profiles.
 * Implementations can equip gear, define speed multipliers,
 * and handle combat ticks with custom spell logic.
 */
public interface PathNpc {
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
     * Utility to cast a MythicMobs skill if its cooldown has expired.
     */
    default boolean cast(NPC npc, String skill, double cooldownSeconds,
                         LivingEntity target, CooldownManager cooldowns) {
        UUID id = npc.getEntity().getUniqueId();
        if (cooldowns.isOnCooldown(id, skill)) {
            return false;
        }
        MythicBukkit.inst().getAPIHelper()
                .castSkill(npc.getEntity(), skill, target.getLocation());
        cooldowns.setCooldown(id, skill, cooldownSeconds);
        return true;
    }
}

