package me.nakilex.levelplugin.salvage.listeners;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.potions.data.PotionInstance;
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
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class SalvageListener implements Listener {

    private final EconomyManager economyManager;
    private final GemsManager gemsManager;

    public SalvageListener(EconomyManager economyManager, GemsManager gemsManager) {
        this.economyManager = economyManager;
        this.gemsManager = gemsManager;
    }

    private boolean isMerchant(InventoryView view) {
        return view != null && ChatColor.stripColor(view.getTitle()).equalsIgnoreCase("Salvage Items");
    }

    private boolean isInputSlot(int slot) {
        return slot >= 0 && slot < 54 && !(slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8);
    }

    private boolean isSalvageable(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        if (cItem != null) return true;
        return Main.getInstance()
            .getPotionManager()
            .getInstanceFromItem(stack) != null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isMerchant(event.getView())) return;

        Inventory topInv = event.getView().getTopInventory();
        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (event.isShiftClick() && event.getCurrentItem() != null) {
            if (!isSalvageable(event.getCurrentItem())) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInv)) {
            if (slot == 53) {
                event.setCancelled(true);
                handleSellButtonClick(event);
                return;
            }
            if (slot == 45) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
            if (slot >= 47 && slot <= 51) {
                event.setCancelled(true);
                handleQuickSellClick(player, slot);
                return;
            }
            if (slot == 52) {
                event.setCancelled(true);
                depositAllItems(player, topInv);
                return;
            }
            if (slot == 46) {
                event.setCancelled(true);
                returnAllItems(player, topInv);
                return;
            }
            if (!isInputSlot(slot)) {
                event.setCancelled(true);
                return;
            }

            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!isSalvageable(cursor)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isMerchant(event.getView())) return;

        for (int slot : event.getRawSlots()) {
            if (isInputSlot(slot)) {
                ItemStack dragged = event.getOldCursor();
                if (dragged != null && dragged.getType() != Material.AIR) {
                    if (!isSalvageable(dragged)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

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

        for (int i = 0; i < 54; i++) {
            if (!isInputSlot(i)) continue;
            ItemStack leftover = topInv.getItem(i);
            if (leftover != null && leftover.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(leftover);
                overflow.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                topInv.setItem(i, null);
            }
        }
    }

    private void handleSellButtonClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        int totalCoins = 0, totalGems = 0;
        for (int i = 0; i < 54; i++) {
            if (!isInputSlot(i)) continue;
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(item);
            if (cItem != null) {
                totalCoins += SalvageManager.getInstance().getSellPrice(cItem);
                totalGems += SalvageManager.getInstance().getGemReward(cItem);
                inv.setItem(i, null);
            } else {
                PotionInstance pInst = Main.getInstance().getPotionManager().getInstanceFromItem(item);
                if (pInst != null) {
                    totalCoins += SalvageManager.getInstance().getPotionSellPrice(pInst);
                    inv.setItem(i, null);
                }
            }
        }

        economyManager.addCoins(player, totalCoins);
        if (totalGems > 0) gemsManager.addUnits(player, totalGems);

        if (totalCoins > 0 || totalGems > 0) {
            StringBuilder msg = new StringBuilder(ChatColor.GOLD + "You received ");
            if (totalCoins > 0) msg.append(ChatColor.YELLOW).append(totalCoins).append(" <glyph:coins_icon> " + ChatColor.GOLD + "coins");
            if (totalCoins > 0 && totalGems > 0) msg.append(ChatColor.GOLD + " and ");
            if (totalGems > 0) msg.append(ChatColor.LIGHT_PURPLE).append(totalGems).append(ChatColor.LIGHT_PURPLE + " <glyph:purple_orb_icon> "  + ChatColor.GOLD + "gems");
            msg.append("!");
            player.sendMessage(msg.toString());
            Main.getInstance().getQuestManager().handleSalvage(player, "ANY");
        } else {
            player.sendMessage(ChatColor.YELLOW + "No valid items to salvage.");
        }
    }

    private void handleQuickSellClick(Player player, int slot) {
        ItemRarity[] rarities = {
            ItemRarity.COMMON,
            ItemRarity.UNCOMMON,
            ItemRarity.RARE,
            ItemRarity.EPIC,
            ItemRarity.LEGENDARY
        };
        int index = slot - 47;
        if (index < 0 || index >= rarities.length) return;
        ItemRarity targetRarity = rarities[index];

        Inventory gui = player.getOpenInventory().getTopInventory();
        PlayerInventory playerInv = player.getInventory();
        int handSlot = playerInv.getHeldItemSlot();
        // move matching items from the player's inventory into the GUI
        ItemStack[] storageContents = playerInv.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            if (i == handSlot) continue;
            ItemStack invItem = storageContents[i];
            if (invItem == null || invItem.getType() == Material.AIR) continue;

            CustomItem cItemInv = ItemManager.getInstance().getCustomItemFromItemStack(invItem);
            if (cItemInv != null && cItemInv.getRarity() == targetRarity) {
                int dest = firstEmptyInputSlot(gui);
                if (dest == -1) break;
                playerInv.setItem(i, null);
                gui.setItem(dest, invItem);
            }
        }

        ItemStack off = playerInv.getItemInOffHand();
        if (off != null && off.getType() != Material.AIR) {
            CustomItem cOff = ItemManager.getInstance().getCustomItemFromItemStack(off);
            if (cOff != null && cOff.getRarity() == targetRarity) {
                int dest = firstEmptyInputSlot(gui);
                if (dest != -1) {
                    playerInv.setItemInOffHand(null);
                    gui.setItem(dest, off);
                }
            }
        }
        player.sendMessage(ChatColor.YELLOW + "Moved all " + targetRarity.name().toLowerCase() + " items.");
    }

    /** Moves all salvageable items from the player's inventory into the GUI. */
    private void depositAllItems(Player player, Inventory gui) {
        PlayerInventory inv = player.getInventory();
        int handSlot = inv.getHeldItemSlot();

        for (int i = 0; i < inv.getSize(); i++) {
            // skip armor slots
            if (i >= 36 && i <= 39) continue;
            // skip item in hand
            if (i == handSlot) continue;

            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (!isSalvageable(item)) continue;

            int dest = firstEmptyInputSlot(gui);
            if (dest == -1) break; // no space left

            inv.setItem(i, null);
            gui.setItem(dest, item);
        }

        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() != Material.AIR && isSalvageable(off)) {
            int dest = firstEmptyInputSlot(gui);
            if (dest != -1) {
                inv.setItemInOffHand(null);
                gui.setItem(dest, off);
            }
        }
    }

    /** Returns all items in the GUI back to the player's inventory. */
    private void returnAllItems(Player player, Inventory gui) {
        for (int i = 0; i < 54; i++) {
            if (!isInputSlot(i)) continue;
            ItemStack item = gui.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            overflow.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
            gui.setItem(i, null);
        }
    }

    /** Finds the first empty input slot in the GUI. */
    private int firstEmptyInputSlot(Inventory gui) {
        for (int i = 0; i < 54; i++) {
            if (!isInputSlot(i)) continue;
            ItemStack item = gui.getItem(i);
            if (item == null || item.getType() == Material.AIR) return i;
        }
        return -1;
    }
}