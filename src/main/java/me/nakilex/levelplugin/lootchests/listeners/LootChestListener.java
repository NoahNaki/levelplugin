package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class LootChestListener implements Listener {

    private final LootChestManager lootChestManager;

    public LootChestListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    /**
     * Called when a player right-clicks a block.
     * We check if it's one of our loot chests, then handle opening.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // We only care about RIGHT_CLICK_BLOCK actions
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        // Check if the player clicked a chest
        if (event.getClickedBlock().getType() == Material.CHEST) {
            Location clickedLocation = event.getClickedBlock().getLocation();

            // Check if this location matches one of our loot chest IDs
            Integer chestId = lootChestManager.getChestIdAtLocation(clickedLocation);
            if (chestId != null) {
                // Optional: Cancel the event so the normal chest GUI doesn't open
                event.setCancelled(true);

                // Open the chest in our plugin's sense (remove it, start cooldown, etc.)
                UUID playerUuid = event.getPlayer().getUniqueId();
                lootChestManager.openChest(chestId, playerUuid);

                // (Optional) If you want to give the player loot, you can do it here or in lootChestManager.openChest(...)
                // e.g. giveTierLoot(event.getPlayer(), lootChestManager.getChestData(chestId).getTier());
            }
        }
    }
}
