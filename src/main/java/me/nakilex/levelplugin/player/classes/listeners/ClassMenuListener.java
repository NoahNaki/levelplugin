package me.nakilex.levelplugin.player.classes.listeners;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
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

        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.DARK_GREEN + "Choose Your Class")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        String disp = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        PlayerClass selected = null;
        String name = null;

        switch (disp.toUpperCase()) {
            case "START AS A WARRIOR!":
                selected = PlayerClass.WARRIOR; name = "Warrior"; break;
            case "START AS AN ARCHER!":
                selected = PlayerClass.ARCHER;  name = "Archer";  break;
            case "START AS A MAGE!":
                selected = PlayerClass.MAGE;    name = "Mage";    break;
            case "START AS A ROGUE!":
                selected = PlayerClass.ROGUE;   name = "Rogue";   break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid class selection.");
                return;
        }

        // Oude klasse voor debug
        UUID puuid = player.getUniqueId();
        PlayerClass oldClass = StatsManager.getInstance().getPlayerStats(puuid).playerClass;

        // Set nieuwe klasse
        PlayerClassManager.getInstance().setPlayerClass(player, selected);

        player.sendMessage(
            ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Class Selection" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GREEN + "You have selected " + ChatColor.AQUA + name + ChatColor.GREEN + "!"
        );
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.closeInventory();

        // Nu: wapen in main-hand aanpassen op nieuwe klasserequirement
        handleWeaponOnClassChange(player, oldClass, selected);

        // Update tooltips en inventory
        player.getInventory().forEach(stack -> {
            if (stack != null && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING)) {
                ItemUtil.updateCustomItemTooltip(stack, player);
            }
        });
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.hasItemMeta()
                && armor.getItemMeta().getPersistentDataContainer().has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING)) {
                ItemUtil.updateCustomItemTooltip(armor, player);
            }
        }
        player.updateInventory();
    }

    private void handleWeaponOnClassChange(Player player, PlayerClass oldClass, PlayerClass newClass) {
        UUID puuid = player.getUniqueId();
        StatsManager statsMgr = StatsManager.getInstance();
        Set<Integer> equipped = statsMgr.getEquippedItems(puuid);

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) return;

        // Alleen custom items als wapen
        CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(weapon);
        WeaponType wt = WeaponType.matchType(weapon);
        if (ci == null || wt == null) return;

        int id = ci.getId();
        boolean wasApplied = equipped.contains(id);

        // Bepaal vereiste klasse voor dit item
        PlayerClass reqClass;
        try {
            reqClass = PlayerClass.valueOf(ci.getClassRequirement().toUpperCase());
        } catch (IllegalArgumentException e) {
            reqClass = PlayerClass.VILLAGER;
        }

        // 1) Verwijder stats als ze toegepast waren, maar nieuwe klasse niet voldoet
        if (wasApplied && reqClass != PlayerClass.VILLAGER && reqClass != newClass) {
            removeWeaponStats(player, ci);
            equipped.remove(id);
            player.sendMessage(ChatColor.RED + "Your new class no longer allows you to wield " + ci.getBaseName() + "!");
        }

        // 2) Voeg stats toe als ze nog niet waren toegepast, maar nieuwe klasse wél voldoet
        else if (!wasApplied && (reqClass == PlayerClass.VILLAGER || reqClass == newClass)) {
            addWeaponStats(player, ci);
            equipped.add(id);
            player.sendMessage(ChatColor.GREEN + "Stats applied for your " + ci.getBaseName() + "!");
        }

        // Recalc altijd na wijziging
        statsMgr.recalcDerivedStats(player);
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
