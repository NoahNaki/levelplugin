package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fakeblock.FakeBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/** Prevents stage fake blocks from disappearing when interacted with. */
public class StageBlockInteractListener implements Listener {
    private final FakeBlockManager fakeBlockManager;

    public StageBlockInteractListener(FakeBlockManager fakeBlockManager) {
        this.fakeBlockManager = fakeBlockManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();
        if (!fakeBlockManager.isFakeBlock(player, loc)) return;

        event.setCancelled(true);
        BlockData data = fakeBlockManager.getFakeBlock(player, loc);
        if (data != null) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                    () -> {
                        if (player.isOnline()) {
                            player.sendBlockChange(loc, data);
                        }
                    }, 1L);
        }
    }
}
