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
        Set<Integer> equipped = stats.getEquippedItems(puuid);

        // 1) Unequip old piece
        ItemStack oldItem = event.getOldArmorPiece();
        if (oldItem != null && !oldItem.getType().isAir()) {
            CustomItem inst = ItemManager.getInstance().getCustomItemFromItemStack(oldItem);
            if (inst != null && equipped.contains(inst.getId())) {
                removeItemStats(player, inst);
                equipped.remove(inst.getId());
            }
        }

        // 2) Equip new piece
        ItemStack newItem = event.getNewArmorPiece();
        if (newItem != null && !newItem.getType().isAir()) {
            CustomItem inst = ItemManager.getInstance().getCustomItemFromItemStack(newItem);
            if (inst != null && !equipped.contains(inst.getId())) {
                int req   = inst.getLevelRequirement();
                int level = LevelManager.getInstance().getLevel(player);
                if (level < req) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You must be Level " + req + " to wear this armor!");
                    return;
                }

                // ← ADDED: If the armor is broken, do not apply stats
                if (inst.isBroken()) {
                    player.sendMessage(ChatColor.RED + "This armor is broken and grants no bonuses.");
                } else {
                    addItemStats(player, inst);
                    equipped.add(inst.getId());
                }
            }
        }

        // 3) Recalculate derived stats (HP/mana will update whether or not we added stats)
        stats.recalcDerivedStats(player);

        // 4) Update armor set bonuses after the inventory update (next tick)
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                me.nakilex.levelplugin.Main.getInstance(),
                () -> me.nakilex.levelplugin.items.managers.SetBonusManager.getInstance().updatePlayer(player),
                1L);
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

    public void removeItemStats(Player player, CustomItem customItem) {
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat   = Math.max(0, ps.bonusHealthStat   - customItem.getHp());
        ps.bonusDefenceStat  = Math.max(0, ps.bonusDefenceStat  - customItem.getDef());
        ps.bonusStrength     = Math.max(0, ps.bonusStrength     - customItem.getStr());
        ps.bonusAgility      = Math.max(0, ps.bonusAgility      - customItem.getAgi());
        ps.bonusIntelligence = Math.max(0, ps.bonusIntelligence - customItem.getIntel());
        ps.bonusDexterity    = Math.max(0, ps.bonusDexterity    - customItem.getDex());

        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
