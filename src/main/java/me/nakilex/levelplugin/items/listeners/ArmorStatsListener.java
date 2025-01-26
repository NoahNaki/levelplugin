package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.events.ArmorEquipEvent;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public class ArmorStatsListener implements Listener {

    private final StatsManager statsManager = StatsManager.getInstance();

    @EventHandler
    public void onArmorEquip(ArmorEquipEvent event) {
        // If the event is cancelled by something else, do nothing
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // ----- 1) Unequip Old Item -----
        ItemStack oldItem = event.getOldArmorPiece();
        if (oldItem != null && !oldItem.getType().isAir()) {
            CustomItem oldCustomItem = ItemManager.getInstance().getCustomItemFromItemStack(oldItem);
            if (oldCustomItem != null) {
                // Only remove if we had previously added it
                Set<Integer> equippedIds = statsManager.getEquippedItems(uuid);
                if (equippedIds.contains(oldCustomItem.getId())) {
                    // Remove stats
                    removeItemStats(player, oldCustomItem);
                    // Remove from the set
                    equippedIds.remove(oldCustomItem.getId());
                }
            }
        }

        // ----- 2) Attempt to Equip New Item (with level check) -----
        ItemStack newItem = event.getNewArmorPiece();
        if (newItem != null && !newItem.getType().isAir()) {
            CustomItem newCustomItem = ItemManager.getInstance().getCustomItemFromItemStack(newItem);
            if (newCustomItem != null) {
                int requiredLevel = newCustomItem.getLevelRequirement();
                int playerLevel = LevelManager.getInstance().getLevel(player);

                if (playerLevel < requiredLevel) {
                    // Cancel so the item doesn't stay equipped
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You must be Level "
                        + requiredLevel + " to wear this armor!");
                    return; // Stop, do not add stats
                }

                // (Optional) class check for armor, if you do that
                // ...

                // If we pass checks, apply stats
                addItemStats(player, newCustomItem);

                // Add to the set of "equipped" item IDs
                statsManager.getEquippedItems(uuid).add(newCustomItem.getId());
            }
        }

        // ----- 3) Recalculate final stats -----
        statsManager.recalcDerivedStats(player);
    }

    private void addItemStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat   += customItem.getHp();
        ps.bonusDefenceStat  += customItem.getDef();
        ps.bonusStrength     += customItem.getStr();
        ps.bonusAgility      += customItem.getAgi();
        ps.bonusIntelligence += customItem.getIntel();
        ps.bonusDexterity    += customItem.getDex();
    }

    private void removeItemStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat   -= customItem.getHp();
        ps.bonusDefenceStat  -= customItem.getDef();
        ps.bonusStrength     -= customItem.getStr();
        ps.bonusAgility      -= customItem.getAgi();
        ps.bonusIntelligence -= customItem.getIntel();
        ps.bonusDexterity    -= customItem.getDex();
    }
}
