package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listener mapping player interactions to MythicMobs skills for the Mage class.
 */
public class MageSpell implements Listener {

    private static final Set<Material> VALID_WEAPONS = EnumSet.copyOf(WeaponType.WAND.getMaterials());

    private boolean isMage(Player player) {
        return StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass ==
                me.nakilex.levelplugin.player.classes.data.PlayerClass.MAGE;
    }

    private boolean validWeapon(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean ok = item != null && WeaponType.isValidMageWeapon(item);
        if (!ok) {
            Main.getPlugin().getLogger().info("[MG DBG] invalid weapon " + (item == null ? "null" : item.getType().name()));
        }
        return ok;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!isMage(player) || !validWeapon(player)) return;

        Main.getPlugin().getLogger().info("[MG] left click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "RRR"); // Inferno Chains
        } else {
            castSpell(player, "BASIC_ATTACK"); // Fireball
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isMage(player) || !validWeapon(player)) return;
        event.setCancelled(true);

        Main.getPlugin().getLogger().info("[MG] right click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "LLL"); // Meteor
        } else {
            castSpell(player, "LRL"); // Blink
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!isMage(player) || !validWeapon(player)) return;
        Main.getPlugin().getLogger().info("[MG] toggle sneak by " + player.getName());
        castSpell(player, "LRR"); // Frost Nova
    }

    private void castSpell(Player player, String combo) {
        Spell spell = SpellManager.getInstance().getSpell("mage", combo);
        Main.getPlugin().getLogger().info("[MG] castSpell combo=" + combo + " spell=" + (spell != null));
        if (spell == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            return;
        }
        if (!spell.getAllowedWeapons().contains(player.getInventory().getItemInMainHand().getType())) {
            player.sendMessage("§cYou must hold a valid mage weapon!");
            return;
        }
        spell.castEffect(player);
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
