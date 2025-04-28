package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public class EquipOnJoinListener implements Listener {

    private final StatsManager statsManager      = StatsManager.getInstance();
    private final LevelManager levelManager      = LevelManager.getInstance();
    private final PlayerClassManager classManager= PlayerClassManager.getInstance();
    private final ItemManager itemManager        = ItemManager.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID puuid = player.getUniqueId();

        // Haal set van al geapplyde item-IDs
        Set<Integer> equipped = statsManager.getEquippedItems(puuid);

        // 1) Check armor slots
        applyArmorIfNeeded(player, puuid, equipped, player.getInventory().getHelmet());
        applyArmorIfNeeded(player, puuid, equipped, player.getInventory().getChestplate());
        applyArmorIfNeeded(player, puuid, equipped, player.getInventory().getLeggings());
        applyArmorIfNeeded(player, puuid, equipped, player.getInventory().getBoots());

        // 2) Check main-hand wapen
        applyWeaponIfNeeded(player, puuid, equipped, player.getInventory().getItemInMainHand());

        // (Optioneel) check off-hand:
        // applyWeaponIfNeeded(player, puuid, equipped, player.getInventory().getItemInOffHand());

        // 3) Recalculate alle afgeleide stats na het (eventueel) toevoegen
        statsManager.recalcDerivedStats(player);
    }

    private void applyArmorIfNeeded(Player player, UUID puuid, Set<Integer> equipped, ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        ArmorType type = ArmorType.matchType(item);
        if (type == null) return;

        CustomItem ci = itemManager.getCustomItemFromItemStack(item);
        if (ci == null) return;

        int id = ci.getId();
        // Als al toegepast, skip
        if (equipped.contains(id)) return;

        int reqLevel = ci.getLevelRequirement();
        int playerLevel = levelManager.getLevel(player);
        if (playerLevel < reqLevel) {
            player.sendMessage(ChatColor.RED + "You need to be Level " + reqLevel + " to wear your " + ci.getBaseName() + "!");
            return;
        }

        // Voeg stats toe
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(puuid);
        ps.bonusHealthStat   += ci.getHp();
        ps.bonusDefenceStat  += ci.getDef();
        ps.bonusStrength     += ci.getStr();
        ps.bonusAgility      += ci.getAgi();
        ps.bonusIntelligence += ci.getIntel();
        ps.bonusDexterity    += ci.getDex();

        equipped.add(id);
    }

    private void applyWeaponIfNeeded(Player player, UUID puuid, Set<Integer> equipped, ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        WeaponType type = WeaponType.matchType(item);
        CustomItem ci = itemManager.getCustomItemFromItemStack(item);
        if (type == null && ci == null) return;

        if (ci == null) return;
        int id = ci.getId();
        if (equipped.contains(id)) return;

        int reqLevel = ci.getLevelRequirement();
        int playerLevel = levelManager.getLevel(player);
        if (playerLevel < reqLevel) {
            player.sendMessage(ChatColor.RED + "You need to be Level " + reqLevel + " to wield your " + ci.getBaseName() + "!");
            return;
        }

        // Klasse-check (optioneel)
        PlayerClass requiredClass;
        try {
            requiredClass = PlayerClass.valueOf(ci.getClassRequirement().toUpperCase());
        } catch (IllegalArgumentException e) {
            requiredClass = PlayerClass.VILLAGER;
        }
        PlayerClass playerClass = statsManager.getPlayerStats(puuid).playerClass;
        if (requiredClass != PlayerClass.VILLAGER && requiredClass != playerClass) {
            player.sendMessage(ChatColor.RED + "Only " + requiredClass + "s can use " + ci.getBaseName() + "!");
            return;
        }

        // Voeg stats toe
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(puuid);
        ps.bonusHealthStat   += ci.getHp();
        ps.bonusDefenceStat  += ci.getDef();
        ps.bonusStrength     += ci.getStr();
        ps.bonusAgility      += ci.getAgi();
        ps.bonusIntelligence += ci.getIntel();
        ps.bonusDexterity    += ci.getDex();

        equipped.add(id);
    }
}
