package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class LootChestCloseListener implements Listener {

    private final LootChestManager lootChestManager;

    public LootChestCloseListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Chest)) return;

        Chest chest = (Chest) holder;
        Block block = chest.getBlock();
        if (block.getType() != Material.CHEST) return;

        Integer chestId = lootChestManager.getChestIdAtLocation(block.getLocation());
        if (chestId != null) {
            lootChestManager.getPlugin().getLogger().info("[LootChestCloseListener] Chest " + chestId +
                " was closed. Removing chest & starting cooldown.");

            lootChestManager.removeChest(chestId);
            lootChestManager.getCooldownManager().startChestCooldown(chestId);

        } else {
            lootChestManager.getPlugin().getLogger().info("[LootChestCloseListener] InventoryCloseEvent for a chest block, " +
                "but it's not in spawnedChests map.");
        }
    }
}
