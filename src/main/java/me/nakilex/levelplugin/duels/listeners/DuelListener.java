package me.nakilex.levelplugin.duels.listeners;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;


public class DuelListener implements Listener {


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


    /**
     * Restrict damage so players can only harm each other if they are in an active duel.
     * Also check if someone's HP has dropped to 0 or 1, in which case end the duel.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Entity raw = event.getDamager();
        Player attacker = null;
        if (raw instanceof Player p) {
            attacker = p;
        } else if (raw instanceof Projectile proj && proj.getShooter() instanceof Player) {
            attacker = (Player) proj.getShooter();
        }

        if (attacker == null) {
            return; // Non-player damager
        }

        DuelManager manager = DuelManager.getInstance();
        if (!manager.areInDuel(victim.getUniqueId(), attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (manager.handleDuelDamage(victim, attacker, event.getFinalDamage())) {
            // Cancel and zero-out damage so other plugins don’t process a lethal hit
            event.setDamage(0);
            event.setCancelled(true);
        }
    }
}
