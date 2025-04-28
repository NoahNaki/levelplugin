package me.nakilex.levelplugin.salvage.listeners;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SalvageListener implements Listener {

    private final EconomyManager economyManager;
    private final GemsManager    gemsManager;

    public SalvageListener(EconomyManager economyManager, GemsManager gemsManager) {
        this.economyManager = economyManager;
        this.gemsManager    = gemsManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || !isMerchantInventory(event.getView().getTitle())) {
            return;
        }

        if (event.isShiftClick()) {
            if (event.getCurrentItem() != null) {
                CustomItem cItem = ItemManager.getInstance()
                    .getCustomItemFromItemStack(event.getCurrentItem());
                if (cItem == null) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        if (event.getClickedInventory() != null
            && event.getClickedInventory().equals(topInv)) {

            int slot = event.getRawSlot();

            if (slot == 8) {
                event.setCancelled(true);
                handleSellButtonClick(event);
            } else if (slot >= 0 && slot < 8) {
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
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || !isMerchantInventory(event.getView().getTitle())) {
            return;
        }

        for (int slot : event.getRawSlots()) {
            if (slot < topInv.getSize() && slot >= 0 && slot < 8) {
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

    private boolean isMerchantInventory(String title) {
        return ChatColor.stripColor(title).equalsIgnoreCase("Merchant");
    }

    private void handleSellButtonClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv  = event.getInventory();

        int totalCoins = 0;
        int totalGems  = 0;

        for (int i = 0; i < 8; i++) {
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
        if (totalGems > 0) {
            gemsManager.addUnits(player, totalGems);
        }

        if (totalCoins > 0 || totalGems > 0) {
            StringBuilder msg = new StringBuilder(ChatColor.GREEN + "You received ");
            if (totalCoins > 0) {
                msg.append(totalCoins).append(" coins");
                if (totalGems > 0) msg.append(" and ");
            }
            if (totalGems > 0) {
                msg.append(totalGems).append(" gems");
            }
            msg.append("!");
            player.sendMessage(msg.toString());
        } else {
            player.sendMessage(ChatColor.YELLOW + "No valid items to salvage.");
        }
    }
}
