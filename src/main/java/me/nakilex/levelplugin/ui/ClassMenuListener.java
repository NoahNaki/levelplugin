package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.managers.PlayerClass;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ClassMenuListener implements Listener {

    @EventHandler
    public void onClassMenuClick(InventoryClickEvent event) {
        // Ensure the click is made by a player
        if (!(event.getWhoClicked() instanceof Player)) {
            System.out.println("Click ignored: Not a player.");
            return;
        }
        Player player = (Player) event.getWhoClicked();

        // Check if the inventory title matches
        String inventoryTitle = event.getView().getTitle();
        System.out.println("Inventory Title: " + inventoryTitle);
        if (!inventoryTitle.equals(ChatColor.DARK_GREEN + "Choose Your Class")) {
            System.out.println("Click ignored: Inventory title did not match.");
            return;
        }

        event.setCancelled(true); // Prevent moving items in the menu

        ItemStack clickedItem = event.getCurrentItem();
        System.out.println("Clicked Item: " + clickedItem);

        // Validate the clicked item
        if (clickedItem == null) {
            System.out.println("Click ignored: Clicked item is null.");
            return;
        }
        if (!clickedItem.hasItemMeta()) {
            System.out.println("Click ignored: Item has no metadata.");
            return;
        }
        if (!clickedItem.getItemMeta().hasDisplayName()) {
            System.out.println("Click ignored: Item has no display name.");
            return;
        }

        // Get the display name of the clicked item
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        System.out.println("Display Name: " + displayName);

        if (displayName == null) {
            System.out.println("Click ignored: Display name is null.");
            return;
        }

        // Assign player class and feedback
        PlayerClass selectedClass = null;
        String className = null;

        switch (displayName.toUpperCase()) {
            case "START AS A WARRIOR!":
                selectedClass = PlayerClass.WARRIOR;
                className = "Warrior";
                break;
            case "START AS AN ARCHER!":
                selectedClass = PlayerClass.ARCHER;
                className = "Archer";
                break;
            case "START AS A MAGE!":
                selectedClass = PlayerClass.MAGE;
                className = "Mage";
                break;
            case "START AS A ROGUE!":
                selectedClass = PlayerClass.ROGUE;
                className = "Rogue";
                break;
            default:
                System.out.println("Invalid class selection detected: " + displayName);
                player.sendMessage(ChatColor.RED + "Invalid class selection.");
                return;
        }


        // Set the player's class
        if (selectedClass != null) {
            System.out.println("Assigning class: " + className);
            StatsManager.getInstance().getPlayerStats(player).playerClass = selectedClass;

            // Send feedback to the player
            player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Class Selection" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GREEN + "You have selected " + ChatColor.AQUA + className + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Close the inventory
            player.closeInventory();
        }
    }
}
