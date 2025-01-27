package me.nakilex.levelplugin.duels.listeners;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.duels.managers.DuelRequest;
import me.nakilex.levelplugin.utils.ChatFormatter;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

/**
 * Handles the logic for sending/receiving duel requests, restricting PvP to duels, etc.
 */
public class DuelListener implements Listener {

    private final Map<UUID, Long> lastRequestTime = new HashMap<>();
    private final Map<UUID, Long> lastAcceptTime = new HashMap<>();
    private final long REQUEST_COOLDOWN_MS = 5000; // 5 seconds

    /**
     * Listen for SHIFT + LEFT_CLICK in the air to send or accept a request
     * by doing a small ray-trace for a player in front.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Must be left-click in air/block while sneaking
        if ((event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
            || !event.getPlayer().isSneaking()) {
            return;
        }
        Player player = event.getPlayer();

        // RayTrace up to ~4 blocks for a Player in front
        double range = 4.0;
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            range,
            (entity) -> entity instanceof Player && !entity.equals(player)
        );
        if (result == null) return;
        Entity hitEntity = result.getHitEntity();
        if (!(hitEntity instanceof Player)) return;

        Player target = (Player) hitEntity;
        DuelManager manager = DuelManager.getInstance();

        // 1) If either player is ALREADY in a duel, don't allow new requests
        if (manager.areInAnyDuel(player) || manager.areInAnyDuel(target)) {
            ChatFormatter.sendCenteredMessage(player, ChatColor.RED + "Either you or they are in a duel already!");
            return;
        }

        // 2) Check if there's a pending request from target -> me
        DuelRequest pendingToMe = manager.getRequest(player.getUniqueId());
        if (pendingToMe != null && pendingToMe.getRequester().equals(target.getUniqueId())) {
            // Accept it
            boolean accepted = manager.acceptRequest(player);
            if (accepted) {
                ChatFormatter.sendCenteredMessage(player,
                    "§aYou accepted " + target.getName() + "'s duel request!");
                ChatFormatter.sendCenteredMessage(target,
                    "§aYour duel request was accepted by " + player.getName() + "!");
            }
            return;
        }

        // 3) Check if there's a request from me -> target
        DuelRequest pendingToTarget = manager.getRequest(target.getUniqueId());
        if (pendingToTarget != null && pendingToTarget.getRequester().equals(player.getUniqueId())) {
            // Already sent one recently
            ChatFormatter.sendCenteredMessage(player,
                "§cYou have already sent a duel request to " + target.getName() + "!");
            return;
        }

        // 4) Check cooldown so we don't spam requests
        long now = System.currentTimeMillis();
        long lastTime = lastRequestTime.getOrDefault(player.getUniqueId(), 0L);
        if ((now - lastTime) < REQUEST_COOLDOWN_MS) {
            ChatFormatter.sendCenteredMessage(player, "§cWait a few seconds before sending another duel request!");
            return;
        }
        lastRequestTime.put(player.getUniqueId(), now);

        // 5) Otherwise, create new request
        manager.createRequest(player, target);

        // Display one centered line to the sender
        ChatFormatter.sendCenteredMessage(player,
            "§6You have sent a duel request to " + target.getName() + "!");

        // Display one centered line to the target
        ChatFormatter.sendCenteredMessage(target,
            "§e" + player.getName() + " has challenged you to a duel! Expires in 10s.");

        // Provide clickable bold [ACCEPT] [DECLINE]
        sendCenteredAcceptDecline(target, player.getName());
    }

    private void sendCenteredAcceptDecline(Player target, String challengerName) {
        // Send the prompt
        ChatFormatter.sendCenteredMessage(target,
            ChatColor.YELLOW + challengerName + " has challenged you! Click below:");

        // Calculate the approximate padding needed for centering
        String padding = "                     "; // Adjust this based on testing for proper centering

        // Construct the centered [ACCEPT] button
        TextComponent acceptBtn = new TextComponent("§a§l[ACCEPT]");
        acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept"));
        acceptBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("Click to accept the duel").create()));

        // Construct the centered [DECLINE] button
        TextComponent declineBtn = new TextComponent(" §c§l[DECLINE]");
        declineBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel decline"));
        declineBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("Click to decline the duel").create()));

        // Combine buttons with padding and send them as a single centered message
        TextComponent finalMessage = new TextComponent(padding);
        finalMessage.addExtra(acceptBtn);
        finalMessage.addExtra("   "); // Space between buttons
        finalMessage.addExtra(declineBtn);

        target.spigot().sendMessage(finalMessage);
    }




    /**
     * Minimal approach for "/duel accept" or "/duel decline" typed in chat,
     * fired from the clickable text or manual typing.
     */
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
     * Minimal example of clickable Accept/Decline.
     * Spigot's ChatComponent API does not easily center text,
     * so we just send a centered prompt line, then the clickable line.
     */
    /**
     * Send a duel request message with centered bold [ACCEPT] [DECLINE] buttons.
     */
    public void sendDuelRequestMessage(Player target, String challengerName) {
        // Send a centered prompt line
        ChatFormatter.sendCenteredMessage(target,
            ChatColor.YELLOW + challengerName + " has challenged you! Click below:");

        // Call the method to send the centered accept/decline buttons
        sendCenteredAcceptDecline(target, challengerName);
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
            // Cancel the damage if they're not in a duel
            event.setCancelled(true);
            return;
        }

        // If they are in a duel, allow damage. Then check if victim is about to die (HP <= 1).
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

            // Prevent actual death
            event.setCancelled(true);
        }
    }

    /**
     * This second EntityDamageByEntityEvent is for SHIFT+left-click as "duel request."
     * You might not need this if your main approach is raytracing in onPlayerInteract().
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // If attacker is sneaking, interpret the hit as a "duel request/accept" attempt
        if (attacker.isSneaking()) {
            event.setCancelled(true);

            DuelManager manager = DuelManager.getInstance();
            DuelRequest existingRequest = manager.getRequest(attacker.getUniqueId());

            if (existingRequest != null && existingRequest.getRequester().equals(victim.getUniqueId())) {
                // Attacker is accepting a request from victim
                boolean accepted = manager.acceptRequest(attacker);
                if (accepted) {
                    ChatFormatter.sendCenteredMessage(attacker,
                        "§aYou accepted " + victim.getName() + "'s duel request!");
                    ChatFormatter.sendCenteredMessage(victim,
                        "§aYour duel request was accepted by " + attacker.getName() + "!");
                }
            } else {
                // Otherwise, create a new request
                manager.createRequest(attacker, victim);

                ChatFormatter.sendCenteredMessage(attacker,
                    "§6You have sent a duel request to " + victim.getName() + "!");
                ChatFormatter.sendCenteredMessage(victim,
                    "§e" + attacker.getName() + " has challenged you to a duel! Expires in 10s.");

                // Optionally send clickable chat Accept/Decline:
                sendDuelRequestMessage(victim, attacker.getName());
            }
        }
    }
}
