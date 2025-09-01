package me.nakilex.levelplugin.duels.listeners;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.duels.managers.DuelRequest;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelListener implements Listener {

    private final Map<UUID, Long> lastRequestTime = new HashMap<>();
    private final Map<UUID, Long> lastAcceptTime = new HashMap<>();
    private final long REQUEST_COOLDOWN_MS = 5000; // 5 seconds

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Duel requests via shift left-click have been removed
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage().toLowerCase();

        if (msg.equals("/duel accept")) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("LevelPlugin"), () -> {
                    boolean accepted = DuelManager.getInstance().acceptRequest(player);
                    if (accepted) {
                        ChatFormatter.sendCenteredMessage(player,
                            "§aYou accepted the duel request!");
                    } else {
                        ChatFormatter.sendCenteredMessage(player,
                            "§cNo valid duel request to accept.");
                    }
                }
            );
        }
        else if (msg.equals("/duel decline")) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("LevelPlugin"), () -> {
                    boolean declined = DuelManager.getInstance().declineRequest(player);
                    if (declined) {
                        ChatFormatter.sendCenteredMessage(player,
                            "§cYou declined the duel request!");
                    } else {
                        ChatFormatter.sendCenteredMessage(player,
                            "§cNo valid duel request to decline.");
                    }
                }
            );
        }
    }

    private boolean canRequest(Player p) {
        long now = System.currentTimeMillis();
        long lastTime = lastRequestTime.getOrDefault(p.getUniqueId(), 0L);
        return (now - lastTime) >= 5000; // 5s in ms
    }
    private void setRequestCooldown(Player p) {
        lastRequestTime.put(p.getUniqueId(), System.currentTimeMillis());
    }

    private boolean canAccept(Player p) {
        long now = System.currentTimeMillis();
        long lastTime = lastAcceptTime.getOrDefault(p.getUniqueId(), 0L);
        return (now - lastTime) >= 5000;
    }
    private void setAcceptCooldown(Player p) {
        lastAcceptTime.put(p.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Restrict damage so players can only harm each other if they are in an active duel.
     * Also check if someone's HP has dropped to 0 or 1, in which case end the duel.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return; // Non-player or PvE, do nothing
        }

        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        DuelManager manager = DuelManager.getInstance();
        boolean inDuel = manager.areInDuel(victim.getUniqueId(), damager.getUniqueId());

        if (!inDuel) {
            // Cancel the damage if they're not allowed to fight
            event.setCancelled(true);
            return;
        }

        boolean formal = manager.areFormallyDueling(victim.getUniqueId(), damager.getUniqueId());

        // If they are in a formal duel, use knockout logic
        if (formal) {
            double newHealth = victim.getHealth() - event.getFinalDamage();
            if (newHealth <= 1) {
                // End the duel, restore both players
                manager.endDuel(victim.getUniqueId(), damager.getUniqueId());

                // Optional "KO" effect
                victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation(), 2);

                ChatFormatter.sendCenteredMessage(victim,
                    "§cYou lost the duel against " + damager.getName() + "!");
                ChatFormatter.sendCenteredMessage(damager,
                    "§aYou have won the duel against " + victim.getName() + "!");

                me.nakilex.levelplugin.Main.getInstance().getQuestManager().handleDuel(damager);
                me.nakilex.levelplugin.Main.getInstance().getQuestManager().handleDuelLose(victim);
                me.nakilex.levelplugin.Main.getInstance().getDuelStatsManager()
                    .addWin(damager.getUniqueId());
                if (me.nakilex.levelplugin.Main.getInstance().getLeaderboardManager() != null) {
                    me.nakilex.levelplugin.Main.getInstance().getLeaderboardManager().updateAll();
                }

                // Prevent actual death
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Shift-left click duel requests removed
        return;
    }
}
