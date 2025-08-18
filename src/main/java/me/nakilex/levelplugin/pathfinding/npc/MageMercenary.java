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
    private final Skill[] offensive = {meteor, missile, fireball};
    private int nextSkill = 0;

    public MageMercenary() {
        super(org.bukkit.Material.BLAZE_ROD, meteor, missile, fireball);
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
            if (cast(npc, frost, target, cd)) return;
            Vector awayDir = npcLoc.toVector().subtract(targetLoc.toVector()).normalize();
            Location away = npcLoc.clone().add(awayDir);
            if (cast(npc, blink, target, cd)) {
                npc.getNavigator().setTarget(away);
                return;
            }
        }

        if (!maintainRange(npc, target)) return;

        // Offensive spells rotate to use full kit
        for (int i = 0; i < offensive.length; i++) {
            int idx = (nextSkill + i) % offensive.length;
            if (cast(npc, offensive[idx], target, cd)) {
                nextSkill = (idx + 1) % offensive.length;
                break;
            }
        }
    }
}
