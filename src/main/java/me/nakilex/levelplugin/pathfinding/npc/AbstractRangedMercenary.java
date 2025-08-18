package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Base implementation for ranged mercenaries that keep 8-10 blocks away
 * and fire a single MythicMobs skill as their basic attack.
 */
public abstract class AbstractRangedMercenary extends AbstractMercenary {
    private final Material weapon;
    private final String skill;

    protected AbstractRangedMercenary(Material weapon, String skill) {
        this.weapon = weapon;
        this.skill = skill;
    }

    @Override
    protected ItemStack weapon() {
        return new ItemStack(weapon);
    }

    @Override
    public void handleCombat(NPC npc, LivingEntity target, CooldownManager cd) {
        Location npcLoc = npc.getEntity().getLocation();
        Location targetLoc = target.getLocation();
        npc.faceLocation(targetLoc);
        double distSq = npcLoc.distanceSquared(targetLoc);
        if (distSq > 100) { // too far, move closer
            npc.getNavigator().setTarget(targetLoc);
        } else if (distSq < 64) { // too close, back off to roughly 8 blocks
            Vector dir = npcLoc.toVector().subtract(targetLoc.toVector()).normalize().multiply(8);
            Location away = targetLoc.clone().add(dir);
            npc.getNavigator().setTarget(away);
        } else {
            cast(npc, skill, 1, target, cd);
        }
    }
}
