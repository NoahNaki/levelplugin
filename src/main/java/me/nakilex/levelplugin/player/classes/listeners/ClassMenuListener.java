package me.nakilex.levelplugin.player.classes.listeners;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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
            case "START AS A WARRIOR!": selectedClass = PlayerClass.WARRIOR; className = "Warrior"; break;
            case "START AS AN ARCHER!": selectedClass = PlayerClass.ARCHER;  className = "Archer";  break;
            case "START AS A MAGE!":    selectedClass = PlayerClass.MAGE;    className = "Mage";    break;
            case "START AS A ROGUE!":   selectedClass = PlayerClass.ROGUE;   className = "Rogue";   break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid class selection.");
                return;
        }

        if (selectedClass != null) {
            UUID puuid = player.getUniqueId();

            // ✅ Set class directly into StatsManager like old version
            StatsManager.getInstance().getPlayerStats(puuid).playerClass = selectedClass;

            boolean canDJ = (selectedClass == PlayerClass.ARCHER || selectedClass == PlayerClass.ROGUE);
            player.setAllowFlight(canDJ);
            if (!canDJ) {
                player.setFlying(false);
            }

            player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Class Selection" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GREEN + "You have selected " + ChatColor.AQUA + className + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.closeInventory();
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

        // 🐛 DEBUGGING OUTPUT
//        player.sendMessage(ChatColor.YELLOW + "[DEBUG] Your class: " + currentClass);
//        player.sendMessage(ChatColor.YELLOW + "[DEBUG] Weapon requires: " + reqClass);
//        player.sendMessage(ChatColor.YELLOW + "[DEBUG] Weapon name: " + ci.getBaseName());
//        player.sendMessage(ChatColor.YELLOW + "[DEBUG] Raw class requirement string: " + clsReqRaw);
//        player.sendMessage(ChatColor.YELLOW + "[DEBUG] Player Level: " + playerLevel + " / Required Level: " + reqLevel);

        boolean meetsClassReq = (reqClass == PlayerClass.VILLAGER || reqClass == currentClass);
        boolean meetsLevelReq = (playerLevel >= reqLevel);
        boolean wasApplied = equipped.contains(id);

        if (wasApplied && (!meetsClassReq || !meetsLevelReq)) {
            removeWeaponStats(player, ci);
            equipped.remove(id);
            player.sendMessage(ChatColor.RED + "You no longer meet the requirements for " + ci.getBaseName() + "!");
        } else if (!wasApplied && meetsClassReq && meetsLevelReq) {
            addWeaponStats(player, ci);
            equipped.add(id);
            player.sendMessage(ChatColor.GREEN + "Stats applied for " + ci.getBaseName() + "!");
        }

        statsMgr.recalcDerivedStats(player);
    }


    /**
     * Updates the tooltip of every item in the player's inventory and armor slots.
     */
    private void refreshInventoryTooltips(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            updateTooltipSafely(stack, player);
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            updateTooltipSafely(armor, player);
        }
    }

    private void updateTooltipSafely(ItemStack item, Player player) {
        if (item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING)) {
            ItemUtil.updateCustomItemTooltip(item, player);
        }
    }

    private void addWeaponStats(Player player, CustomItem ci) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat   += ci.getHp();
        ps.bonusDefenceStat  += ci.getDef();
        ps.bonusStrength     += ci.getStr();
        ps.bonusAgility      += ci.getAgi();
        ps.bonusIntelligence += ci.getIntel();
        ps.bonusDexterity    += ci.getDex();
    }

    private void removeWeaponStats(Player player, CustomItem ci) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        ps.bonusHealthStat   -= ci.getHp();
        ps.bonusDefenceStat  -= ci.getDef();
        ps.bonusStrength     -= ci.getStr();
        ps.bonusAgility      -= ci.getAgi();
        ps.bonusIntelligence -= ci.getIntel();
        ps.bonusDexterity    -= ci.getDex();
    }
}
