package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
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
 * Listener mapping player interactions to MythicMobs skills for the Overlord class.
 * This class mirrors the style of other spell listeners but directly triggers
 * the MythicMobs skills defined for the Overlord/witch class.
 */
public class OverlordSpell implements Listener {

    private static final Set<Material> VALID_WEAPONS = EnumSet.copyOf(WeaponType.WAND.getMaterials());

    private boolean isOverlord(Player player) {
        return StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass ==
                PlayerClass.OVERLORD;
    }

    private boolean validWeapon(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean ok = item != null && VALID_WEAPONS.contains(item.getType());
        if (!ok) {
            Main.getPlugin().getLogger().info("[OVL DBG] invalid weapon " + (item == null ? "null" : item.getType().name()));
        }
        return ok;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!isOverlord(player) || !validWeapon(player)) return;

        if (player.isSneaking()) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, "mf_class_witch_sneak_leftclick");
        } else {
            MythicBukkit.inst().getAPIHelper().castSkill(player, "mf_class_witch_normalattack");
        }
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isOverlord(player) || !validWeapon(player)) return;
        event.setCancelled(true);

        if (player.isSneaking()) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, "mf_class_witch_sneak_rightclick");
        } else {
            MythicBukkit.inst().getAPIHelper().castSkill(player, "mf_class_witch_rightclick");
        }
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!isOverlord(player) || !validWeapon(player)) return;

        MythicBukkit.inst().getAPIHelper().castSkill(player, "mf_class_witch_shiftshift");
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
