package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.managers.StatsManager.StatType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;

public class StatsMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.BLUE + "Allocate Your Stats")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem().getType() == Material.WRITTEN_BOOK) {
                String displayName = ChatColor.stripColor(
                    event.getCurrentItem().getItemMeta().getDisplayName()
                );

                StatType stat = null;
                if (displayName.contains("STR")) stat = StatType.STR;
                else if (displayName.contains("AGI")) stat = StatType.AGI;
                else if (displayName.contains("INT")) stat = StatType.INT;
                else if (displayName.contains("DEX")) stat = StatType.DEX;
                else if (displayName.contains("HP"))  stat = StatType.HP;
                else if (displayName.contains("DEF")) stat = StatType.DEF;

                if (stat != null) {
                    if (event.getClick() == ClickType.LEFT) {
                        StatsManager.getInstance().investStat(player, stat);
                    } else if (event.getClick() == ClickType.RIGHT) {
                        StatsManager.getInstance().refundStat(player, stat);
                    }
                    // Refresh GUI
                    player.openInventory(StatsInventory.getStatsMenu(player));
                }
            }
        }
    }
}
