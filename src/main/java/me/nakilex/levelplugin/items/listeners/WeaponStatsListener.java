package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.events.WeaponEquipEvent;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public class WeaponStatsListener implements Listener {

    private final StatsManager statsManager = StatsManager.getInstance();

    @EventHandler
    public void onWeaponEquip(WeaponEquipEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        UUID puuid = player.getUniqueId();
        StatsManager stats = StatsManager.getInstance();
        Set<Integer> equippedIds = stats.getEquippedItems(puuid);

        // 1) Remove old weapon stats by template ID
        ItemStack oldWeapon = event.getOldWeapon();
        if (oldWeapon != null && !oldWeapon.getType().isAir()) {
            int oldId = ItemUtil.getCustomItemId(oldWeapon);
            if (oldId != -1 && equippedIds.contains(oldId)) {
                CustomItem template = ItemManager.getInstance().getTemplateById(oldId);
                if (template != null) {
                    removeWeaponStats(player, template);
                    equippedIds.remove(oldId);
                }
            }
        }

        // 2) Add new weapon stats by template ID
        ItemStack newWeapon = event.getNewWeapon();
        if (newWeapon != null && !newWeapon.getType().isAir()) {
            int newId = ItemUtil.getCustomItemId(newWeapon);
            if (newId != -1 && !equippedIds.contains(newId)) {
                CustomItem template = ItemManager.getInstance().getTemplateById(newId);
                if (template != null) {
                    // Level/class checks on the template
                    int playerLevel   = LevelManager.getInstance().getLevel(player);
                    int requiredLevel = template.getLevelRequirement();
                    if (playerLevel < requiredLevel) {
                        player.sendMessage(ChatColor.RED +
                            "You can hold " + template.getBaseName() +
                            " but lack the level to gain its stats.");
                    } else {
                        // Apply template stats and track by ID
                        addWeaponStats(player, template);
                        equippedIds.add(newId);
                    }
                }
            }
        }

        // 3) Always recalc final stats
        stats.recalcDerivedStats(player);
    }


    // Helper methods (same as before)
    private void addWeaponStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat      += customItem.getHp();
        ps.bonusDefenceStat     += customItem.getDef();
        ps.bonusStrength        += customItem.getStr();
        ps.bonusAgility         += customItem.getAgi();
        ps.bonusIntelligence    += customItem.getIntel();
        ps.bonusDexterity       += customItem.getDex();
    }

    private void removeWeaponStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat      -= customItem.getHp();
        ps.bonusDefenceStat     -= customItem.getDef();
        ps.bonusStrength        -= customItem.getStr();
        ps.bonusAgility         -= customItem.getAgi();
        ps.bonusIntelligence    -= customItem.getIntel();
        ps.bonusDexterity       -= customItem.getDex();
    }
}
