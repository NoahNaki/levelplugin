package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.managers.HorseConfigManager;
import me.nakilex.levelplugin.mob.HorseData;
import me.nakilex.levelplugin.mob.HorseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class HorseGUI implements Listener {

    private final HorseManager horseManager;

    public HorseGUI(HorseManager horseManager) {
        this.horseManager = horseManager;
    }


    // Open the horse GUI for the player
    public void openHorseMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "Horse Menu");

        // Add the saddle item for rerolling
        ItemStack saddle = new ItemStack(Material.SADDLE);
        ItemMeta meta = saddle.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aReroll Horse");
            saddle.setItemMeta(meta);
        }
        gui.setItem(4, saddle);

        player.openInventory(gui);
    }

    // Handle clicking on the saddle item to reroll
    @EventHandler
    public void handleSaddleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        if (!event.getView().getTitle().equals("Horse Menu")) return;
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.SADDLE) return;

        // Reroll the horse stats
        UUID playerUUID = player.getUniqueId();
        horseManager.rerollHorse(playerUUID);

        HorseData newHorse = horseManager.getHorse(playerUUID);
        player.sendMessage("§eYou rerolled your horse! New stats: " + newHorse.toString());
        //player.closeInventory();
    }
}
