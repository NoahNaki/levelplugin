package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.persistence.PersistentDataType;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Set;

public class PhoenixHunterSpell implements Listener {

    // Use bow and crossbow materials
    private static final Set<Material> VALID_WEAPONS = EnumSet.copyOf(WeaponType.BOW.getMaterials());

    public PhoenixHunterSpell() {
        // Passive task for Flameborn/Phoenix Totem every 10 ticks
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    ItemStack item = p.getInventory().getItemInMainHand();
                    if (item != null && item.hasItemMeta() && VALID_WEAPONS.contains(item.getType())) {
                        MythicBukkit.inst().getAPIHelper().castSkill(p, "Flameborn");
                        Main.getPlugin().getLogger().info("[PH] passive Flameborn for " + p.getName());
                        castSpell(p, "LLR");
                    }
                }
            }
        }.runTaskTimer(me.nakilex.levelplugin.Main.getInstance(), 10L, 10L);
    }

    private boolean isPhoenixHunter(Player player) {
        return StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass ==
                me.nakilex.levelplugin.player.classes.data.PlayerClass.PHOENIXHUNTER;
    }

    private boolean validWeapon(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean ok = item != null && VALID_WEAPONS.contains(item.getType());
        if (!ok) {
            Main.getPlugin().getLogger().info("[PH DBG] invalid weapon " + (item == null ? "null" : item.getType().name()));
        }
        return ok;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!isPhoenixHunter(player) || !validWeapon(player)) return;

        Main.getPlugin().getLogger().info("[PH] left click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "RRR"); // Phoenix Rebirth
        } else {
            castSpell(player, "BASIC_ATTACK"); // Blazing Feathers
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return; // main hand only
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isPhoenixHunter(player) || !validWeapon(player)) return;
        event.setCancelled(true);
        Main.getPlugin().getLogger().info("[PH] right click " + player.getName() + " sneaking=" + player.isSneaking());
        if (player.isSneaking()) {
            castSpell(player, "LLL"); // Pyroclasmic Barrage
        } else {
            castSpell(player, "LRL"); // Ashdance
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!isPhoenixHunter(player) || !validWeapon(player)) return;
        Main.getPlugin().getLogger().info("[PH] toggle sneak by " + player.getName());
        castSpell(player, "LRR"); // Flameburst Convergence
    }

    private void castSpell(Player player, String combo) {
        Spell spell = SpellManager.getInstance().getSpell("phoenixhunter", combo);
        Main.getPlugin().getLogger().info("[PH] castSpell combo=" + combo + " spell=" + (spell!=null));
        if (spell == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            return;
        }
        if (!spell.getAllowedWeapons().contains(player.getInventory().getItemInMainHand().getType())) {
            player.sendMessage("§cYou must hold a valid phoenixhunter weapon!");
            return;
        }
        boolean ok = spell.castEffect(player);
        if (ok) {
            StatsManager.getInstance().recalcDerivedStats(player);
        }
    }
}
