package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.fakeblock.FakeBlockManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.Event.Result;

/** Prevents stage fake blocks from disappearing when interacted with. */
public class StageBlockInteractListener implements Listener {
    private final FakeBlockManager fakeBlockManager;

    public StageBlockInteractListener(FakeBlockManager fakeBlockManager) {
        this.fakeBlockManager = fakeBlockManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Location loc = null;
        if (event.getClickedBlock() != null) {
            loc = event.getClickedBlock().getLocation();
        } else {
            var target = player.getTargetBlockExact(5);
            if (target != null) loc = target.getLocation();
        }
        if (loc == null) return;
        if (!fakeBlockManager.isFakeBlock(player, loc)) return;

        event.setUseInteractedBlock(Result.DENY);
        event.setUseItemInHand(Result.DENY);
        event.setCancelled(true);
    }
}
