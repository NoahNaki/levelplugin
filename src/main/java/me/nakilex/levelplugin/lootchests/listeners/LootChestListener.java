package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class LootChestListener implements Listener {

    private final LootChestManager lootChestManager;

    public LootChestListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    // (you already have your PlayerInteractEvent here…)

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        // 1) Only care about chest GUIs
        if (!(event.getInventory().getHolder() instanceof Chest)) return;

        Chest chest = (Chest) event.getInventory().getHolder();
        Location loc = chest.getLocation();

        // 2) Is this one of your loot chests?
        Integer chestId = lootChestManager.getChestIdAtLocation(loc);
        if (chestId == null) return;

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        // 3) Iterate, detect custom items, re‐lore them
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || !stack.hasItemMeta()) continue;

            if (!stack.getItemMeta()
                .getPersistentDataContainer()
                .has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING)) {
                continue;
            }

            // 4) Rebuild the lore with checks/X’s for this player
            ItemUtil.updateCustomItemTooltip(stack, player);
            inv.setItem(slot, stack);
        }

        // 5) Force a client refresh
        player.updateInventory();
    }
}
