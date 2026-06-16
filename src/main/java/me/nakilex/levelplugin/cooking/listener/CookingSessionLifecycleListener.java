package me.nakilex.levelplugin.cooking.listener;

import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.service.CookingSessionService;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/** Cancels active cooking sessions from player lifecycle actions such as sneaking or walking away. */
public class CookingSessionLifecycleListener implements Listener {
    private static final double MAX_DISTANCE_SQUARED = 10.0D * 10.0D;

    private final ActiveCookingSessionRegistry sessionRegistry;
    private final CookingSessionService sessionService;

    public CookingSessionLifecycleListener(ActiveCookingSessionRegistry sessionRegistry, CookingSessionService sessionService) {
        this.sessionRegistry = sessionRegistry;
        this.sessionService = sessionService;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        boolean cancelled = sessionService.cancelSessionByPlayer(player.getUniqueId(), true, "Player sneaked to cancel cooking");
        if (cancelled) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Cancelled your cooking session and returned inserted ingredients.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!movedBlock(event)) {
            return;
        }
        Player player = event.getPlayer();
        ActiveCookingSession session = sessionRegistry.getByPlayer(player.getUniqueId()).orElse(null);
        if (session == null || isWithinRange(player.getLocation(), session.workstationKey().toLocation())) {
            return;
        }
        boolean cancelled = sessionService.cancelSessionByPlayer(player.getUniqueId(), true, "Player moved too far from cooking workstation");
        if (cancelled) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Cooking cancelled because you moved too far away. Inserted ingredients were returned.");
        }
    }

    private boolean movedBlock(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        return to != null && (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ());
    }

    private boolean isWithinRange(Location playerLocation, Location workstationLocation) {
        if (playerLocation == null || workstationLocation == null
                || playerLocation.getWorld() == null || workstationLocation.getWorld() == null
                || !playerLocation.getWorld().equals(workstationLocation.getWorld())) {
            return false;
        }
        return playerLocation.distanceSquared(workstationLocation.clone().add(0.5D, 0.5D, 0.5D)) <= MAX_DISTANCE_SQUARED;
    }
}
