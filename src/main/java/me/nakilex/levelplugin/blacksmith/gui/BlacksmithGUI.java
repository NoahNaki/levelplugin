package me.nakilex.levelplugin.blacksmith.gui;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.managers.ItemRepairManager;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BlacksmithGUI implements Listener {

    private static final int GUI_SIZE = 27;
    private static final String GUI_TITLE_UPGRADE = ChatColor.DARK_GRAY + "Blacksmith: Upgrade";
    private static final String GUI_TITLE_REPAIR = ChatColor.DARK_GRAY + "Blacksmith: Repair";

    private final EconomyManager economyManager;
    private final ItemUpgradeManager upgradeManager;
    private final ItemRepairManager repairManager;
    private final ItemManager itemManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public BlacksmithGUI(EconomyManager economyManager, ItemUpgradeManager upgradeManager, ItemManager itemManager, ItemRepairManager repairManager) {
        this.economyManager = economyManager;
        this.upgradeManager = upgradeManager;
        this.repairManager = repairManager;
        this.itemManager = itemManager;
    }

    public void openUpgradeGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, GUI_SIZE, GUI_TITLE_UPGRADE);
        fillGuiWithFiller(gui);
        gui.setItem(8, createUpgradeInfoItem());
        gui.setItem(11, getOraxenItem("arrow_left", ChatColor.GRAY + "Go to Repair"));
        gui.setItem(15, getOraxenItem("arrow_right", ChatColor.GRAY + "Go to Repair"));
        gui.setItem(13, null);
        gui.setItem(22, createUpgradeButton(0, 0));
        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    public void openRepairGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, GUI_SIZE, GUI_TITLE_REPAIR);
        fillGuiWithFiller(gui);
        gui.setItem(8, createRepairInfoItem());
        gui.setItem(11, getOraxenItem("arrow_left", ChatColor.GRAY + "Go to Upgrade"));
        gui.setItem(15, getOraxenItem("arrow_right", ChatColor.GRAY + "Go to Upgrade"));
        gui.setItem(0, createRepairAllButton(calculateTotalRepairCost(player)));
        gui.setItem(13, null);
        gui.setItem(22, createRepairButton(0));
        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    private void fillGuiWithFiller(Inventory gui) {
        ItemStack filler = createFiller();
        for (int i = 0; i < GUI_SIZE; i++) gui.setItem(i, filler);
    }

    private ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private ItemStack getOraxenItem(String id, String name) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createUpgradeInfoItem() {
        ItemStack info = getOraxenItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "",
                ChatColor.GRAY + "Upgrade Success Rates:",
                ChatColor.GRAY + "",
                ChatColor.GRAY + "  +0 ➜ +1: " + ChatColor.WHITE + "33%",
                ChatColor.GRAY + "  +1 ➜ +2: " + ChatColor.WHITE + "15%",
                ChatColor.GRAY + "  +2 ➜ +3: " + ChatColor.WHITE + "10%",
                ChatColor.GRAY + "  +3 ➜ +4: " + ChatColor.WHITE + "5%",
                ChatColor.GRAY + "  +4 ➜ +5: " + ChatColor.WHITE + "2%",
                "",
                ChatColor.GRAY + "Upgrade costs scale with " + ChatColor.AQUA + "rarity" + ChatColor.GRAY + " and",
                ChatColor.GRAY + "current upgrade " + ChatColor.AQUA + "tier" + ChatColor.GRAY + "."
            ));
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createRepairInfoItem() {
        ItemStack info = getOraxenItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "",
                ChatColor.GRAY + "Costs increase with item " + ChatColor.AQUA + "rarity",
                ChatColor.GRAY + "and " + ChatColor.AQUA + "durability" + ChatColor.GRAY + " lost.",
                "",
                ChatColor.GRAY + "Place a damaged item in the center.",
                ChatColor.GRAY + "Use " + ChatColor.GREEN + "Repair Item" + ChatColor.GRAY + " or",
                ChatColor.GREEN + "Repair All Items" + ChatColor.GRAY + " to fix it."
            ));
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createUpgradeButton(int upgradeCost, int successChance) {
        ItemStack upgrade = new ItemStack(Material.ANVIL);
        ItemMeta meta = upgrade.getItemMeta();
        meta.setDisplayName("§aUpgrade");
        List<String> lore = new ArrayList<>();
        if (upgradeCost > 0) {
            lore.add("§7Cost: §6⛃ " + upgradeCost);
            lore.add("§7Success Chance: §6" + successChance + "%");
        } else {
            lore.add("§7Place an item in upgrade slot.");
        }
        meta.setLore(lore);
        upgrade.setItemMeta(meta);
        return upgrade;
    }

    private ItemStack createRepairButton(int cost) {
        ItemStack repair = new ItemStack(Material.ANVIL);
        ItemMeta meta = repair.getItemMeta();
        meta.setDisplayName("§bRepair Item");
        List<String> lore = new ArrayList<>();
        if (cost > 0) {
            lore.add("§7Cost: §6⛃ " + cost);
        } else {
            lore.add("§7Place an item in the repair slot.");
        }
        meta.setLore(lore);
        repair.setItemMeta(meta);
        return repair;
    }

    private ItemStack createRepairAllButton(int totalCost) {
        ItemStack repairAll = new ItemStack(Material.GRINDSTONE);
        ItemMeta meta = repairAll.getItemMeta();
        meta.setDisplayName("§cRepair All Items");
        List<String> lore = new ArrayList<>();
        if (totalCost > 0) {
            lore.add("§7Total Cost: §6⛃ " + totalCost);
        } else {
            lore.add("§7No damaged items found.");
        }
        meta.setLore(lore);
        repairAll.setItemMeta(meta);
        return repairAll;
    }

    private int calculateTotalRepairCost(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci != null && ci.getCurrentDurability() < ci.getMaxDurability()) {
                total += repairManager.getRepairCost(ci);
            }
        }
        return total;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null || !event.getView().getTopInventory().equals(gui)) return;

        int rawSlot = event.getRawSlot();
        int clickedSlot = event.getSlot();

        String title = event.getView().getTitle();

// Allow placing into slot 13 only manually
        if (event.getRawSlot() == 13) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Inventory updatedGui = openInventories.get(player.getUniqueId());
                if (updatedGui != null) updateActionButton(player, updatedGui, title);
            }, 1L);
            return;
        }


// Allow dragging into slot 13
        if (event.getAction() == InventoryAction.PLACE_ALL ||
            event.getAction() == InventoryAction.PLACE_SOME ||
            event.getAction() == InventoryAction.PLACE_ONE ||
            event.getAction() == InventoryAction.SWAP_WITH_CURSOR ||
            event.getAction() == InventoryAction.HOTBAR_SWAP &&
                event.getSlot() == 13) {
            event.setCancelled(false);
            return;
        }

// Cancel all interactions outside the GUI
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Inventory updatedGui = openInventories.get(player.getUniqueId());
                    if (updatedGui != null) updateActionButton(player, updatedGui, title);
                }, 1L);
            }
            return; // allow interactions with the player's inventory
        }

        event.setCancelled(true);


        if (rawSlot == 11 || rawSlot == 15) {
            ItemStack carriedItem = gui.getItem(13); // Save item before switching
            boolean switchingToRepair = title.equals(GUI_TITLE_UPGRADE);
            if (switchingToRepair) {
                openRepairGUI(player);
            } else {
                openUpgradeGUI(player);
            }
            // Restore the carried item into the new GUI and update the button
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Inventory newGui = openInventories.get(player.getUniqueId());
                if (newGui != null) {
                    newGui.setItem(13, carriedItem);
                    String newTitle = switchingToRepair ? GUI_TITLE_REPAIR : GUI_TITLE_UPGRADE;
                    updateActionButton(player, newGui, newTitle);
                }
            }, 1L);
            return;
        }


        if (rawSlot == 0 && title.equals(GUI_TITLE_REPAIR)) {
            handleRepairAllClick(player);
            gui.setItem(0, createRepairAllButton(calculateTotalRepairCost(player)));
            return;
        }

        if (rawSlot == 22) {
            ItemStack item = gui.getItem(13);
            if (item == null || item.getType().isAir()) return;
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci == null) return;

            if (title.equals(GUI_TITLE_UPGRADE)) {
                int cost = upgradeManager.getUpgradeCost(ci);
                int chance = upgradeManager.getSuccessChance(ci);
                try {
                    economyManager.deductCoins(player, cost);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("§cNot enough coins! Upgrade cost: §6⛃ " + cost);
                    return;
                }
                if (upgradeManager.attemptUpgrade(player, item, ci)) {
                    player.sendMessage("§aUpgrade successful!");
                    Main.getInstance().getQuestManager().handleUpgrade(player, String.valueOf(ci.getId()));
                    gui.setItem(13, item);
                } else {
                    player.sendMessage("§cUpgrade failed!");
                }
                gui.setItem(22, createUpgradeButton(upgradeManager.getUpgradeCost(ci), upgradeManager.getSuccessChance(ci)));
            } else if (title.equals(GUI_TITLE_REPAIR)) {
                int cost = repairManager.getRepairCost(ci);
                try {
                    economyManager.deductCoins(player, cost);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("§cNot enough coins to repair! Cost: §6⛃ " + cost);
                    return;
                }
                if (repairManager.repairItem(player, item, ci)) {
                    player.sendMessage("§aItem repaired!");
                    gui.setItem(13, item);
                    ItemUtil.updateCustomItemTooltip(item, player);
                }
                gui.setItem(22, createRepairButton(0));
            }
        }

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            ItemStack current = gui.getItem(13);
            if (current == null || current.getType().isAir()) {
                gui.setItem(22, title.equals(GUI_TITLE_UPGRADE) ? createUpgradeButton(0, 0) : createRepairButton(0));
                return;
            }
            CustomItem ci = itemManager.getCustomItemFromItemStack(current);
            if (ci != null) {
                if (title.equals(GUI_TITLE_UPGRADE)) {
                    gui.setItem(22, createUpgradeButton(upgradeManager.getUpgradeCost(ci), upgradeManager.getSuccessChance(ci)));
                } else if (title.equals(GUI_TITLE_REPAIR)) {
                    gui.setItem(22, createRepairButton(repairManager.getRepairCost(ci)));
                }
            }
        }, 1L);
    }

    private void handleRepairAllClick(Player player) {
        int totalCost = 0;
        List<ItemStack> toRepair = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci != null && ci.getCurrentDurability() < ci.getMaxDurability()) {
                totalCost += repairManager.getRepairCost(ci);
                toRepair.add(item);
            }
        }
        if (totalCost == 0) {
            player.sendMessage("§7No damaged items found.");
            return;
        }
        try {
            economyManager.deductCoins(player, totalCost);
        } catch (IllegalArgumentException ex) {
            player.sendMessage("§cYou need §6⛃ " + totalCost + " §cto repair all items.");
            return;
        }
        for (ItemStack item : toRepair) {
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci != null) {
                repairManager.repairItem(player, item, ci);
                ItemUtil.updateCustomItemTooltip(item, player);
            }
        }
        player.sendMessage("§aAll items repaired! Total cost: §6⛃ " + totalCost);
    }

    private void updateActionButton(Player player, Inventory gui, String title) {
        ItemStack current = gui.getItem(13);
        if (current == null || current.getType().isAir()) {
            gui.setItem(22, title.equals(GUI_TITLE_UPGRADE)
                ? createUpgradeButton(0, 0)
                : createRepairButton(0));
            return;
        }

        CustomItem ci = itemManager.getCustomItemFromItemStack(current);
        if (ci == null) {
            gui.setItem(22, title.equals(GUI_TITLE_UPGRADE)
                ? createUpgradeButton(0, 0)
                : createRepairButton(0));
            return;
        }

        if (title.equals(GUI_TITLE_UPGRADE)) {
            gui.setItem(22, createUpgradeButton(
                upgradeManager.getUpgradeCost(ci),
                upgradeManager.getSuccessChance(ci)));
        } else {
            gui.setItem(22, createRepairButton(repairManager.getRepairCost(ci)));
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui)) return;
        ItemStack item = gui.getItem(13);
        if (item != null) player.getInventory().addItem(item);
        openInventories.remove(player.getUniqueId());
    }
}
