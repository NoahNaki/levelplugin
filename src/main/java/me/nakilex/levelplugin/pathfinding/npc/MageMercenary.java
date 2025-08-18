package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/** Ranged mage profile wielding multiple Awakened Mage spells. */
public class MageMercenary extends AbstractRangedMercenary {
    private final Skill meteor = new Skill("Meteor", 8);
    private final Skill frost = new Skill("Frost_Nova", 5);
    private final Skill fireball = new Skill("Fireball", 1);
    private final Skill missile = new Skill("Arcane_Missile", 2);
    private final Skill blink = new Skill("Blink", 6);

    public MageMercenary() {
        super(org.bukkit.Material.BLAZE_ROD, new Skill("Fireball", 1));
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
        npc.faceLocation(targetLoc);

        // If too close, try Frost Nova then blink away
        if (distSq < 25) {
            if (cast(npc, frost, target, cd)) return;
            Vector awayDir = npcLoc.toVector().subtract(targetLoc.toVector()).normalize();
            Location away = npcLoc.clone().add(awayDir);
            npc.faceLocation(away);
            if (cast(npc, blink, target, cd)) {
                npc.faceLocation(targetLoc);
                return;
            }
        }

        // Maintain distance 8-10 blocks
        if (distSq > 100) {
            npc.getNavigator().setTarget(targetLoc);
            return;
        } else if (distSq < 64) {
            Vector dir = npcLoc.toVector().subtract(targetLoc.toVector()).normalize().multiply(8);
            npc.getNavigator().setTarget(targetLoc.clone().add(dir));
        }

        // Offensive spells
        if (cast(npc, meteor, target, cd)) return;
        if (cast(npc, missile, target, cd)) return;
        if (cast(npc, fireball, target, cd)) return;
    }
}
