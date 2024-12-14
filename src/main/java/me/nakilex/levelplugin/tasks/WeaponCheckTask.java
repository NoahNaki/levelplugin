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

/**
 * A repeating task that ensures weapon stats are always up-to-date,
 * including level/class requirement checks.
 */
public class WeaponCheckTask extends BukkitRunnable {

    /**
     * Map tracking which weapon ID each player currently has "equipped" in their main hand.
     * Key = Player UUID, Value = weapon ID (int)
     */
    private static final Map<UUID, Integer> lastEquippedWeapon = new HashMap<>();

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            int itemId = ItemUtil.getCustomItemId(handItem);

            if (itemId != -1) {
                // We have some custom item in main hand. Check if it's a valid weapon
                CustomItem cItem = ItemManager.getInstance().getItemById(itemId);
                if (cItem != null && isWeaponMaterial(cItem.getMaterial())) {

                    // #### LEVEL CHECK ####
                    int playerLevel = StatsManager.getInstance().getLevel(player);
                    if (playerLevel < cItem.getLevelRequirement()) {
                        Bukkit.getLogger().info("[WeaponCheckTask] " + player.getName()
                            + " doesn't meet level requirement for " + cItem.getName()
                            + " (ReqLv=" + cItem.getLevelRequirement() + ", PlayerLv=" + playerLevel + ")");
                        removeIfEquipped(player, itemId, cItem);
                        continue; // skip applying
                    }

                    // #### CLASS CHECK ####
                    String requiredClass = (cItem.getClassRequirement() == null)
                        ? "ANY"
                        : cItem.getClassRequirement().toUpperCase();
                    StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
                    String playerClass = ps.playerClass.name(); // e.g. "WARRIOR", "ROGUE"

                    if (!requiredClass.equals("ANY") && !playerClass.equals(requiredClass)) {
                        Bukkit.getLogger().info("[WeaponCheckTask] " + player.getName()
                            + " doesn't meet class req for " + cItem.getName()
                            + ". Required=" + requiredClass + ", actual=" + playerClass);
                        removeIfEquipped(player, itemId, cItem);
                        continue;
                    }

                    // #### If level & class checks pass, apply if not already ####
                    if (!Objects.equals(lastEquippedWeapon.get(player.getUniqueId()), itemId)) {
                        // Remove previously equipped weapon stats
                        if (lastEquippedWeapon.containsKey(player.getUniqueId())) {
                            int oldId = lastEquippedWeapon.get(player.getUniqueId());
                            CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
                            if (oldItem != null) {
                                removeWeaponStats(player, oldItem);
                            }
                        }
                        // Apply the new weapon
                        applyWeaponStats(player, cItem);
                        lastEquippedWeapon.put(player.getUniqueId(), itemId);
                    }
                } else {
                    // The item in main hand is not a valid custom weapon
                    unequipIfPresent(player);
                }
            } else {
                // Main hand is empty or non-custom
                unequipIfPresent(player);
            }
        }
    }

    /**
     * If the player had a previously-equipped weapon, remove it from stats & the map.
     */
    private void unequipIfPresent(Player player) {
        if (lastEquippedWeapon.containsKey(player.getUniqueId())) {
            int oldId = lastEquippedWeapon.get(player.getUniqueId());
            CustomItem oldItem = ItemManager.getInstance().getItemById(oldId);
            if (oldItem != null) removeWeaponStats(player, oldItem);
            lastEquippedWeapon.remove(player.getUniqueId());
        }
    }

    /**
     * Remove stats if it was previously equipped for this itemId.
     */
    private void removeIfEquipped(Player player, int itemId, CustomItem cItem) {
        if (lastEquippedWeapon.containsKey(player.getUniqueId())) {
            Integer oldId = lastEquippedWeapon.get(player.getUniqueId());
            if (Objects.equals(oldId, itemId)) {
                // If they're currently "equipped", remove
                removeWeaponStats(player, cItem);
                lastEquippedWeapon.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Applies the stats for the given weapon to the player.
     */
    private void applyWeaponStats(Player player, CustomItem cItem) {
        Bukkit.getLogger().info("[WeaponCheckTask] Applying weapon stats for " + cItem.getName());
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

        ps.healthStat += (cItem.getHp() / 2);
        ps.defenceStat += cItem.getDef();
        ps.strength += cItem.getStr();
        ps.agility += cItem.getAgi();
        ps.intelligence += cItem.getIntel();
        ps.dexterity += cItem.getDex();

        StatsManager.getInstance().recalcDerivedStats(player);
        Bukkit.getLogger().info("[WeaponCheckTask] Stats after equip => "
            + "HPstat=" + ps.healthStat + ", STR=" + ps.strength
            + ", DEF=" + ps.defenceStat + ", AGI=" + ps.agility
            + ", INT=" + ps.intelligence + ", DEX=" + ps.dexterity);
    }

    /**
     * Removes the stats for the given weapon from the player.
     */
    private void removeWeaponStats(Player player, CustomItem cItem) {
        Bukkit.getLogger().info("[WeaponCheckTask] Removing weapon stats for " + cItem.getName());
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

        ps.healthStat -= (cItem.getHp() / 2);
        ps.defenceStat -= cItem.getDef();
        ps.strength -= cItem.getStr();
        ps.agility -= cItem.getAgi();
        ps.intelligence -= cItem.getIntel();
        ps.dexterity -= cItem.getDex();

        StatsManager.getInstance().recalcDerivedStats(player);
        Bukkit.getLogger().info("[WeaponCheckTask] Stats after removal => "
            + "HPstat=" + ps.healthStat + ", STR=" + ps.strength
            + ", DEF=" + ps.defenceStat + ", AGI=" + ps.agility
            + ", INT=" + ps.intelligence + ", DEX=" + ps.dexterity);
    }

    /**
     * Check if a Material is considered a weapon. Expand as needed.
     */
    private boolean isWeaponMaterial(Material mat) {
        if (mat == null) return false;
        if (mat.name().endsWith("_SWORD")) return true;
        if (mat == Material.BOW || mat == Material.CROSSBOW) return true;
        return false;
    }
}
