package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically drains or regenerates stamina depending on player movement.
 */
public class StaminaTask extends BukkitRunnable {
    private static final double DRAIN_RATE = 1.0;   // per run while sprinting
    // 10 stamina per second (10%) regardless of Vitality
    private static final double REGEN_RATE = 1.0;   // per run while not sprinting

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            StaminaManager mgr = StaminaManager.getInstance();
            double max = mgr.getMaxStamina(player);
            double current = mgr.getStamina(player);

            if (player.getGameMode() == GameMode.CREATIVE) {
                mgr.setStamina(player, max);
                if (player.getFoodLevel() != 20) {
                    player.setFoodLevel(20);
                }
                player.setSaturation(0f);
                continue;
            }

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
