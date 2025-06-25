package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.UUID;

public class EquipOnJoinListener implements Listener {

    private final StatsManager statsManager      = StatsManager.getInstance();
    private final LevelManager levelManager      = LevelManager.getInstance();
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

        // Load Ego weapon from the item in main hand so XP gains apply
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.hasItemMeta()) {
            PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
            if (pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) {
                String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
                String key = id.split("_")[0];
                me.nakilex.levelplugin.ego.EgoWeapon proto = me.nakilex.levelplugin.ego.EgoWeaponManager.getInstance().getPrototype(key);
                if (proto != null) {
                    me.nakilex.levelplugin.ego.EgoWeapon weapon = proto.copy();
                    int rank = pdc.getOrDefault(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
                    int exp = pdc.getOrDefault(ItemUtil.EGO_EXP_KEY, PersistentDataType.INTEGER, 0);
                    while (weapon.getRank() < rank) weapon.addExp(weapon.expToNextRank());
                    weapon.addExp(exp);
                    me.nakilex.levelplugin.ego.EgoWeaponManager.getInstance().setWeapon(puuid, weapon);
                }
            }
        }
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

        // Skip applying weapon stats if the item is actually armor being held
        // in the player's hand. This prevents armor bonuses from activating
        // merely by logging in while holding an armor piece.
        if (ArmorType.matchType(item) != null) return;

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
