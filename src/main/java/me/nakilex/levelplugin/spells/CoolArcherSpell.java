package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.WeaponType;
import org.bukkit.persistence.PersistentDataType;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.Spell;
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

import java.util.EnumSet;
import java.util.Set;

/**
 * Simple listener that maps basic interactions to MythicMobs skills for the
 * CoolArcher test class.
 */
public class CoolArcherSpell implements Listener {

    // Use bow and crossbow materials
    private static final Set<Material> VALID_WEAPONS = EnumSet.copyOf(WeaponType.BOW.getMaterials());

    private boolean isCoolArcher(Player p) {
        return StatsManager.getInstance().getPlayerStats(p.getUniqueId()).playerClass ==
                me.nakilex.levelplugin.player.classes.data.PlayerClass.COOLARCHER;
    }

    private boolean validWeapon(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        boolean ok = item != null && VALID_WEAPONS.contains(item.getType());
        if (!ok) {
            Main.getPlugin().getLogger().info("[CA DBG] invalid weapon " + (item == null ? "null" : item.getType().name()));
        }
        return ok;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!isCoolArcher(player) || !validWeapon(player)) return;

        Main.getPlugin().getLogger().info("[CA] left click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "RRR"); // Bow Drone
        } else {
            castSpell(player, "BASIC_ATTACK"); // Quick Shot
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isCoolArcher(player) || !validWeapon(player)) return;
        event.setCancelled(true);

        Main.getPlugin().getLogger().info("[CA] right click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "LLL"); // Dragon Piercer
        } else {
            castSpell(player, "LRL"); // Backstep
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!isCoolArcher(player) || !validWeapon(player)) return;
        Main.getPlugin().getLogger().info("[CA] toggle sneak by " + player.getName());
        castSpell(player, "LLR"); // Arrow Barrage
    }

    private void castSpell(Player player, String combo) {
        Spell spell = SpellManager.getInstance().getSpell("coolarcher", combo);
        Main.getPlugin().getLogger().info("[CA] castSpell combo=" + combo + " spell=" + (spell!=null));
        if (spell == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            return;
        }

        // weapon check
        if (!spell.getAllowedWeapons().contains(player.getInventory().getItemInMainHand().getType())) {
            player.sendMessage("§cYou must hold a valid coolarcher weapon!");
            return;
        }
        spell.castEffect(player);
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
