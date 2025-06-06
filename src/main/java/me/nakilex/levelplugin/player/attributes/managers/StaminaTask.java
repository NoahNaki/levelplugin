package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically drains or regenerates stamina depending on player movement.
 */
public class StaminaTask extends BukkitRunnable {
    private static final double DRAIN_RATE = 1.0;   // per run while sprinting
    private static final double REGEN_RATE = 0.5;   // per run while not sprinting

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            StaminaManager mgr = StaminaManager.getInstance();
            double max = mgr.getMaxStamina(player);
            double current = mgr.getStamina(player);

            if (player.isSprinting()) {
                current -= DRAIN_RATE;
                if (current <= 0) {
                    current = 0;
                    player.setSprinting(false);
                }
            } else {
                current += REGEN_RATE;
            }

            if (current > max) current = max;
            mgr.setStamina(player, current);

            int food = (int) Math.round((current / max) * 20.0);
            int clamped = Math.max(0, Math.min(20, food));
            if (player.getFoodLevel() != clamped) {
                player.setFoodLevel(clamped);
            }
            player.setSaturation(0f);
        }
    }
}
