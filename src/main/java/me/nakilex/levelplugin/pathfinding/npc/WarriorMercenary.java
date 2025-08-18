package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/** Simple melee warrior using Awakened Warrior's Brutal Combo. */
public class WarriorMercenary extends AbstractMercenary {
    private static final String SKILL_SLASH = "Brutal_Combo";

    @Override
    protected ItemStack weapon() {
        return new ItemStack(Material.NETHERITE_SWORD);
    }

    @Override
    public void handleCombat(NPC npc, LivingEntity target, CooldownManager cd) {
        Location npcLoc = npc.getEntity().getLocation();
        Location targetLoc = target.getLocation();
        npc.faceLocation(targetLoc);
        double distSq = npcLoc.distanceSquared(targetLoc);
        if (distSq > 9) {
            npc.getNavigator().setTarget(targetLoc);
        } else {
            cast(npc, SKILL_SLASH, 1, target, cd);
        }
    }

    @Override
    public String name() {
        return "warriormercenary";
    }

    @Override
    public String primarySkill() {
        return SKILL_SLASH;
    }
}
