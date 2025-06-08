package me.nakilex.levelplugin.lootchests.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class LootChestListener implements Listener {

    private final LootChestManager lootChestManager;

    public LootChestListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    @EventHandler
    public void onFurnitureInteract(NexoFurnitureInteractEvent event) {
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

        // 4) Build the custom loot GUI
        Player player = event.getPlayer();
        Inventory lootGui = lootChestManager.buildLootInventory(chestId, player);

        // ─────────────────────────────────────────────────────────────────────
        // 5) Update each ItemStack’s tooltip (lore) before the player sees it
        for (int slot = 0; slot < lootGui.getSize(); slot++) {
            ItemStack stack = lootGui.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;

            // This mutates the ItemStack’s lore in place based on the player’s stats:
            ItemUtil.updateCustomItemTooltip(stack, player);
        }
        // ─────────────────────────────────────────────────────────────────────

        // 6) Open the inventory
        player.openInventory(lootGui);

        // 7) Remember which chest this player just opened
        lootChestManager.markPlayerViewingChest(player.getUniqueId(), chestId);
    }
}
