package me.nakilex.levelplugin.player.classes.listeners;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Set;
import java.util.UUID;

public class ClassMenuListener implements Listener {

    @EventHandler
    public void onClassMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!"Choose Your Class".equalsIgnoreCase(title)) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;

        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        PlayerClass selectedClass = null;
        String className = null;

        switch (displayName.toUpperCase()) {
            case "START AS A BARBARIAN!": selectedClass = PlayerClass.BARBARIAN; className = "Barbarian"; break;
            case "START AS A WARRIOR!": selectedClass = PlayerClass.WARRIOR; className = "Warrior"; break;
            case "START AS AN ARCHER!": selectedClass = PlayerClass.ARCHER;  className = "Archer";  break;
            case "START AS A PHOENIXHUNTER!": selectedClass = PlayerClass.PHOENIXHUNTER; className = "PhoenixHunter"; break;
            case "START AS A MAGE!":    selectedClass = PlayerClass.MAGE;    className = "Mage";    break;
            case "START AS A PALADIN!": selectedClass = PlayerClass.PALADIN; className = "Paladin"; break;
            case "START AS A ROGUE!":   selectedClass = PlayerClass.ROGUE;   className = "Rogue";   break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid class selection.");
                return;
        }

        if (selectedClass != null) {
            UUID puuid = player.getUniqueId();

            int level = LevelManager.getInstance().getLevel(player);

            if (level < selectedClass.getRequiredLevel()) {
                player.sendMessage(ChatColor.RED + "You must reach level " + selectedClass.getRequiredLevel() + " to select this class.");
                return;
            }

            PlayerClass current = StatsManager.getInstance().getPlayerStats(puuid).playerClass;
            int cost = current == PlayerClass.VILLAGER ? 0 : level * 50;
            if (cost > 0) {
                try {
                    me.nakilex.levelplugin.Main.getInstance().getEconomyManager().deductCoins(player, cost);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage(ChatColor.RED + "Not enough coins! Cost: " + ChatColor.GOLD + "⛃ " + cost);
                    return;
                }
            }

            StatsManager.getInstance().getPlayerStats(puuid).playerClass = selectedClass;

            boolean canDJ = (selectedClass == PlayerClass.ARCHER
                    || selectedClass == PlayerClass.ROGUE
                    || selectedClass == PlayerClass.DEADEYE
                    || selectedClass == PlayerClass.PHOENIXHUNTER);
            player.setAllowFlight(canDJ);
            if (!canDJ) {
                player.setFlying(false);
            }

            ChatFormatter.constructDivider(player, "§6§l-", 45);
            ChatFormatter.sendCenteredMessage(player, "§6§lCLASS SELECTED!");
            ChatFormatter.sendCenteredMessage(player, "");
            ChatFormatter.sendCenteredMessage(player,
                "§7You are now the §e§l" + className + " §7class!");
            ChatFormatter.sendCenteredMessage(player, "");
            ChatFormatter.constructDivider(player, "§6§l-", 45);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.closeInventory();
            me.nakilex.levelplugin.Main.getInstance().getQuestManager().handleClassSelect(player);
        }

        // After setting class, handle weapon stats and refresh tooltips
        handleWeaponStatAdjustment(player);
        refreshInventoryTooltips(player);
        player.updateInventory();
    }



    /**
     * Handles applying or removing weapon stats after a class change.
     */
    private void handleWeaponStatAdjustment(Player player) {
        UUID puuid = player.getUniqueId();
        StatsManager statsMgr = StatsManager.getInstance();
        Set<Integer> equipped = statsMgr.getEquippedItems(puuid);
        int playerLevel = LevelManager.getInstance().getLevel(player);

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) return;

        CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(weapon);
        WeaponType wt = WeaponType.matchType(weapon);
        if (ci == null || wt == null) return;

        int id = ci.getId();
        int reqLevel = ci.getLevelRequirement();
        String clsReqRaw = ci.getClassRequirement();

        PlayerClass reqClass;
        try {
            reqClass = PlayerClass.valueOf(clsReqRaw.toUpperCase());
        } catch (IllegalArgumentException e) {
            reqClass = PlayerClass.VILLAGER;
        }

        PlayerClass currentClass = StatsManager.getInstance().getPlayerStats(puuid).playerClass;


        boolean meetsClassReq = (reqClass == PlayerClass.VILLAGER || reqClass == currentClass);
        boolean meetsLevelReq = (playerLevel >= reqLevel);
        boolean wasApplied = equipped.contains(id);

        if (wasApplied && (!meetsClassReq || !meetsLevelReq)) {
            removeWeaponStats(player, ci, weapon);
            equipped.remove(id);
            player.sendMessage(ChatColor.RED + "You no longer meet the requirements for " + ci.getBaseName() + "!");
        } else if (!wasApplied && meetsClassReq && meetsLevelReq) {
            addWeaponStats(player, ci, weapon);
            equipped.add(id);
            player.sendMessage(ChatColor.GREEN + "Stats applied for " + ci.getBaseName() + "!");
        }

        statsMgr.recalcDerivedStats(player);
    }


    /**
     * Updates the tooltip of every item in the player's inventory and armor slots.
     */
    private void refreshInventoryTooltips(Player player) {
        me.nakilex.levelplugin.items.utils.ItemUtil.refreshTooltips(player);
    }


    private void addWeaponStats(Player player, CustomItem ci, ItemStack stack) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        me.nakilex.levelplugin.ego.EgoRarity rarity = me.nakilex.levelplugin.ego.EgoRarity.COMMON;
        int rank = 1;
        if (stack.hasItemMeta()) {
            PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
            if (pdc.has(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING)) {
                try { rarity = me.nakilex.levelplugin.ego.EgoRarity.valueOf(pdc.get(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING)); } catch (Exception ignored) {}
            }
            rank = pdc.getOrDefault(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
        }
        ps.bonusHealthStat   += ItemUtil.scaleEgoStat(ci.getHp(), rarity, rank);
        ps.bonusDefenceStat  += ItemUtil.scaleEgoStat(ci.getDef(), rarity, rank);
        ps.bonusStrength     += ItemUtil.scaleEgoStat(ci.getStr(), rarity, rank);
        ps.bonusAgility      += ItemUtil.scaleEgoStat(ci.getAgi(), rarity, rank);
        ps.bonusIntelligence += ItemUtil.scaleEgoStat(ci.getIntel(), rarity, rank);
        ps.bonusDexterity    += ItemUtil.scaleEgoStat(ci.getDex(), rarity, rank);
    }

    private void removeWeaponStats(Player player, CustomItem ci, ItemStack stack) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        me.nakilex.levelplugin.ego.EgoRarity rarity = me.nakilex.levelplugin.ego.EgoRarity.COMMON;
        int rank = 1;
        if (stack.hasItemMeta()) {
            PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
            if (pdc.has(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING)) {
                try { rarity = me.nakilex.levelplugin.ego.EgoRarity.valueOf(pdc.get(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING)); } catch (Exception ignored) {}
            }
            rank = pdc.getOrDefault(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
        }
        ps.bonusHealthStat   -= ItemUtil.scaleEgoStat(ci.getHp(), rarity, rank);
        ps.bonusDefenceStat  -= ItemUtil.scaleEgoStat(ci.getDef(), rarity, rank);
        ps.bonusStrength     -= ItemUtil.scaleEgoStat(ci.getStr(), rarity, rank);
        ps.bonusAgility      -= ItemUtil.scaleEgoStat(ci.getAgi(), rarity, rank);
        ps.bonusIntelligence -= ItemUtil.scaleEgoStat(ci.getIntel(), rarity, rank);
        ps.bonusDexterity    -= ItemUtil.scaleEgoStat(ci.getDex(), rarity, rank);
    }
}
