package me.nakilex.levelplugin.effects.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
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
            PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

            // Calculate total strength
            int totalStrength = ps.baseStrength + ps.bonusStrength;
            double finalDamage = event.getDamage() + (totalStrength * 0.5);

            // Dexterity: Crit chance e.g. 1% per DEX
            int totalDexterity = ps.baseDexterity + ps.bonusDexterity;
            double critChance = totalDexterity * 0.01; // e.g. 20 DEX = 20% crit
            if (random.nextDouble() < critChance) {
                finalDamage *= 1.5; // 50% more damage on crit
                player.sendMessage(ChatColor.GOLD + "Critical Hit!");
            }

            event.setDamage(finalDamage);
        }

        // Incoming damage modifications if the target is a player
        if (target instanceof Player) {
            Player player = (Player) target;
            PlayerStats vs = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

            // Agility: Dodge chance e.g. 1% per AGI
            int totalAgility = vs.baseAgility + vs.bonusAgility;
            double dodgeChance = totalAgility * 0.01;
            if (random.nextDouble() < dodgeChance) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.GREEN + "You dodged the attack!");
                return;
            }

            // Defence: Flat damage negation e.g. 0.5 per DEF stat
            int totalDefence = vs.baseDefenceStat + vs.bonusDefenceStat;
            double reducedDamage = event.getDamage() - (totalDefence * 0.5);
            if (reducedDamage < 0) reducedDamage = 0;

            event.setDamage(reducedDamage);
        }
    }
}
