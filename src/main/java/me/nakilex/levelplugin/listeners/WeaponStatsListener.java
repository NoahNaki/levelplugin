package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.events.WeaponEquipEvent;
import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.managers.StatsManager;
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
        // 1) If event was cancelled by something else, do nothing
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 2) Remove old weapon stats if the old item was truly equipped
        ItemStack oldWeapon = event.getOldWeapon();
        if (oldWeapon != null && !oldWeapon.getType().isAir()) {
            CustomItem oldCustom = ItemManager.getInstance().getCustomItemFromItemStack(oldWeapon);
            if (oldCustom != null) {
                Set<Integer> eqSet = StatsManager.getInstance().getEquippedItems(uuid);
                if (eqSet.contains(oldCustom.getId())) {
                    // We only remove stats if they were actually added
                    removeWeaponStats(player, oldCustom);
                    eqSet.remove(oldCustom.getId());
                }
            }
        }

        // 3) Attempt to add new weapon stats, but only if not already equipped
        ItemStack newWeapon = event.getNewWeapon();
        if (newWeapon != null && !newWeapon.getType().isAir()) {
            CustomItem newCustom = ItemManager.getInstance().getCustomItemFromItemStack(newWeapon);
            if (newCustom != null) {
                StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
                Set<Integer> eqSet = StatsManager.getInstance().getEquippedItems(uuid);

                // Check level/class requirements here (if you skip them, remove this)
                int playerLevel = StatsManager.getInstance().getLevel(player);
                int requiredLevel = newCustom.getLevelRequirement();
                boolean meetsLevel = (playerLevel >= requiredLevel);

                String reqClass = newCustom.getClassRequirement();
                boolean meetsClass = reqClass.equalsIgnoreCase("ANY")
                    || ps.playerClass.name().equalsIgnoreCase(reqClass);

                if (!meetsLevel) {
                    player.sendMessage(ChatColor.RED +
                        "You can hold " + newCustom.getBaseName() + " but lack the level to gain its stats.");
                }
                else if (!meetsClass) {
                    player.sendMessage(ChatColor.RED +
                        "You can hold " + newCustom.getBaseName() + " but your class can’t use its stats.");
                }
                else {
                    // If we haven’t already equipped this item, add its stats
                    if (!eqSet.contains(newCustom.getId())) {
                        addWeaponStats(player, newCustom);
                        eqSet.add(newCustom.getId());
                    } else {
                        // If it's already in the set, skip adding again
                        Bukkit.getLogger().info("[DEBUG] Skipping double-add for item ID=" + newCustom.getId());
                    }
                }
            }
        }

        // 4) Always recalc final stats
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    // Helper methods (same as before)
    private void addWeaponStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        ps.bonusHealthStat      += customItem.getHp();
        ps.bonusDefenceStat     += customItem.getDef();
        ps.bonusStrength        += customItem.getStr();
        ps.bonusAgility         += customItem.getAgi();
        ps.bonusIntelligence    += customItem.getIntel();
        ps.bonusDexterity       += customItem.getDex();
    }

    private void removeWeaponStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        ps.bonusHealthStat      -= customItem.getHp();
        ps.bonusDefenceStat     -= customItem.getDef();
        ps.bonusStrength        -= customItem.getStr();
        ps.bonusAgility         -= customItem.getAgi();
        ps.bonusIntelligence    -= customItem.getIntel();
        ps.bonusDexterity       -= customItem.getDex();
    }
}
