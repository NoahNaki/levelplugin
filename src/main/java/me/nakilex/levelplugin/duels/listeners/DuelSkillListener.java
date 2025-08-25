package me.nakilex.levelplugin.duels.listeners;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Collection;
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
    public void onSkillCast(Event event) {
        var name = event.getClass().getName();
        if (!name.equals("io.lumine.mythic.api.events.MythicMobSkillEvent")
                && !name.equals("io.lumine.mythic.bukkit.events.MythicMobSkillEvent")) {
            return; // Not a MythicMob skill cast
        }

        try {
            Object casterObj = event.getClass().getMethod("getCaster").invoke(event);
            var casterEntity = MythicMobModifier.toBukkitEntity(casterObj);
            if (!(casterEntity instanceof Player caster)) return;

            Object targetsObj = event.getClass().getMethod("getTargets").invoke(event);
            if (!(targetsObj instanceof Collection<?> targets)) return;

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
        } catch (ReflectiveOperationException ignored) {
            // If MythicMobs changes its API, fail silently
        }
    }
}
