package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.items.data.WeaponType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listener mapping player interactions to MythicMobs skills for the Barbarian class.
 */
public class BarbarianSpell implements Listener {

    // Barbarian weapons follow the same warrior rules
    private static final Set<Material> VALID_WEAPONS = EnumSet.copyOf(WeaponType.SWORD.getMaterials());
    static {
        VALID_WEAPONS.addAll(WeaponType.AXE.getMaterials());
        VALID_WEAPONS.addAll(WeaponType.SHOVEL.getMaterials());
    }

    private boolean isBarbarian(Player player) {
        return StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass ==
                me.nakilex.levelplugin.player.classes.data.PlayerClass.BARBARIAN;
    }

    private boolean validWeapon(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean ok = item != null && VALID_WEAPONS.contains(item.getType());
        if (!ok) {
            Main.getPlugin().getLogger().info("[BB DBG] invalid weapon " + (item == null ? "null" : item.getType().name()));
        }
        return ok;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!isBarbarian(player) || !validWeapon(player)) return;

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
        if (!isBarbarian(player) || !validWeapon(player)) return;
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
        if (!isBarbarian(player) || !validWeapon(player)) return;
        Main.getPlugin().getLogger().info("[BB] toggle sneak by " + player.getName());
        castSpell(player, "LLR"); // War Cry
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
