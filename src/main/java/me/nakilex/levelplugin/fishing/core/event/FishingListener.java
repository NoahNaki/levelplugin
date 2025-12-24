package me.nakilex.levelplugin.fishing.core.event;

import me.nakilex.levelplugin.fishing.core.CustomFishingPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FishingListener implements Listener {
    private final CustomFishingPlugin fishing;

    public FishingListener(CustomFishingPlugin fishing) {
        this.fishing = fishing;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        switch (event.getState()) {
            case FISHING -> fishing.handleCast(event);
            case BITE -> fishing.handleBite(event);
            case CAUGHT_FISH -> fishing.handleCaught(event);
            case CAUGHT_ENTITY -> fishing.handleCaught(event);
            case FAILED_ATTEMPT, IN_GROUND -> fishing.handleAbort(event.getPlayer().getUniqueId());
            default -> {
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!fishing.isActive(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        fishing.handlePlayerAction(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        fishing.handleQuit(event.getPlayer().getUniqueId());
    }
}
