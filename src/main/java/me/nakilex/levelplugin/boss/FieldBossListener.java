package me.nakilex.levelplugin.boss;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FieldBossListener implements Listener {
    // Configure your field boss display names here
    private static final Set<String> BOSS_NAMES = Set.of(
        "Alien", "Fire Dragon", "Ancient Titan"
    );

    // Map<bossUUID, Map<playerUUID, totalDamage>>
    private final Map<UUID, Map<UUID, Double>> damageMap = new ConcurrentHashMap<>();
    private final Main plugin;

    public FieldBossListener(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent ev) {
        Entity targetEntity = ev.getEntity();
        if (!(targetEntity instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) targetEntity;

        // Check if target is a MythicMob with a configured name
        ActiveMob mob = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(target);
        if (mob == null) return;
        // Get the display name directly (PlaceholderString#get())
        String displayName = mob.getType().getDisplayName().get();
        if (!BOSS_NAMES.contains(displayName)) return;

        // Determine the player source
        Player damager = null;
        if (ev.getDamager() instanceof Player p) {
            damager = p;
        } else if (ev.getDamager() instanceof Projectile proj
            && proj.getShooter() instanceof Player shooter) {
            damager = shooter;
        }
        if (damager == null) return;

        UUID bossId = target.getUniqueId();
        UUID playerId = damager.getUniqueId();
        double dmg = ev.getFinalDamage();

        // Initialize boss entry if first hit
        damageMap.computeIfAbsent(bossId, id -> {
            announceBossEngage(displayName);
            return new ConcurrentHashMap<>();
        });

        // Accumulate damage
        Map<UUID, Double> bossRecord = damageMap.get(bossId);
        bossRecord.merge(playerId, dmg, Double::sum);
    }

    @EventHandler
    public void onBossDeath(MythicMobDeathEvent ev) {
        ActiveMob mob = ev.getMob();
        // Use the same getType().getDisplayName().get() here
        String displayName = mob.getType().getDisplayName().get();
        if (!BOSS_NAMES.contains(displayName)) return;

        UUID bossId = BukkitAdapter.adapt(ev.getEntity()).getUniqueId();
        Map<UUID, Double> bossRecord = damageMap.remove(bossId);
        if (bossRecord == null || bossRecord.isEmpty()) return;

        // Build leaderboard
        List<Map.Entry<UUID, Double>> sorted = bossRecord.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());

        // Broadcast in chat
        Bukkit.broadcastMessage(ChatColor.GOLD + "---- "
            + ChatColor.RED + displayName
            + ChatColor.GOLD + " Defeated ----");
        int rank = 1;
        for (var entry : sorted) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            double total = Math.round(entry.getValue() * 10.0) / 10.0;
            String line = ChatColor.YELLOW + "#" + rank++ + " "
                + ChatColor.WHITE + p.getName() + ": "
                + ChatColor.GREEN + total + " dmg";
            Bukkit.broadcastMessage(line);
        }
    }

    private void announceBossEngage(String name) {
        new BukkitRunnable() {
            @Override public void run() {
                Bukkit.broadcastMessage(ChatColor.DARK_PURPLE
                    + "[Boss Engaged] " + ChatColor.RED + name
                    + ChatColor.DARK_PURPLE + " has entered combat!");
            }
        }.runTask(plugin);
    }
}

