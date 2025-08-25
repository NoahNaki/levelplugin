package me.nakilex.levelplugin.duels.listeners;

import io.lumine.mythic.bukkit.events.MythicMobSkillEvent;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

/**
 * Prevents MythicMobs skills from affecting players that are not
 * currently duelling the caster. This ensures class abilities do not
 * push, debuff or otherwise interact with bystanders.
 */
public class DuelSkillListener implements Listener {

    private final DuelManager duels = DuelManager.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSkillCast(MythicMobSkillEvent event) {
        var casterEntity = MythicMobModifier.toBukkitEntity(event.getCaster());
        if (!(casterEntity instanceof Player caster)) return;

        var targets = event.getTargets();
        Set<Object> remove = new HashSet<>();
        for (var target : targets) {
            var bukkit = MythicMobModifier.toBukkitEntity(target);
            if (bukkit instanceof Player victim) {
                if (!duels.areInDuel(caster.getUniqueId(), victim.getUniqueId())) {
                    remove.add(target);
                }
            }
        }
        targets.removeAll(remove);
    }
}
