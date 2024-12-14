package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.managers.PlayerClass;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ClassMenuListener implements Listener {

    @EventHandler
    public void onClassMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Choose Your Class")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            String displayName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            // e.g. "Warrior", "Archer", "Mage", "Rogue"

            switch (displayName.toUpperCase()) {
                case "WARRIOR":
                    StatsManager.getInstance().getPlayerStats(player).playerClass = PlayerClass.WARRIOR;
                    player.sendMessage("§aYou are now a Warrior!");
                    player.closeInventory();
                    break;
                case "ARCHER":
                    StatsManager.getInstance().getPlayerStats(player).playerClass = PlayerClass.ARCHER;
                    player.sendMessage("§aYou are now an Archer!");
                    player.closeInventory();
                    break;
                case "MAGE":
                    StatsManager.getInstance().getPlayerStats(player).playerClass = PlayerClass.MAGE;
                    player.sendMessage("§aYou are now a Mage!");
                    player.closeInventory();
                    break;
                case "ROGUE":
                    StatsManager.getInstance().getPlayerStats(player).playerClass = PlayerClass.ROGUE;
                    player.sendMessage("§aYou are now a Rogue!");
                    player.closeInventory();
                    break;
                default:
                    break;
            }
        }
    }
}
