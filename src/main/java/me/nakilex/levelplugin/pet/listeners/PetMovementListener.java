package me.nakilex.levelplugin.pet.listeners;

import me.nakilex.levelplugin.pet.PetManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PetMovementListener implements Listener {
    private static final double MOVE_EPSILON_SQUARED = 0.0001;
    private final PetManager petManager;

    public PetMovementListener(PetManager petManager) {
        this.petManager = petManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getWorld() != to.getWorld()) {
            petManager.recordMovement(event.getPlayer().getUniqueId());
            return;
        }
        if (from.distanceSquared(to) > MOVE_EPSILON_SQUARED) {
            petManager.recordMovement(event.getPlayer().getUniqueId());
        }
    }
}
