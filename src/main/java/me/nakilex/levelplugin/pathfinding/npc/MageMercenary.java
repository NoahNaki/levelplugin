package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;
import me.nakilex.levelplugin.utils.cooldowns.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/** Ranged mage profile wielding multiple Awakened Mage spells. */
public class MageMercenary extends AbstractRangedMercenary {
    private static final Skill METEOR = new Skill("Meteor", 8);
    private static final Skill FROST = new Skill("Frost_Nova", 5);
    private static final Skill FIREBALL = new Skill("Fireball", 1);
    private static final Skill MISSILE = new Skill("Arcane_Missile", 2);
    private static final Skill BLINK = new Skill("Blink", 6);

    public MageMercenary() {
        super(org.bukkit.Material.BLAZE_ROD, METEOR, MISSILE, FIREBALL);
    }

    @Override
    public String name() {
        return "magemercenary";
    }


    @Override
    public void handleCombat(NPC npc, LivingEntity target, CooldownManager cd) {
        Location npcLoc = npc.getEntity().getLocation();
        Location targetLoc = target.getEyeLocation();
        double distSq = npcLoc.distanceSquared(targetLoc);

        // If too close, try Frost Nova then blink away
        if (distSq < 25) {
            if (cast(npc, FROST, target, cd)) return;
            Vector awayDir = npcLoc.toVector().subtract(targetLoc.toVector()).normalize();
            Location away = npcLoc.clone().add(awayDir);
            if (cast(npc, BLINK, target, cd)) {
                npc.getNavigator().setTarget(away);
                return;
            }
        }

        if (!maintainRange(npc, target)) return;

        // Offensive spells rotate automatically via superclass helper
        castNextSkill(npc, target, cd);
    }
}
