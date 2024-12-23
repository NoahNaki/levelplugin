package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.managers.EconomyManager;
import me.nakilex.levelplugin.mob.HorseData;
import me.nakilex.levelplugin.managers.HorseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;

public class HorseGUI implements Listener {

    private final HorseManager horseManager;
    private final EconomyManager economyManager; // Added EconomyManager for handling costs
    private final int REROLL_COST = 300; // Set the cost for rerolling horses

    // Constructor
    public HorseGUI(HorseManager horseManager, EconomyManager economyManager) {
        this.horseManager = horseManager;
        this.economyManager = economyManager;
    }

    // Create a menu item with custom properties
    private static ItemStack createMenuItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(loreLines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Open or refresh the horse GUI
    public void openHorseMenu(Player player) {
        UUID playerUUID = player.getUniqueId();
        HorseData horseData = horseManager.getHorse(playerUUID);

        // Create the GUI with 36 slots
        Inventory gui = Bukkit.createInventory(null, 36, "Horse Menu");

        // Add horse information to slot 11
        gui.setItem(11, createHorseInfoItem(horseData));

        // Add the reroll button to slot 13
        gui.setItem(13, createRerollButton());

        // Fill empty slots with gray stained glass
        ItemStack filler = createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ", " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        // Open the GUI
        player.openInventory(gui);

        // Start auto-update for live refresh
        startAutoUpdate(player, gui);
    }

    private ItemStack createHorseInfoItem(HorseData horseData) {
        if (horseData != null) {
            // Generate star ratings for speed and jump height
            String speedStars = generateStars(horseData.getSpeed(), 10); // Total 10 stars
            String jumpStars = generateStars(horseData.getJumpHeight(), 10); // Total 10 stars

            // Format horse type (capitalize first letter)
            String formattedType = horseData.getType().substring(0, 1).toUpperCase() + horseData.getType().substring(1).toLowerCase();

            // Create and return the horse info item
            return createMenuItem(
                Material.BOOK,
                "§bYour Horse",
                "",
                "§7Type: §f" + formattedType,
                "§7Speed: §6" + speedStars,
                "§7Jump: §6" + jumpStars
            );
        } else {
            // Fallback if no horse data is found
            return createMenuItem(
                Material.BARRIER,
                "§cNo Horse Owned",
                "",
                "§7You don't own a horse yet!"
            );
        }
    }

    // Generates a star rating string based on the value (out of 10)
    private String generateStars(int value, int max) {
        StringBuilder stars = new StringBuilder();

        // Add filled stars
        for (int i = 0; i < value; i++) {
            stars.append("✦"); // Filled star
        }

        // Add empty stars
        for (int i = value; i < max; i++) {
            stars.append("✧"); // Empty star
        }

        return stars.toString();
    }



    // Create the reroll button with cost details
    private ItemStack createRerollButton() {
        return createMenuItem(
            Material.SADDLE,
            "§aBuy a New Horse",
            "",
            "§cYour current horse will be deleted.",
            "",
            "§7Cost: §6⛃" + REROLL_COST,
            "",
            "§7Click to buy a new horse!"
        );
    }

    // Update the GUI dynamically without closing it
    private void updateHorseInfo(Inventory inventory, UUID playerUUID) {
        HorseData horseData = horseManager.getHorse(playerUUID);
        inventory.setItem(11, createHorseInfoItem(horseData)); // Refresh slot 11
    }

    // Automatically refresh the GUI every second
    public void startAutoUpdate(Player player, Inventory inventory) {
        UUID playerUUID = player.getUniqueId();

        Bukkit.getScheduler().runTaskTimer(
            me.nakilex.levelplugin.Main.getInstance(),
            () -> {
                if (player.getOpenInventory().getTitle().equals("Horse Menu")) {
                    updateHorseInfo(inventory, playerUUID); // Refresh stats
                }
            },
            0L, 20L // 20 ticks = 1 second
        );
    }

    // Handle GUI clicks
    @EventHandler
    public void handleSaddleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // Check if the inventory is the horse menu
        if (!event.getView().getTitle().equals("Horse Menu")) return;
        event.setCancelled(true); // Prevent taking/moving items

        // Handle clicks on the reroll button
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.SADDLE) return;

        UUID playerUUID = player.getUniqueId();

        // Check if the player has enough coins
        int playerBalance = economyManager.getBalance(player);
        if (playerBalance < REROLL_COST) {
            player.sendMessage("§cYou don't have enough coins to buy a new horse! (Cost: §6" + REROLL_COST + " coins§c)");
            return;
        }

        // Deduct coins and reroll the horse
        economyManager.deductCoins(player, REROLL_COST);
        horseManager.dismountHorse(player); // Force dismount before rerolling
        horseManager.rerollHorse(playerUUID);

        // Update the horse stats immediately in the GUI
        updateHorseInfo(inventory, playerUUID);

        player.sendMessage("§aYou bought a new horse for §6" + REROLL_COST + " coins§a!");
    }
}
