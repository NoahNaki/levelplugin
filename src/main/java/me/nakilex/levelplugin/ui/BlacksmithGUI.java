package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.managers.EconomyManager;
import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.managers.ItemManager;
import me.nakilex.levelplugin.managers.ItemUpgradeManager;
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

        // Add Upgrade Button (slot 22)
        ItemStack upgradeButton = createUpgradeButton(0); // Pass 0 as the initial upgrade cost
        gui.setItem(22, upgradeButton);

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

    private ItemStack createUpgradeButton(int upgradeCost) {
        ItemStack upgradeButton = new ItemStack(Material.ANVIL);
        ItemMeta meta = upgradeButton.getItemMeta();
        if (meta == null) return upgradeButton;

        meta.setDisplayName("§aUpgrade");

        // Add lore to display the upgrade cost
        List<String> lore = new ArrayList<>();
        if (upgradeCost > 0) {
            lore.add("§7Cost: §6⛃ " + upgradeCost);
            lore.add("§7Click to upgrade your item.");
        } else {
            lore.add("§7Place an item in the slot above.");
        }
        meta.setLore(lore);

        upgradeButton.setItemMeta(meta);
        return upgradeButton;
    }

    private void updateUpgradeButton(Inventory gui, int upgradeCost) {
        ItemStack upgradeButton = createUpgradeButton(upgradeCost);
        gui.setItem(22, upgradeButton); // Slot 22 is the upgrade button
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory(); // The Blacksmith GUI

        // Ensure the event is only for the Blacksmith GUI
        if (!openInventories.containsKey(player.getUniqueId())) return;

        Inventory playerGUI = openInventories.get(player.getUniqueId());
        if (!topInventory.equals(playerGUI)) return;

        if (event.getRawSlot() >= 27) return; // Ignore clicks in the player's inventory

        // Prevent default behavior for all slots in the Blacksmith GUI
        event.setCancelled(true);

        int slot = event.getSlot();

        // Handle shift-click behavior
        if (event.isShiftClick() && clickedInventory != null) {
            ItemStack currentItem = event.getCurrentItem();

            // Ensure the item is valid
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                // Check if slot 13 is empty
                if (playerGUI.getItem(13) == null) {
                    playerGUI.setItem(13, currentItem);
                    clickedInventory.setItem(event.getSlot(), null); // Remove the item from the clicked inventory

                    // Update the upgrade button based on the item in slot 13
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                        ItemStack itemStack = playerGUI.getItem(13);
                        if (itemStack != null) {
                            CustomItem customItem = itemManager.getCustomItemFromItemStack(itemStack);
                            if (customItem != null) {
                                int upgradeCost = upgradeManager.getUpgradeCost(customItem);
                                updateUpgradeButton(playerGUI, upgradeCost);
                            } else {
                                updateUpgradeButton(playerGUI, 0);
                            }
                        } else {
                            updateUpgradeButton(playerGUI, 0);
                        }
                    }, 1L);
                }
            }
            return; // Stop further handling as shift-click is processed
        }

        if (slot == 13) {
            // Allow item placement in the upgrade slot
            event.setCancelled(false);

            // Update the upgrade button based on the item in slot 13
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                ItemStack itemStack = playerGUI.getItem(13);
                if (itemStack != null) {
                    CustomItem customItem = itemManager.getCustomItemFromItemStack(itemStack);
                    if (customItem != null) {
                        int upgradeCost = upgradeManager.getUpgradeCost(customItem);
                        updateUpgradeButton(playerGUI, upgradeCost);
                    } else {
                        updateUpgradeButton(playerGUI, 0);
                    }
                } else {
                    updateUpgradeButton(playerGUI, 0);
                }
            }, 1L);
        } else if (slot == 22) {
            handleUpgradeButtonClick(player, playerGUI);
        }
    }

    private void handleUpgradeButtonClick(Player player, Inventory playerGUI) {
        ItemStack itemStack = playerGUI.getItem(13); // Get item from the upgrade slot
        if (itemStack == null) {
            player.sendMessage("§cNo item in the upgrade slot!");
            return;
        }

        CustomItem customItem = itemManager.getCustomItemFromItemStack(itemStack);
        if (customItem == null) {
            player.sendMessage("§cInvalid item! Only custom items can be upgraded.");
            return;
        }

        if (customItem.getUpgradeLevel() >= 5) {
            player.sendMessage("§cThis item has reached the maximum upgrade level!");
            return;
        }

        int upgradeCost = upgradeManager.getUpgradeCost(customItem);

        try {
            economyManager.deductCoins(player, upgradeCost);

            if (upgradeManager.attemptUpgrade(itemStack, customItem)) {
                player.sendMessage("§aUpgrade successful!");

                // Change red panes to green temporarily
                setTemporaryGreenPanes(playerGUI);

                // Update the GUI with the upgraded item
                playerGUI.setItem(13, itemStack);

                // Update the upgrade button to reflect the new cost
                int newUpgradeCost = upgradeManager.getUpgradeCost(customItem);
                updateUpgradeButton(playerGUI, newUpgradeCost);
            } else {
                player.sendMessage("§cUpgrade failed!");
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cNot enough coins! Upgrade cost: §6⛃ " + upgradeCost);
        }
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
