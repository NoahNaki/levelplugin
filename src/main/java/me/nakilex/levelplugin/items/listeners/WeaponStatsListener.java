package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public class WeaponStatsListener implements Listener {

    private final StatsManager statsManager = StatsManager.getInstance();
    private final PlayerClassManager classManager = PlayerClassManager.getInstance();

    @EventHandler
    public void onWeaponEquip(WeaponEquipEvent event) {
        // 0) ignore canceled or non-weapon events
        if (event.isCancelled()) return;
        if (WeaponType.matchType(event.getOldWeapon()) == null
            && WeaponType.matchType(event.getNewWeapon()) == null) {
            return;
        }

        Player player = event.getPlayer();
        UUID puuid = player.getUniqueId();
        StatsManager stats = StatsManager.getInstance();
        Set<Integer> equipped = stats.getEquippedItems(puuid);

        // 1) Remove old weapon stats
        ItemStack oldWeap = event.getOldWeapon();
        if (oldWeap != null && !oldWeap.getType().isAir()) {
            CustomItem inst = ItemManager.getInstance().getCustomItemFromItemStack(oldWeap);
            if (inst != null && equipped.contains(inst.getId())) {
                removeWeaponStats(player, inst);
                equipped.remove(inst.getId());
            }
        }

        // 2) Add new weapon stats with level and class requirement check
        ItemStack newWeap = event.getNewWeapon();
        if (newWeap != null && !newWeap.getType().isAir()) {
            CustomItem inst = ItemManager.getInstance().getCustomItemFromItemStack(newWeap);
            if (inst != null && !equipped.contains(inst.getId())) {
                int playerLevel = LevelManager.getInstance().getLevel(player);
                int requiredLevel = inst.getLevelRequirement();
                PlayerClass requiredClass;

                try {
                    requiredClass = PlayerClass.valueOf(inst.getClassRequirement().toUpperCase());
                } catch (IllegalArgumentException e) {
                    requiredClass = PlayerClass.VILLAGER;
                }

                PlayerClass playerClass = StatsManager.getInstance()
                    .getPlayerStats(puuid).playerClass;

                if (playerLevel < requiredLevel) {
                    player.sendMessage(ChatColor.RED + "You can hold "
                        + inst.getBaseName() + " but lack the level to gain its stats.");
                } else if (requiredClass != PlayerClass.VILLAGER
                    && requiredClass != playerClass) {
                    player.sendMessage(ChatColor.RED + "You can hold "
                        + inst.getBaseName() + " but lack the required class to gain its stats.");
                } else {
                    addWeaponStats(player, inst);
                    equipped.add(inst.getId());
                }
            }
        }

        // 3) Recalculate derived stats
        stats.recalcDerivedStats(player);
    }

    private void addWeaponStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat += customItem.getHp();
        ps.bonusDefenceStat += customItem.getDef();
        ps.bonusStrength += customItem.getStr();
        ps.bonusAgility += customItem.getAgi();
        ps.bonusIntelligence += customItem.getIntel();
        ps.bonusDexterity += customItem.getDex();
    }

    private void removeWeaponStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat -= customItem.getHp();
        ps.bonusDefenceStat -= customItem.getDef();
        ps.bonusStrength -= customItem.getStr();
        ps.bonusAgility -= customItem.getAgi();
        ps.bonusIntelligence -= customItem.getIntel();
        ps.bonusDexterity -= customItem.getDex();
    }
}