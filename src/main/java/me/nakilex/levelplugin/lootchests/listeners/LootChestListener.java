package me.nakilex.levelplugin.lootchests.listeners;

import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class LootChestListener implements Listener {

    private final LootChestManager lootChestManager;

    public LootChestListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    @EventHandler
    public void onFurnitureInteract(OraxenFurnitureInteractEvent event) {
        // 1) Which furniture did the player click?
        FurnitureMechanic mech = event.getMechanic();
        if (!"crate_lvl1".equals(mech.getItemID())) {
            return; // not our crate, ignore
        }

        // 2) Cancel default behavior (so the barrier block doesn’t break/open itself)
        event.setCancelled(true);

        // 3) Locate our chestId from the clicked block’s location
        Location loc = event.getBlock().getLocation();
        Integer chestId = lootChestManager.getChestIdAtLocation(loc);
        if (chestId == null) {
            return; // not one of our managed chests
        }

        // 4) Open the custom loot GUI
        Player player = event.getPlayer();
        Inventory lootGui = lootChestManager.buildLootInventory(chestId, player);
        player.openInventory(lootGui);
// NEW: remember which chest this player just opened
        lootChestManager.markPlayerViewingChest(player.getUniqueId(), chestId);

    }
}
