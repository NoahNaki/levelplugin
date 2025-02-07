package me.nakilex.levelplugin.player.attributes.listeners;

import me.nakilex.levelplugin.player.attributes.gui.StatsInventory;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class StatsMenuListener implements Listener {

    private final Set<Player> refundConfirmations = new HashSet<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().endsWith("skill points remaining")) {
            event.setCancelled(true); // Prevent item movement

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            StatsManager statsManager = StatsManager.getInstance();
            String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            // Handle Refund All Skill Points Confirmation
            if (clickedItem.getType() == Material.ENDER_EYE) {
                if (refundConfirmations.contains(player)) {
                    statsManager.refundAllStats(player);
                    refundConfirmations.remove(player);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    refundConfirmations.add(player);
                    player.sendMessage(ChatColor.YELLOW + "Click again to confirm refunding all skill points.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
                // Refresh the inventory to reflect changes
                player.openInventory(StatsInventory.getStatsMenu(player));
                return;
            }

            // Determine which stat was clicked
            StatsManager.StatType stat = null;
            if (displayName.contains("Strength")) stat = StatsManager.StatType.STR;
            else if (displayName.contains("Agility")) stat = StatsManager.StatType.AGI;
            else if (displayName.contains("Intelligence")) stat = StatsManager.StatType.INT;
            else if (displayName.contains("Dexterity")) stat = StatsManager.StatType.DEX;
            else if (displayName.contains("Health")) stat = StatsManager.StatType.HP;
            else if (displayName.contains("Defense")) stat = StatsManager.StatType.DEF;

            if (stat != null) {
                int availablePoints = statsManager.getPlayerStats(player.getUniqueId()).skillPoints;
                int currentStatValue = statsManager.getStatValue(player, stat);

                // Left-click: Add 1 point
                if (event.getClick() == ClickType.LEFT) {
                    if (availablePoints > 0) {
                        statsManager.investStat(player, stat);
                        playSoundEffect(player, true);
                        player.sendMessage(ChatColor.GREEN + "You added 1 point to " + stat.name() + ".");
                    } else {
                        player.sendMessage(ChatColor.RED + "You have no skill points left!");
                    }
                }
                // Shift-left-click: Add 5 points
                else if (event.getClick() == ClickType.SHIFT_LEFT) {
                    int pointsToAdd = Math.min(5, availablePoints);
                    if (pointsToAdd > 0) {
                        for (int i = 0; i < pointsToAdd; i++) {
                            statsManager.investStat(player, stat);
                        }
                        playSoundEffect(player, true);
                        player.sendMessage(ChatColor.GREEN + "You added " + pointsToAdd + " points to " + stat.name() + ".");
                    } else {
                        player.sendMessage(ChatColor.RED + "You have no skill points left!");
                    }
                }
                // Right-click: Refund 1 point
                else if (event.getClick() == ClickType.RIGHT) {
                    if (currentStatValue > 0) {
                        statsManager.refundStat(player, stat);
                        playSoundEffect(player, false);
                        player.sendMessage(ChatColor.RED + "You refunded 1 point from " + stat.name() + ".");
                    } else {
                        player.sendMessage(ChatColor.RED + "You can't refund points you haven't invested!");
                    }
                }
                // Shift-right-click: Refund 5 points
                else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    int pointsToRefund = Math.min(5, currentStatValue);
                    if (pointsToRefund > 0) {
                        for (int i = 0; i < pointsToRefund; i++) {
                            statsManager.refundStat(player, stat);
                        }
                        playSoundEffect(player, false);
                        player.sendMessage(ChatColor.RED + "You refunded " + pointsToRefund + " points from " + stat.name() + ".");
                    } else {
                        player.sendMessage(ChatColor.RED + "You can't refund points you haven't invested!");
                    }
                }

                // Refresh the inventory to reflect updated stats
                player.openInventory(StatsInventory.getStatsMenu(player));
            }
        }
    }



    // Method to play sound effects
    private void playSoundEffect(Player player, boolean isInvesting) {
        if (isInvesting) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f); // Investing sound
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f); // Refunding sound
        }
    }
}
