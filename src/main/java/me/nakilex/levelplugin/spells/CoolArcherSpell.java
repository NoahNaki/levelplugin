package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
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

    private static final Set<Material> VALID_WEAPONS = EnumSet.of(Material.CROSSBOW, Material.BOW);

    private boolean isCoolArcher(Player p) {
        return StatsManager.getInstance().getPlayerStats(p.getUniqueId()).playerClass == PlayerClass.COOLARCHER;
    }

    private boolean validWeapon(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && VALID_WEAPONS.contains(item.getType());
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!isCoolArcher(player) || !validWeapon(player)) return;

        if (player.isSneaking()) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, "Deadly_Javelin");
        } else {
            MythicBukkit.inst().getAPIHelper().castSkill(player, "Quick_Shot");
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isCoolArcher(player) || !validWeapon(player)) return;
        event.setCancelled(true);

        if (player.isSneaking()) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, "Dragon_Piercer");
        } else {
            MythicBukkit.inst().getAPIHelper().castSkill(player, "Backstep");
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!isCoolArcher(player) || !validWeapon(player)) return;
        MythicBukkit.inst().getAPIHelper().castSkill(player, "Arrow_Barrage");
    }
}
