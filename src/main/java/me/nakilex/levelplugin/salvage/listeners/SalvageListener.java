package me.nakilex.levelplugin.salvage.listeners;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.salvage.gui.SalvageGUI;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class SalvageListener implements Listener {

    private final EconomyManager economyManager;
    private final GemsManager    gemsManager;

    public SalvageListener(EconomyManager economyManager, GemsManager gemsManager) {
        this.economyManager = economyManager;
        this.gemsManager    = gemsManager;
    }

    private boolean isMerchant(InventoryView view) {
        return view != null
            && ChatColor.stripColor(view.getTitle()).equalsIgnoreCase("Merchant");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isMerchant(event.getView())) return;

        Inventory topInv = event.getView().getTopInventory();
        int slot = event.getRawSlot();

        // SHIFT‑click protection for non‑custom items
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            CustomItem cItem = ItemManager.getInstance()
                .getCustomItemFromItemStack(event.getCurrentItem());
            if (cItem == null) {
                event.setCancelled(true);
            }
            return;
        }

        // Clicking inside the salvage GUI
        if (event.getClickedInventory() != null
            && event.getClickedInventory().equals(topInv)) {

            // Sell‑button
            if (slot == SalvageGUI.SELL_SLOT) {
                event.setCancelled(true);
                handleSellButtonClick(event);
            }
            // Only allow placing CustomItems into slots 0–25
            else if (slot >= 0 && slot < SalvageGUI.SELL_SLOT) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    CustomItem cItem = ItemManager.getInstance()
                        .getCustomItemFromItemStack(cursor);
                    if (cItem == null) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isMerchant(event.getView())) return;

        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < SalvageGUI.SELL_SLOT) {
                ItemStack dragged = event.getOldCursor();
                if (dragged != null && dragged.getType() != Material.AIR) {
                    CustomItem cItem = ItemManager.getInstance()
                        .getCustomItemFromItemStack(dragged);
                    if (cItem == null) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Prevent any ground‑pickup while the salvage GUI is open.
     */
    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (isMerchant(player.getOpenInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isMerchant(event.getView())) return;

        Player player = (Player) event.getPlayer();
        Inventory topInv = event.getView().getTopInventory();

        // Return any leftover items in slots 0–25
        for (int i = 0; i < SalvageGUI.SELL_SLOT; i++) {
            ItemStack leftover = topInv.getItem(i);
            if (leftover != null && leftover.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> overflow =
                    player.getInventory().addItem(leftover);
                // drop anything that still wouldn't fit
                overflow.values().forEach(drop ->
                    player.getWorld().dropItemNaturally(player.getLocation(), drop)
                );
                topInv.setItem(i, null);
            }
        }
    }

    private void handleSellButtonClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv  = event.getInventory();

        int totalCoins = 0, totalGems = 0;
        for (int i = 0; i < SalvageGUI.SELL_SLOT; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            CustomItem cItem = ItemManager.getInstance()
                .getCustomItemFromItemStack(item);
            if (cItem != null) {
                totalCoins += SalvageManager.getInstance().getSellPrice(cItem);
                totalGems  += SalvageManager.getInstance().getGemReward(cItem);
            }
            inv.setItem(i, null);
        }

        economyManager.addCoins(player, totalCoins);
        if (totalGems > 0) gemsManager.addUnits(player, totalGems);

        if (totalCoins > 0 || totalGems > 0) {
            StringBuilder msg = new StringBuilder(ChatColor.GREEN + "You received ");
            if (totalCoins > 0) {
                msg.append(totalCoins).append(" coins");
                if (totalGems > 0) msg.append(" and ");
            }
            if (totalGems > 0) msg.append(totalGems).append(" gems");
            msg.append("!");
            player.sendMessage(msg.toString());
        } else {
            player.sendMessage(ChatColor.YELLOW + "No valid items to salvage.");
        }
    }
}
