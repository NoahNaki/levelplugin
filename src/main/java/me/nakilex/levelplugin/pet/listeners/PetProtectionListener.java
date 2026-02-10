package me.nakilex.levelplugin.pet.listeners;

import me.nakilex.levelplugin.pet.PetManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Ensures managed pet entities stay immortal during gameplay.
 *
 * Command-driven removal is still allowed (e.g. /kill), which typically uses
 * {@link org.bukkit.event.entity.EntityDamageEvent.DamageCause#KILL}.
 */
public class PetProtectionListener implements Listener {
    private final PetManager petManager;

    public PetProtectionListener(PetManager petManager) {
        this.petManager = petManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!petManager.isManagedPetEntity(event.getEntity())) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.KILL) {
            return;
        }
        event.setCancelled(true);
    }
}
