package me.nakilex.levelplugin.duels.listeners;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.EventExecutor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Filters MythicMobs skill targets so that only active duel opponents
 * can be affected by player skills. This prevents abilities from
 * interacting with bystanders.
 */
public class DuelSkillListener implements Listener {

    private final DuelManager duels = DuelManager.getInstance();
    private final Plugin plugin;

    public DuelSkillListener(Plugin plugin) {
        this.plugin = plugin;
        register(plugin, "io.lumine.mythic.api.events.MythicMobSkillEvent");
        register(plugin, "io.lumine.mythic.bukkit.events.MythicMobSkillEvent");
    }

    private void register(Plugin plugin, String className) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> clazz = (Class<? extends Event>) Class.forName(className);
            EventExecutor executor = (listener, event) -> handle(event);
            Bukkit.getPluginManager().registerEvent(clazz, this, EventPriority.HIGHEST, executor, plugin, true);
        } catch (ClassNotFoundException ignored) {
            // MythicMobs not installed or API absent
        }
    }

    private void handle(Event event) {
        try {
            Object casterObj = event.getClass().getMethod("getCaster").invoke(event);
            var casterEntity = MythicMobModifier.toBukkitEntity(casterObj);
            if (!(casterEntity instanceof Player caster)) return;

            Object nameObj = event.getClass().getMethod("getSkillName").invoke(event);
            String skillName = nameObj != null ? nameObj.toString() : "unknown";
            Bukkit.getLogger().info("[DuelSkillDebug] " + caster.getName() + " cast skill " + skillName);

            // Mark caster so upcoming projectiles can be identified
            caster.setMetadata(ProjectileFriendlyFireListener.MYTHIC_META, new FixedMetadataValue(plugin, skillName));
            Bukkit.getScheduler().runTask(plugin,
                    () -> caster.removeMetadata(ProjectileFriendlyFireListener.MYTHIC_META, plugin));

            Object targetsObj = event.getClass().getMethod("getTargets").invoke(event);
            if (!(targetsObj instanceof Collection<?> targets)) return;

            Set<Object> remove = new HashSet<>();
            for (var target : targets) {
                var bukkit = MythicMobModifier.toBukkitEntity(target);
                if (bukkit instanceof Player victim
                        && !duels.areInDuel(caster.getUniqueId(), victim.getUniqueId())) {
                    remove.add(target);
                    Bukkit.getLogger().info("[DuelSkillDebug] Removed bystander " + victim.getName() + " from skill " + skillName);
                }
            }
            targets.removeAll(remove);
        } catch (ReflectiveOperationException ignored) {
            // If MythicMobs changes its API, fail silently
        }
    }
}
