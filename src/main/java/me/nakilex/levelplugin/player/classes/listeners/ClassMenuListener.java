package me.nakilex.levelplugin.player.classes.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
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
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        String inventoryTitle = event.getView().getTitle();
        if (!inventoryTitle.equals(ChatColor.DARK_GREEN + "Choose Your Class")) {
            return;
        }

        event.setCancelled(true); // Prevent moving items in the menu

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            return;
        }
        if (!clickedItem.hasItemMeta()) {
            return;
        }
        if (!clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }

        // Get the display name of the clicked item
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (displayName == null) {
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
                player.sendMessage(ChatColor.RED + "Invalid class selection.");
                return;
        }

        if (selectedClass != null) {
            StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass = selectedClass;
            player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Class Selection" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GREEN + "You have selected " + ChatColor.AQUA + className + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.closeInventory();
        }
    }
}
