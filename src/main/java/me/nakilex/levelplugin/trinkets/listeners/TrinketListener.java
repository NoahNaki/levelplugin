package me.nakilex.levelplugin.trinkets.listeners;

import me.nakilex.levelplugin.trinkets.managers.TrinketManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles user interaction for trinket activation and cleanup.
 */
public class TrinketListener implements Listener {

    private final TrinketManager manager;

    public TrinketListener(TrinketManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack off = event.getOffHandItem();
        ItemStack main = event.getMainHandItem();
        boolean offTrinket = manager.isTrinket(off);
        boolean mainTrinket = manager.isTrinket(main);
        if (!offTrinket && !mainTrinket) {
            return;
        }
        event.setCancelled(true);
        if (offTrinket) {
            manager.trigger(player, off);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (manager.shouldCancelDamage(player)) {
            event.setCancelled(true);
            event.setDamage(0.0);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.clear(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        manager.clear(event.getEntity());
    }
}
