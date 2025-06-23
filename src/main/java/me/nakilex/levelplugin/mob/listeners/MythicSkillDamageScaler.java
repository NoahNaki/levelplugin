package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.api.events.MythicDamageEvent;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Scales damage dealt by MythicMobs skills according to the caster's stats.
 * This catches MythicDamageEvent so we can boost skill damage without editing
 * the skill configs.
 */
public class MythicSkillDamageScaler implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onMythicDamage(MythicDamageEvent event) {
        if (event.getCaster() == null) return;
        if (!(event.getCaster().getEntity().getBukkitEntity() instanceof Player player)) return;

        var stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double strength = stats.baseStrength + stats.bonusStrength;
        double scaled = event.getDamage() + strength * 0.5;
        me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                "[MythicDamageScaler] base=" + event.getDamage() + " scaled=" + scaled +
                " caster=" + player.getName());
        event.setDamage(scaled);
    }
}
