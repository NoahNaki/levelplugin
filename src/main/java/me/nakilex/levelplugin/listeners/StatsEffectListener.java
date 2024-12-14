package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.managers.StatsManager.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

public class StatsEffectListener implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        // Outgoing damage modifications if a player is the damager
        if (damager instanceof Player) {
            Player player = (Player) damager;
            PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

            // Strength: e.g. +0.5 damage per STR
            double finalDamage = event.getDamage() + (ps.strength * 0.5);

            // Dexterity: Crit chance e.g. 1% per DEX
            double critChance = ps.dexterity * 0.01; // e.g. 20 DEX = 20% crit
            if (random.nextDouble() < critChance) {
                finalDamage *= 1.5; // 50% more damage on crit
                player.sendMessage(ChatColor.GOLD + "Critical Hit!");
            }

            event.setDamage(finalDamage);
        }

        // Incoming damage modifications if the target is a player
        if (target instanceof Player) {
            Player victim = (Player) target;
            PlayerStats vs = StatsManager.getInstance().getPlayerStats(victim);

            // Agility: Dodge chance e.g. 1% per AGI
            double dodgeChance = vs.agility * 0.01;
            if (random.nextDouble() < dodgeChance) {
                event.setCancelled(true);
                victim.sendMessage(ChatColor.GREEN + "You dodged the attack!");
                return;
            }

            // Defence: Flat damage negation e.g. 0.5 per DEF stat
            double reducedDamage = event.getDamage() - (vs.defenceStat * 0.5);
            if (reducedDamage < 0) reducedDamage = 0;

            event.setDamage(reducedDamage);
        }
    }
}
