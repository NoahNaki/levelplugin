package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.items.ItemUtil;
import me.nakilex.levelplugin.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ArmorEquipListener implements Listener {

    private static final Map<UUID, Set<Integer>> equippedArmors = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack oldArmor = event.getCurrentItem();
            ItemStack newArmor = event.getCursor();

            handleArmorChange(player, oldArmor, newArmor);
        }
    }

    private void handleArmorChange(Player player, ItemStack oldArmor, ItemStack newArmor) {
        // Remove stats from old armor
        if (oldArmor != null) {
            CustomItem oldItem = ItemManager.getInstance().getCustomItemFromItemStack(oldArmor);
            if (oldItem != null && isArmorMaterial(oldItem.getMaterial())) {
                removeArmorStats(player, oldItem);
                equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).remove(oldItem.getId());
            }
        }

        // Apply stats from new armor
        if (newArmor != null) {
            CustomItem newItem = ItemManager.getInstance().getCustomItemFromItemStack(newArmor);
            if (newItem != null && isArmorMaterial(newItem.getMaterial())) {
                int playerLevel = StatsManager.getInstance().getLevel(player);
                if (playerLevel < newItem.getLevelRequirement()) {
                    player.sendMessage("§cYou are not high enough level to equip this item!");
                    return;
                }
                applyArmorStats(player, newItem);
                equippedArmors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(newItem.getId());
            }
        }
    }

    private boolean isArmorMaterial(Material mat) {
        return mat.name().endsWith("_HELMET")
            || mat.name().endsWith("_CHESTPLATE")
            || mat.name().endsWith("_LEGGINGS")
            || mat.name().endsWith("_BOOTS");
    }

    private void applyArmorStats(Player player, CustomItem cItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        ps.bonusHealthStat += (cItem.getHp());
        ps.bonusDefenceStat += cItem.getDef();
        ps.bonusStrength += cItem.getStr();
        ps.bonusAgility += cItem.getAgi();
        ps.bonusIntelligence += cItem.getIntel();
        ps.bonusDexterity += cItem.getDex();

        StatsManager.getInstance().recalcDerivedStats(player);
    }

    private void removeArmorStats(Player player, CustomItem cItem) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        ps.bonusHealthStat -= (cItem.getHp());
        ps.bonusDefenceStat -= cItem.getDef();
        ps.bonusStrength -= cItem.getStr();
        ps.bonusAgility -= cItem.getAgi();
        ps.bonusIntelligence -= cItem.getIntel();
        ps.bonusDexterity -= cItem.getDex();

        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
