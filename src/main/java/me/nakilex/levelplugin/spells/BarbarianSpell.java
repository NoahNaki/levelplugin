package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listener mapping player interactions to MythicMobs skills for the Barbarian class.
 */
public class BarbarianSpell implements Listener {

    // Barbarian ego weapons use sword items as the Nexo base
    private static final Set<Material> VALID_WEAPONS = EnumSet.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);

    private boolean hasEgoBarbarian(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) return false;
        String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
        return id != null && id.startsWith("barbarian");
    }

    private boolean validWeapon(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item != null && VALID_WEAPONS.contains(item.getType());
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!hasEgoBarbarian(player) || !validWeapon(player)) return;

        Main.getPlugin().getLogger().info("[BB] left click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "RRR"); // Eternal Fury
        } else {
            castSpell(player, "BASIC_ATTACK"); // Rageblade
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasEgoBarbarian(player) || !validWeapon(player)) return;
        event.setCancelled(true);

        Main.getPlugin().getLogger().info("[BB] right click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "LLL"); // Double Edge
        } else {
            castSpell(player, "LRL"); // Primal Axe
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!hasEgoBarbarian(player) || !validWeapon(player)) return;
        Main.getPlugin().getLogger().info("[BB] toggle sneak by " + player.getName());
        castSpell(player, "LLR"); // War Cry
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!hasEgoBarbarian(player) || !validWeapon(player)) return;
        event.setCancelled(true);
        castSpell(player, "RLL"); // Relentless Leap
    }

    private void castSpell(Player player, String combo) {
        Spell spell = SpellManager.getInstance().getSpell("barbarian", combo);
        Main.getPlugin().getLogger().info("[BB] castSpell combo=" + combo + " spell=" + (spell != null));
        if (spell == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            return;
        }
        if (!spell.getAllowedWeapons().contains(player.getInventory().getItemInMainHand().getType())) {
            player.sendMessage("§cYou must hold a valid barbarian weapon!");
            return;
        }
        spell.castEffect(player);
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
