package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.events.ArmorEquipEvent;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
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
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        UUID puuid = player.getUniqueId();
        StatsManager stats = StatsManager.getInstance();
        Set<Integer> equippedIds = stats.getEquippedItems(puuid);

        // 1) Unequip old item
        ItemStack oldItem = event.getOldArmorPiece();
        if (oldItem != null && !oldItem.getType().isAir()) {
            int oldId = ItemUtil.getCustomItemId(oldItem);
            if (oldId != -1 && equippedIds.contains(oldId)) {
                CustomItem oldTemplate = ItemManager.getInstance().getTemplateById(oldId);
                if (oldTemplate != null) {
                    removeItemStats(player, oldTemplate);
                    equippedIds.remove(oldId);
                }
            }
        }

        // 2) Equip new item (level check)
        ItemStack newItem = event.getNewArmorPiece();
        if (newItem != null && !newItem.getType().isAir()) {
            int newId = ItemUtil.getCustomItemId(newItem);
            if (newId != -1 && !equippedIds.contains(newId)) {
                CustomItem newTemplate = ItemManager.getInstance().getTemplateById(newId);
                if (newTemplate != null) {
                    int req = newTemplate.getLevelRequirement();
                    int lvl = LevelManager.getInstance().getLevel(player);
                    if (lvl < req) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You must be Level " + req + " to wear this armor!");
                        return;
                    }
                    addItemStats(player, newTemplate);
                    equippedIds.add(newId);
                }
            }
        }

        // 3) Recalc
        stats.recalcDerivedStats(player);
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
