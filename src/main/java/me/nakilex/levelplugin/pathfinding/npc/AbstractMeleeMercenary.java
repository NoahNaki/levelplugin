package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;

/** Base implementation for melee mercenaries with multiple skills. */
public abstract class AbstractMeleeMercenary extends AbstractMercenary {
    private final Material weapon;
    private final Skill[] skills;

    protected AbstractMeleeMercenary(Material weapon, Skill... skills) {
        this.weapon = weapon;
        this.skills = skills;
    }

    @Override
    protected ItemStack weapon() {
        return new ItemStack(weapon);
    }

    @Override
    public String primarySkill() {
        return skills.length > 0 ? skills[0].name() : "";
    }

    @Override
    public void handleCombat(NPC npc, LivingEntity target, CooldownManager cd) {
        Location npcLoc = npc.getEntity().getLocation();
        Location targetLoc = target.getEyeLocation();
        npc.faceLocation(targetLoc);
        double distSq = npcLoc.distanceSquared(targetLoc);
        if (distSq > 9) {
            npc.getNavigator().setTarget(targetLoc);
        } else {
            for (Skill s : skills) {
                if (cast(npc, s, target, cd)) {
                    break;
                }
            }
        }
    }
}
