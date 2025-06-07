package me.nakilex.levelplugin.player.attributes.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StaminaManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.entity.Player;

/**
 * Allows sprinting while hunger is below the vanilla limit as long as the
 * player still has stamina remaining.
 */
public class StaminaSprintListener implements Listener {

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        double stamina = StaminaManager.getInstance().getStamina(player);

        if (event.isSprinting()) {
            // Player is trying to start sprinting
            if (stamina <= 0) {
                event.setCancelled(true);
            }
        } else {
            // Server or player attempting to stop sprinting
            if (player.isSprinting() && stamina > 0 && player.getFoodLevel() <= 6) {
                event.setCancelled(true);
            }
        }
    }
}
