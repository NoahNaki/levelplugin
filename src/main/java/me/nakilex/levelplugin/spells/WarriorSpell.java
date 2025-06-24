package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
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

public class WarriorSpell implements Listener {

    private static final Set<Material> VALID_WEAPONS = EnumSet.of(Material.PAPER);

    private boolean hasEgoWarrior(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) return false;
        String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
        return id != null && id.startsWith("warrior");
    }

    private boolean validWeapon(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && VALID_WEAPONS.contains(item.getType());
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!hasEgoWarrior(player) || !validWeapon(player)) return;

        Main.getPlugin().getLogger().info("[WR] left click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "RLL"); // Shockwave
        } else {
            castSpell(player, "BASIC_ATTACK"); // Brutal Strike
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Player player = event.getPlayer();
        if (!hasEgoWarrior(player) || !validWeapon(player)) return;
        event.setCancelled(true);
        Main.getPlugin().getLogger().info("[WR] right click " + player.getName() + " sneaking=" + player.isSneaking());
        if (player.isSneaking()) {
            castSpell(player, "LRR"); // Chain Hook
        } else {
            castSpell(player, "LRL"); // Charge
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!hasEgoWarrior(player) || !validWeapon(player)) return;
        Main.getPlugin().getLogger().info("[WR] toggle sneak by " + player.getName());
        castSpell(player, "LLR"); // Shield Barrier
    }

    private void castSpell(Player player, String combo) {
        Spell spell = SpellManager.getInstance().getSpell("warrior", combo);
        Main.getPlugin().getLogger().info("[WR] castSpell combo=" + combo + " spell=" + (spell != null));
        if (spell == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            return;
        }
        if (!spell.getAllowedWeapons().contains(player.getInventory().getItemInMainHand().getType())) {
            player.sendMessage("§cYou must hold a valid warrior weapon!");
            return;
        }
        int level = StatsManager.getInstance().getLevel(player);
        if (level < spell.getLevelReq()) {
            player.sendMessage("§cYou are not high enough level for " + spell.getDisplayName());
            return;
        }
        spell.castEffect(player);
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
