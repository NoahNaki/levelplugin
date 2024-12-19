package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.EconomyManager;
import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemManager;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        ItemStack glassPane = createGlassPane();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glassPane);
        }

        // Add Upgrade Slot (slot 13)
        gui.setItem(13, null);

        // Add Upgrade Button (slot 22)
        ItemStack upgradeButton = createUpgradeButton();
        gui.setItem(22, upgradeButton);

        // Track GUI
        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    private ItemStack createGlassPane() {
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glassPane.getItemMeta();
        meta.setDisplayName(" ");
        glassPane.setItemMeta(meta);
        return glassPane;
    }

    private ItemStack createUpgradeButton() {
        ItemStack upgradeButton = new ItemStack(Material.ANVIL);
        ItemMeta meta = upgradeButton.getItemMeta();
        meta.setDisplayName("§aUpgrade");
        upgradeButton.setItemMeta(meta);
        return upgradeButton;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory(); // The Blacksmith GUI

        // Debugging
        Main.getInstance().getLogger().info("=== InventoryClickEvent Fired ===");
        Main.getInstance().getLogger().info("Clicked Inventory: " + (clickedInventory == null ? "NULL" : clickedInventory.getType()));
        Main.getInstance().getLogger().info("Top Inventory: " + (topInventory == null ? "NULL" : topInventory.getType()));
        Main.getInstance().getLogger().info("Slot: " + event.getSlot());
        Main.getInstance().getLogger().info("Raw Slot: " + event.getRawSlot());
        Main.getInstance().getLogger().info("Player Inventory: " + player.getInventory().getType());

        // Ensure the event is only for the Blacksmith GUI
        if (!openInventories.containsKey(player.getUniqueId())) {
            Main.getInstance().getLogger().info("Ignored: Player does not have a tracked Blacksmith GUI.");
            return;
        }

        Inventory playerGUI = openInventories.get(player.getUniqueId());
        if (!topInventory.equals(playerGUI)) {
            Main.getInstance().getLogger().info("Ignored: Event inventory does not match the Blacksmith GUI.");
            return;
        }

        if (event.getRawSlot() >= 27) {
            Main.getInstance().getLogger().info("Ignored: Click was in the player's inventory.");
            return;
        }


        // Prevent default behavior for all slots in the Blacksmith GUI
        event.setCancelled(true);

        int slot = event.getSlot();
        Main.getInstance().getLogger().info("Clicked Slot: " + slot);

        if (slot == 13) {
            // Allow item placement in the upgrade slot
            Main.getInstance().getLogger().info("Upgrade Slot Clicked: Allowing item placement.");
            event.setCancelled(false);
        } else if (slot == 22) {
            // Handle upgrade button click
            Main.getInstance().getLogger().info("Upgrade Button Clicked.");
            handleUpgradeButtonClick(player, playerGUI);
        } else {
            Main.getInstance().getLogger().info("Ignored: Click was in an unrelated slot.");
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

        int upgradeCost = upgradeManager.getUpgradeCost(customItem);

        try {
            // Deduct coins
            economyManager.deductCoins(player, upgradeCost);

            // Attempt upgrade
            if (upgradeManager.attemptUpgrade(itemStack, customItem)) {
                player.sendMessage("§aUpgrade successful!");
                // Update the GUI with the upgraded item
                playerGUI.setItem(13, itemManager.updateItem(itemStack, customItem, customItem.getUpgradeLevel()));
            } else {
                player.sendMessage("§cUpgrade failed!");
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cNot enough coins! Upgrade cost: " + upgradeCost);
        }
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
            // Add the item back to the player's inventory
            player.getInventory().addItem(itemInSlot);
        }

        // Remove the player from the tracking map
        openInventories.remove(player.getUniqueId());
    }
}
