package me.nakilex.levelplugin.tasks;

import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.items.ItemUtil;
import me.nakilex.levelplugin.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WeaponCheckTask extends BukkitRunnable {

    private static final Map<UUID, Integer> lastEquippedWeapon = new HashMap<>();

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            int itemId = ItemUtil.getCustomItemId(handItem);

            if (itemId != -1) {
                CustomItem cItem = ItemManager.getInstance().getItemById(itemId);
                if (cItem != null && isWeaponMaterial(cItem.getMaterial())) {

                    int playerLevel = StatsManager.getInstance().getLevel(player);
                    if (playerLevel < cItem.getLevelRequirement()) {
                        removeIfEquipped(player, itemId, cItem);
                        continue;
                    }

                    String requiredClass = (cItem.getClassRequirement() == null)
                        ? "ANY"
                        : cItem.getClassRequirement().toUpperCase();
                    StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
                    String playerClass = ps.playerClass.name();

                    if (!requiredClass.equals("ANY") && !playerClass.equals(requiredClass)) {
                        removeIfEquipped(player, itemId, cItem);
                        continue;
                    }

                    if (!Objects.equals(lastEquippedWeapon.get(player.getUniqueId()), itemId)) {
                        if (lastEquippedWeapon.containsKey(player.getUniqueId())) {
                            int oldId = lastEquippedWeapon.get(player.getUniqueId());
                            CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
                            if (oldItem != null) {
                                removeWeaponStats(player, oldItem);
                            }
                        }
                        applyWeaponStats(player, cItem);
                        lastEquippedWeapon.put(player.getUniqueId(), itemId);
                    }
                } else {
                    unequipIfPresent(player);
                }
            } else {
                unequipIfPresent(player);
            }
        }
    }

    private void unequipIfPresent(Player player) {
        if (lastEquippedWeapon.containsKey(player.getUniqueId())) {
            int oldId = lastEquippedWeapon.get(player.getUniqueId());
            CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
            if (oldItem != null) removeWeaponStats(player, oldItem);
            lastEquippedWeapon.remove(player.getUniqueId());
        }
    }

    private void removeIfEquipped(Player player, int itemId, CustomItem cItem) {
        if (lastEquippedWeapon.containsKey(player.getUniqueId())) {
            Integer oldId = lastEquippedWeapon.get(player.getUniqueId());
            if (Objects.equals(oldId, itemId)) {
                removeWeaponStats(player, cItem);
                lastEquippedWeapon.remove(player.getUniqueId());
            }
        }
    }

    private void applyWeaponStats(Player player, CustomItem cItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

        ps.bonusHealthStat += (cItem.getHp() / 2);
        ps.bonusDefenceStat += cItem.getDef();
        ps.bonusStrength += cItem.getStr();
        ps.bonusAgility += cItem.getAgi();
        ps.bonusIntelligence += cItem.getIntel();
        ps.bonusDexterity += cItem.getDex();

        StatsManager.getInstance().recalcDerivedStats(player);
    }

    private void removeWeaponStats(Player player, CustomItem cItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

        ps.bonusHealthStat -= (cItem.getHp() / 2);
        ps.bonusDefenceStat -= cItem.getDef();
        ps.bonusStrength -= cItem.getStr();
        ps.bonusAgility -= cItem.getAgi();
        ps.bonusIntelligence -= cItem.getIntel();
        ps.bonusDexterity -= cItem.getDex();

        StatsManager.getInstance().recalcDerivedStats(player);
    }

    private boolean isWeaponMaterial(Material mat) {
        if (mat == null) return false;
        if (mat.name().endsWith("_SWORD")) return true;
        if (mat == Material.BOW || mat == Material.CROSSBOW) return true;
        return false;
    }
}
