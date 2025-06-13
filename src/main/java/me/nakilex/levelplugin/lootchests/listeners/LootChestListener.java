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

        // Only handle our crate furniture
        if (!mech.getItemID().startsWith("crate_lvl")) {
            return;
        }

        // 2) Cancel default behavior (so the barrier block doesn’t break/open itself)
        event.setCancelled(true);

        // 3) Locate our chestId from the clicked furniture's base block
        Location loc = event.getBaseEntity().getLocation().getBlock().getLocation();
        Integer chestId = lootChestManager.getChestIdAtLocation(loc);
        if (chestId == null) {
            return; // not one of our managed chests
        }

        // Verify the mechanic ID matches the tier for this chest
        int tier = lootChestManager.getTierForChest(chestId);
        String expectedId = lootChestManager.getCrateIdForTier(tier);
        if (!mech.getItemID().equals(expectedId)) {
            return;
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
            ItemUtil.updateTooltip(stack, player);
        }
        // ─────────────────────────────────────────────────────────────────────

        // 6) Open the inventory
        player.openInventory(lootGui);

        // 7) Remember which chest this player just opened
        lootChestManager.markPlayerViewingChest(player.getUniqueId(), chestId);
    }
}
