package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * Default pathfinding NPC that mimics an assassin mercenary.
 */
public class AssassinMercenary extends AbstractMercenary {
    // MythicMobs skill names from the AwakAssassin class configuration
    private static final String SKILL_DASH = "Ravaging_Dash";
    private static final String SKILL_LETHAL = "Lethal_Combo";
    private static final String SKILL_SHADOW = "Shadowquake";
    private static final String SKILL_BLOOM = "Death_Bloom";

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
            if (!cast(npc, SKILL_DASH, 5, target, cd)) {
                npc.getNavigator().setTarget(target, true);
            }
        } else {
            cast(npc, SKILL_LETHAL, 1, target, cd);
            cast(npc, SKILL_BLOOM, 5, target, cd);
            cast(npc, SKILL_SHADOW, 8, target, cd);
        }
    }

    @Override
    public String name() {
        return "assassinmercenary";
    }

    @Override
    public String primarySkill() {
        return SKILL_LETHAL;
    }
}
