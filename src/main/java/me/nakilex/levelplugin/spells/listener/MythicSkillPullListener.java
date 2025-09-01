package me.nakilex.levelplugin.spells.listener;

import io.lumine.mythic.bukkit.events.MythicTargetedEntitySkillEvent;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

/**
 * Applies a pull toward the caster when a specific MythicMobs skill hits.
 * Skips pulling players who aren't dueling the caster.
 */
public class MythicSkillPullListener implements Listener {

    private final String skillName;
    private final double speed;

    /**
     * @param skillName internal name of the Mythic skill to hook into
     * @param speed     velocity multiplier applied toward the caster
     */
    public MythicSkillPullListener(String skillName, double speed) {
        this.skillName = skillName;
        this.speed = speed;
    }

    @EventHandler
    public void onSkillHit(MythicTargetedEntitySkillEvent event) {
        if (!event.getSkill().getInternalName().equalsIgnoreCase(skillName)) return;
        if (!(event.getCaster().getEntity().getBukkitEntity() instanceof Player caster)) return;
        if (!(event.getTarget().getBukkitEntity() instanceof LivingEntity target)) return;

        if (target instanceof Player targetPlayer &&
                !DuelManager.getInstance().areInDuel(caster.getUniqueId(), targetPlayer.getUniqueId())) {
            return; // don't pull players who aren't dueling the caster
        }

        Vector pull = caster.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(speed);
        pull.setY(0.2);
        target.setVelocity(pull);
    }
}

