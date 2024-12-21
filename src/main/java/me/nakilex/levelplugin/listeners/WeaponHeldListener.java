package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles weapon stat bonuses when switching held items.
 */
public class WeaponHeldListener implements Listener {

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Get the items in the old and new slots
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        // Remove bonuses from the old item if it's valid
        if (oldItem != null) {
            CustomItem oldCustomItem = ItemManager.getInstance().getCustomItemFromItemStack(oldItem);
            if (oldCustomItem != null) {
                removeWeaponStats(player, oldCustomItem);
            }
        }

        // Apply bonuses for the new item if it's valid
        if (newItem != null) {
            CustomItem newCustomItem = ItemManager.getInstance().getCustomItemFromItemStack(newItem);
            if (newCustomItem != null) {
                applyWeaponStats(player, newCustomItem);
            }
        }

        // Debug log
        Bukkit.getLogger().info("[WeaponHeldListener] " + player.getName() +
            " switched from slot " + event.getPreviousSlot() +
            " to slot " + event.getNewSlot());
    }

    /**
     * Applies weapon stats as bonus stats.
     */
    private void applyWeaponStats(Player player, CustomItem item) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        ps.bonusStrength += item.getStr();
        ps.bonusAgility += item.getAgi();
        ps.bonusIntelligence += item.getIntel();
        ps.bonusDexterity += item.getDex();
        ps.bonusHealthStat += item.getHp();
        ps.bonusDefenceStat += item.getDef();
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    /**
     * Removes weapon stats from bonus stats.
     */
    private void removeWeaponStats(Player player, CustomItem item) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        ps.bonusStrength -= item.getStr();
        ps.bonusAgility -= item.getAgi();
        ps.bonusIntelligence -= item.getIntel();
        ps.bonusDexterity -= item.getDex();
        ps.bonusHealthStat -= item.getHp();
        ps.bonusDefenceStat -= item.getDef();
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
