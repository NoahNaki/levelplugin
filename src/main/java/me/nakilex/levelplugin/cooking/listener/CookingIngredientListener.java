package me.nakilex.levelplugin.cooking.listener;

import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.service.CookingSessionService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Thin listener that routes active workstation ingredient inserts to the session service. */
public class CookingIngredientListener implements Listener {
    private final PlacedCookingWorkstationRegistry placedWorkstations;
    private final ActiveCookingSessionRegistry activeSessions;
    private final CookingSessionService sessionService;

    public CookingIngredientListener(
            PlacedCookingWorkstationRegistry placedWorkstations,
            ActiveCookingSessionRegistry activeSessions,
            CookingSessionService sessionService
    ) {
        this.placedWorkstations = placedWorkstations;
        this.activeSessions = activeSessions;
        this.sessionService = sessionService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onIngredientInsert(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        placedWorkstations.find(clicked).ifPresent(placed -> {
            if (activeSessions.getByWorkstation(placed.locationKey()).isEmpty()) {
                return;
            }
            event.setCancelled(true);
            ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
            sessionService.insertHeldIngredient(event.getPlayer(), placed, held, clicked.getLocation());
        });
    }
}
