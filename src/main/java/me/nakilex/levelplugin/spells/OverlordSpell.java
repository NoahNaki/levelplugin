package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
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

import java.util.EnumSet;
import java.util.Set;

/**
 * Listener mapping interactions to MythicMobs skills for the Overlord class.
 */
public class OverlordSpell implements Listener {

    private static final Set<Material> VALID_WEAPONS = EnumSet.copyOf(WeaponType.WAND.getMaterials());

    private boolean isOverlord(Player player) {
        return StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass ==
                me.nakilex.levelplugin.player.classes.data.PlayerClass.OVERLORD;
    }

    private boolean validWeapon(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold a valid overlord weapon!");
            return false;
        }

        if (WeaponType.isValidMageWeapon(item)) return true;

        CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(item);
        if (ci != null) {
            String reqRaw = ci.getClassRequirement();
            if (reqRaw != null && !reqRaw.isBlank()) {
                try {
                    PlayerClass req = PlayerClass.valueOf(reqRaw.toUpperCase());
                    if (ClassUtil.isMageFamily(req)) return true;
                } catch (IllegalArgumentException ignored) {}
            }
        }

        Main.getPlugin().getLogger().info("[OV DBG] invalid weapon " + item.getType());
        player.sendMessage("§cYou must hold a valid overlord weapon!");
        return false;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!isOverlord(player) || !validWeapon(player)) return;

        if (player.isSneaking()) {
            castSpell(player, "RRR"); // Ultimate
        } else {
            castSpell(player, "BASIC_ATTACK"); // Dark Bolt
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Player player = event.getPlayer();
        if (!isOverlord(player) || !validWeapon(player)) return;
        event.setCancelled(true);

        // Summon minions on right click
        castSpell(player, "LRL");
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!isOverlord(player) || !validWeapon(player)) return;
        castSpell(player, "LRR");
    }

    private void castSpell(Player player, String combo) {
        Spell spell = SpellManager.getInstance().getSpell("overlord", combo);
        if (spell == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            return;
        }
        spell.castEffect(player);
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
