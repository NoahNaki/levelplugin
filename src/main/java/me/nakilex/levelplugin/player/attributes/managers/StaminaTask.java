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

    // Regeneration percentages per second
    private static final double WALKING_REGEN_PCT  = 0.10; // walking
    private static final double STANDING_REGEN_PCT = 0.15; // standing still

    private final java.util.Map<java.util.UUID, org.bukkit.Location> lastLocations = new java.util.HashMap<>();

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

            org.bukkit.Location last = lastLocations.get(player.getUniqueId());
            org.bukkit.Location now  = player.getLocation();

            boolean standing = false;
            if (last != null && last.getWorld().equals(now.getWorld())) {
                standing = last.distanceSquared(now) < 0.003; // barely moved
            }
            lastLocations.put(player.getUniqueId(), now);

            boolean wantsSprint = SprintManager.getInstance().wantsSprint(player);
            if (wantsSprint) {
                player.setSprinting(true); // force sprint even if hunger low
                current -= DRAIN_RATE;
                if (current <= 0) {
                    current = 0;
                    player.setSprinting(false);
                    SprintManager.getInstance().setWantsSprint(player, false);
                }
            } else {
                double pct = standing ? STANDING_REGEN_PCT : WALKING_REGEN_PCT;
                double perRun = (max * pct) / 10.0; // task runs 10x per sec
                current += perRun;
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
