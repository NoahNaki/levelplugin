package me.nakilex.levelplugin.blacksmith.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BlacksmithGUI implements Listener {

    private final EconomyManager economyManager;
    private final ItemUpgradeManager upgradeManager;
    private final ItemManager itemManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>(); // Tracks open GUIs by player UUID

    public BlacksmithGUI(EconomyManager economyManager, ItemUpgradeManager upgradeManager, ItemManager itemManager) {
        this.economyManager = economyManager;
        this.upgradeManager = upgradeManager;
        this.itemManager = itemManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "Blacksmith");

        // Fill GUI with decorative panes
        for (int i = 0; i < gui.getSize(); i++) {
            if (i == 0 || i == 8 || i == 9 || i == 17 || i == 18 || i == 26) {
                gui.setItem(i, createRedGlassPane());
            } else {
                gui.setItem(i, createGlassPane());
            }
        }

        // Add Upgrade Slot (slot 13)
        gui.setItem(13, null);

        // Add Upgrade Button (slot 22) with initial cost and chance = 0
        gui.setItem(22, createUpgradeButton(0, 0));

        // Track GUI
        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }


    private ItemStack createGlassPane() {
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glassPane.getItemMeta();
        if (meta == null) return glassPane;
        meta.setDisplayName(" ");
        glassPane.setItemMeta(meta);
        return glassPane;
    }

    private ItemStack createRedGlassPane() {
        ItemStack glassPane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = glassPane.getItemMeta();
        if (meta == null) return glassPane;
        meta.setDisplayName(" ");
        glassPane.setItemMeta(meta);
        return glassPane;
    }

    private ItemStack createGreenGlassPane() {
        ItemStack glassPane = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = glassPane.getItemMeta();
        if (meta == null) return glassPane;
        meta.setDisplayName(" ");
        glassPane.setItemMeta(meta);
        return glassPane;
    }

    private ItemStack createUpgradeButton(int upgradeCost, int successChance) {
        ItemStack upgradeButton = new ItemStack(Material.ANVIL);
        ItemMeta meta = upgradeButton.getItemMeta();
        if (meta == null) return upgradeButton;

        meta.setDisplayName("§aUpgrade");

        List<String> lore = new ArrayList<>();
        if (upgradeCost > 0) {
            lore.add("§7Cost: §6⛃ " + upgradeCost);
            lore.add("§7Success Chance: §6" + successChance + "%");
            lore.add("§7Click to upgrade your item.");
        } else {
            lore.add("§7Place an item in the slot above.");
        }
        meta.setLore(lore);

        upgradeButton.setItemMeta(meta);
        return upgradeButton;
    }

    private void updateUpgradeButton(Inventory gui, int upgradeCost, int successChance) {
        ItemStack upgradeButton = createUpgradeButton(upgradeCost, successChance);
        gui.setItem(22, upgradeButton);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        // Only handle clicks in our Blacksmith GUI
        if (!openInventories.containsKey(player.getUniqueId())) return;
        Inventory playerGUI = openInventories.get(player.getUniqueId());
        if (!topInventory.equals(playerGUI)) return;
        if (event.getRawSlot() >= playerGUI.getSize()) return;

        // Block default behavior
        event.setCancelled(true);
        int slot = event.getSlot();

        // SHIFT‑CLICK into slot 13
        if (event.isShiftClick() && clickedInventory != null) {
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem != null && currentItem.getType() != Material.AIR && playerGUI.getItem(13) == null) {
                playerGUI.setItem(13, currentItem);
                clickedInventory.setItem(event.getSlot(), null);

                // Delay a tick then refresh cost & chance
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    ItemStack placed = playerGUI.getItem(13);
                    if (placed != null) {
                        CustomItem ci = itemManager.getCustomItemFromItemStack(placed);
                        if (ci != null) {
                            int cost   = upgradeManager.getUpgradeCost(ci);
                            int chance = upgradeManager.getSuccessChance(ci);
                            updateUpgradeButton(playerGUI, cost, chance);
                        } else {
                            updateUpgradeButton(playerGUI, 0, 0);
                        }
                    } else {
                        updateUpgradeButton(playerGUI, 0, 0);
                    }
                }, 1L);
            }
            return;
        }

        // Click directly in slot 13 to place/pickup
        if (slot == 13) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                ItemStack placed = playerGUI.getItem(13);
                if (placed != null) {
                    CustomItem ci = itemManager.getCustomItemFromItemStack(placed);
                    if (ci != null) {
                        int cost   = upgradeManager.getUpgradeCost(ci);
                        int chance = upgradeManager.getSuccessChance(ci);
                        updateUpgradeButton(playerGUI, cost, chance);
                    } else {
                        updateUpgradeButton(playerGUI, 0, 0);
                    }
                } else {
                    updateUpgradeButton(playerGUI, 0, 0);
                }
            }, 1L);

            // Click the upgrade anvil
        } else if (slot == 22) {
            handleUpgradeButtonClick(player, playerGUI);
        }
    }


    private void handleUpgradeButtonClick(Player player, Inventory gui) {
        ItemStack itemStack = gui.getItem(13);
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            player.sendMessage("§cNo item in the upgrade slot!");
            return;
        }

        CustomItem customItem = itemManager.getCustomItemFromItemStack(itemStack);
        if (customItem == null) {
            player.sendMessage("§cInvalid item! Only already‐created custom items can be upgraded.");
            return;
        }

        if (customItem.getUpgradeLevel() >= 5) {
            player.sendMessage("§cThis item has reached the maximum upgrade level!");
            return;
        }

        // Current cost & chance before spending
        int cost   = upgradeManager.getUpgradeCost(customItem);
        int chance = upgradeManager.getSuccessChance(customItem);

        try {
            economyManager.deductCoins(player, cost);
        } catch (IllegalArgumentException ex) {
            player.sendMessage("§cNot enough coins! Upgrade cost: §6⛃ " + cost);
            return;
        }

        // Attempt upgrade
        if (upgradeManager.attemptUpgrade(player, itemStack, customItem)) {
            player.sendMessage("§aUpgrade successful!");
            setTemporaryGreenPanes(gui);
            gui.setItem(13, itemStack);
        } else {
            player.sendMessage("§cUpgrade failed!");
        }

        // Refresh the button’s cost & chance (reflecting new level or unchanged on failure)
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            CustomItem ci = itemManager.getCustomItemFromItemStack(itemStack);
            if (ci != null) {
                int newCost   = upgradeManager.getUpgradeCost(ci);
                int newChance = upgradeManager.getSuccessChance(ci);
                updateUpgradeButton(gui, newCost, newChance);
            } else {
                updateUpgradeButton(gui, 0, 0);
            }
        }, 1L);
    }




    private void setTemporaryGreenPanes(Inventory gui) {
        int[] slots = {0, 8, 9, 17, 18, 26};

        // Set green panes
        for (int slot : slots) {
            gui.setItem(slot, createGreenGlassPane());
        }

        // Reset to red panes after 1 second
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            for (int slot : slots) {
                gui.setItem(slot, createRedGlassPane());
            }
        }, 20L); // 20 ticks = 1 second
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        Inventory closedInventory = event.getInventory();

        // Check if the closed inventory is tracked
        if (!openInventories.containsKey(player.getUniqueId())) return;

        Inventory playerGUI = openInventories.get(player.getUniqueId());

        // Ensure we are dealing with the correct GUI
        if (!closedInventory.equals(playerGUI)) return;

        // Handle returning the item in the upgrade slot
        ItemStack itemInSlot = playerGUI.getItem(13); // Slot 13 is the upgrade slot
        if (itemInSlot != null) {
            player.getInventory().addItem(itemInSlot);
        }

        // Remove the player from the tracking map
        openInventories.remove(player.getUniqueId());
    }
}
