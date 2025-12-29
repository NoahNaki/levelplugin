package me.nakilex.levelplugin.player.fishing.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FishingRewardsGUI implements Listener, CommandExecutor {

    private static final String TITLE = "Fishing Rewards";
    private static final int INFO_SLOT = 8;
    private static final int CANCEL_SLOT = 45;
    private static final int CATALOG_SLOT = 47;
    private static final int WITHDRAW_SLOT = 46;
    private static final int DEPOSIT_SLOT = 52;
    private static final int CONFIRM_SLOT = 53;
    private final EconomyManager economyManager;
    private final Main plugin;
    private final FishingCatalogGUI catalogGUI;

    public FishingRewardsGUI(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.catalogGUI = new FishingCatalogGUI(plugin, plugin.getFishingRewardsConfig(), this);
        plugin.getCommand("fishrewards").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(54, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Place caught fish in the center.");
            lore.add(ChatColor.GRAY + "Confirm to sell them for coins.");
            lore.add("");
            lore.add(ChatColor.GOLD + "Fish Value:");
            lore.add(ChatColor.GRAY + "  Scales with size and rarity.");
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to confirm sale", null));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }

        inv.setItem(INFO_SLOT, info);
        inv.setItem(CANCEL_SLOT, GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
        inv.setItem(WITHDRAW_SLOT, GuiUtil.getNexoItem("arrow_down", ChatColor.YELLOW + "Return All"));
        inv.setItem(CATALOG_SLOT, createCatalogItem());
        inv.setItem(DEPOSIT_SLOT, GuiUtil.getNexoItem("arrow_up", ChatColor.YELLOW + "Deposit All"));
        inv.setItem(CONFIRM_SLOT, GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm Sale"));
        player.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        open(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        ItemStack current = event.getCurrentItem();

        if (rawSlot == CANCEL_SLOT) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }
        if (rawSlot == CONFIRM_SLOT) {
            event.setCancelled(true);
            handleSell(player, top);
            return;
        }
        if (rawSlot == INFO_SLOT) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot == CATALOG_SLOT) {
            event.setCancelled(true);
            catalogGUI.open(player);
            return;
        }
        if (rawSlot == WITHDRAW_SLOT) {
            event.setCancelled(true);
            withdrawAll(player, top);
            return;
        }
        if (rawSlot == DEPOSIT_SLOT) {
            event.setCancelled(true);
            depositAll(player, top);
            return;
        }

        if (rawSlot < top.getSize()) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR && !FishingItemUtil.isFish(cursor)) {
                event.setCancelled(true);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Only fish can be sold here.");
                return;
            }
            if (current != null && current.getType() != Material.AIR && !FishingItemUtil.isFish(current)) {
                event.setCancelled(true);
                return;
            }
        } else {
            if (event.isShiftClick() && current != null && current.getType() != Material.AIR
                    && !FishingItemUtil.isFish(current)) {
                event.setCancelled(true);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Only fish can be sold here.");
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        ItemStack item = event.getOldCursor();
        if (item == null || item.getType() == Material.AIR) return;
        if (!FishingItemUtil.isFish(item)) {
            event.setCancelled(true);
        }
    }

    private void handleSell(Player player, Inventory top) {
        int total = 0;
        for (int slot = 0; slot < top.getSize(); slot++) {
            if (isControlSlot(slot)) continue;
            ItemStack stack = top.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (!FishingItemUtil.isFish(stack)) continue;
            int value = FishingItemUtil.getFishValue(stack);
            total += value * stack.getAmount();
            top.setItem(slot, null);
        }

        if (total <= 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Place fish in the menu to sell them.");
            return;
        }

        economyManager.addCoins(player, total, false);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Sold your catch for " + ChatColor.YELLOW + total + " <glyph:coins_icon>.");
        player.updateInventory();
    }

    private void depositAll(Player player, Inventory top) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean moved = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (!FishingItemUtil.isFish(stack)) continue;
            int slot = findEmptySlot(top);
            if (slot == -1) {
                break;
            }
            top.setItem(slot, stack);
            contents[i] = null;
            moved = true;
        }
        player.getInventory().setContents(contents);
        if (!moved) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You have no fish to deposit.");
        }
    }

    private void withdrawAll(Player player, Inventory top) {
        boolean moved = false;
        for (int slot = 0; slot < top.getSize(); slot++) {
            if (isControlSlot(slot)) continue;
            ItemStack stack = top.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (!FishingItemUtil.isFish(stack)) continue;
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            if (leftover.isEmpty()) {
                top.setItem(slot, null);
                moved = true;
            } else {
                top.setItem(slot, leftover.values().iterator().next());
                break;
            }
        }
        if (!moved) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No fish to withdraw.");
        }
        player.updateInventory();
    }

    private boolean isControlSlot(int slot) {
        return slot == INFO_SLOT || slot == CANCEL_SLOT || slot == CONFIRM_SLOT
                || slot == WITHDRAW_SLOT || slot == DEPOSIT_SLOT || slot == CATALOG_SLOT;
    }

    private ItemStack createCatalogItem() {
        ItemStack item = GuiUtil.getNexoItem("info", ChatColor.AQUA + "Fishing Catalog");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Browse every fish you've caught.");
            lore.add(ChatColor.GRAY + "Unknown entries reveal on discovery.");
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to open the catalog", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private int findEmptySlot(Inventory top) {
        for (int slot = 0; slot < top.getSize(); slot++) {
            if (isControlSlot(slot)) continue;
            ItemStack stack = top.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                return slot;
            }
        }
        return -1;
    }
}
