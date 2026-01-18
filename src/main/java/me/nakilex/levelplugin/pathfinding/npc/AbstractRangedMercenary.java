package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.utils.cooldowns.CooldownManager;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;

/**
 * Base implementation for ranged mercenaries that keep 8-10 blocks away
 * and fire a single skill as their basic attack.
 */
public abstract class AbstractRangedMercenary extends AbstractMercenary {
    private final Material weapon;
    private final Skill[] skills;
    private int nextSkill = 0;

    protected AbstractRangedMercenary(Material weapon, Skill... skills) {
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
        if (!maintainRange(npc, target)) return;
        castNextSkill(npc, target, cd);
    }

    protected void castNextSkill(NPC npc, LivingEntity target, CooldownManager cd) {
        for (int i = 0; i < skills.length; i++) {
            int idx = (nextSkill + i) % skills.length;
            if (cast(npc, skills[idx], target, cd)) {
                nextSkill = (idx + 1) % skills.length;
                break;
            }
        }
    }

    /**
     * Faces the target and keeps roughly 8–10 blocks of distance.
     *
     * @return true if within desired range and ready to attack
     */
    protected boolean maintainRange(NPC npc, LivingEntity target) {
        Location npcLoc = npc.getEntity().getLocation();
        Location targetLoc = target.getEyeLocation();
        double distSq = npcLoc.distanceSquared(targetLoc);
        if (distSq > 100) { // >10 blocks, chase target entity
            npc.getNavigator().setTarget(target, true);
            return false;
        }
        if (distSq < 64) { // <8 blocks, back off to ~8
            Vector dir = npcLoc.toVector().subtract(targetLoc.toVector()).normalize().multiply(8);
            Location away = targetLoc.clone().add(dir);
            npc.getNavigator().setTarget(away);
            return false;
        }
        return true;
    }
}
