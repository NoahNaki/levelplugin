package me.nakilex.levelplugin.animatedlb;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.MetadataValue;

/** Advances a board's page when its footer hitbox is left-clicked. */
public class AnimatedLeaderboardClickListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!entity.hasMetadata(AnimatedLeaderboard.NEXT_PAGE_META)) {
            return;
        }
        event.setCancelled(true);
        for (MetadataValue value : entity.getMetadata(AnimatedLeaderboard.NEXT_PAGE_META)) {
            if (value.value() instanceof AnimatedLeaderboard board) {
                board.next();
                return;
            }
        }
    }
}
