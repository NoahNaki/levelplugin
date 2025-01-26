package me.nakilex.levelplugin.duels.listeners;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.duels.managers.DuelRequest;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class DuelListener implements Listener {

    /**
     * Listen for SHIFT + LEFT_CLICK to send or accept a request.
     * Because "left-clicking an entity" typically triggers damage,
     * we do a small ray-trace to see if a player is in front.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return; // not a left click
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return; // not crouching
        }

        // RayTrace up to ~4 blocks for an entity in front
        // (adjust as you like)
        double range = 4.0;
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            range,
            (entity) -> entity instanceof Player && !entity.equals(player)
        );

        if (result == null) {
            return;
        }

        Entity hitEntity = result.getHitEntity();
        if (!(hitEntity instanceof Player)) {
            return;
        }

        Player target = (Player) hitEntity;

        // 1) Check if there's an existing request from 'target' to 'player' (meaning target is the "requester" and player is the "target").
        //    If so, SHIFT+Left-Click means "accept" that request?
        //    2) Otherwise, create a new request from 'player' -> 'target'.

        // In other words, are we "accepting" or "creating" a request?
        DuelManager manager = DuelManager.getInstance();
        DuelRequest existing = manager.getRequest(player.getUniqueId());
        if (existing != null && existing.getRequester().equals(target.getUniqueId())) {
            // That means target -> player request is pending
            // Now 'player' is effectively "accepting"
            boolean accepted = manager.acceptRequest(player);
            if (accepted) {
                player.sendMessage("§aYou accepted " + target.getName() + "'s duel request!");
                target.sendMessage("§aYour duel request was accepted by " + player.getName() + "!");
            }
            return;
        }

        // Otherwise, create a new request
        manager.createRequest(player, target);
        player.sendMessage("§6You have sent a duel request to " + target.getName() + "!");
        target.sendMessage("§e" + player.getName() + " has challenged you to a duel! Expires in 10s.");

        // Optionally send clickable chat Accept/Decline:
        sendDuelRequestMessage(target, player.getName());
    }

    /**
     * Minimal example of clickable Accept/Decline.
     * Requires Spigot's ChatComponent API (net.md_5.bungee.api.chat.*).
     */
    public void sendDuelRequestMessage(Player target, String challengerName) {
        TextComponent base = new TextComponent(ChatColor.YELLOW + challengerName + " has challenged you! ");

        TextComponent acceptBtn = new TextComponent("[ACCEPT]");
        acceptBtn.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept"));
        acceptBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to accept the duel").create()));

        TextComponent declineBtn = new TextComponent("[DECLINE]");
        declineBtn.setColor(net.md_5.bungee.api.ChatColor.RED);
        declineBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel decline"));
        declineBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to decline the duel").create()));

        base.addExtra(acceptBtn);
        base.addExtra(" ");
        base.addExtra(declineBtn);

        target.spigot().sendMessage(base);
    }

    /**
     * Minimal chat command approach for "/duel accept" or "/duel decline".
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String msg = event.getMessage().toLowerCase();
        Player player = event.getPlayer();
        if (msg.equalsIgnoreCase("/duel accept")) {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("LevelPlugin"), () -> {
                boolean accepted = DuelManager.getInstance().acceptRequest(player);
                if (accepted) {
                    player.sendMessage("§aYou accepted the duel request!");
                } else {
                    player.sendMessage("§cNo valid duel request to accept.");
                }
            });
            event.setCancelled(true); // optional
        }
        else if (msg.equalsIgnoreCase("/duel decline")) {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("LevelPlugin"), () -> {
                boolean declined = DuelManager.getInstance().declineRequest(player);
                if (declined) {
                    player.sendMessage("§cYou declined the duel request!");
                } else {
                    player.sendMessage("§cNo valid duel request to decline.");
                }
            });
            event.setCancelled(true); // optional
        }
    }

    /**
     * Restrict damage so players can only harm each other if they are in an active duel.
     * Also check if someone's HP has dropped to 0 or 1, in which case end the duel.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return; // non-player or pve, do nothing
        }

        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        // Check if they are in a duel with each other
        DuelManager manager = DuelManager.getInstance();
        boolean inDuel = manager.areInDuel(victim.getUniqueId(), damager.getUniqueId());

        if (!inDuel) {
            // Cancel the damage
            event.setCancelled(true);
            return;
        }

        // If they are in a duel, allow the damage. Then check if the victim is about to die (HP <= 1).
        double newHealth = victim.getHealth() - event.getFinalDamage();
        // Note: newHealth might go below zero in some edge cases; handle as you prefer.
        if (newHealth <= 1) {
            // End the duel, restore both players
            manager.endDuel(victim.getUniqueId(), damager.getUniqueId());

            // Optionally you can show some effect to highlight the "KO"
            victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation(), 2);
            victim.sendMessage("§cYou lost the duel against " + damager.getName() + "!");
            damager.sendMessage("§aYou have won the duel against " + victim.getName() + "!");

            // Force-set victim's HP so they do not actually die
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check that the "damager" is a Player and the "target" is a Player
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // If attacker is sneaking, we treat the hit as a "send or accept a duel"
        if (attacker.isSneaking()) {
            // Cancel any real damage for this "duel request" action
            event.setCancelled(true);

            DuelManager manager = DuelManager.getInstance();

            // Check if there's an existing request from victim -> attacker
            // That would mean victim asked attacker, and now the attacker is accepting
            DuelRequest existingRequest = manager.getRequest(attacker.getUniqueId());
            if (existingRequest != null && existingRequest.getRequester().equals(victim.getUniqueId())) {
                // Accept the request
                boolean accepted = manager.acceptRequest(attacker);
                if (accepted) {
                    attacker.sendMessage("§aYou accepted " + victim.getName() + "'s duel request!");
                    victim.sendMessage("§aYour duel request was accepted by " + attacker.getName() + "!");
                }
            } else {
                // Otherwise, we create a new request from attacker -> victim
                manager.createRequest(attacker, victim);
                attacker.sendMessage("§6You have sent a duel request to " + victim.getName() + "!");
                victim.sendMessage("§e" + attacker.getName() + " has challenged you to a duel! Expires in 10s.");

                // Optionally send clickable chat Accept/Decline:
                sendDuelRequestMessage(victim, attacker.getName());
            }
        }
    }
}
