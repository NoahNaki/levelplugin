package me.nakilex.levelplugin.doublejump.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

public class DoubleJumpListener implements Listener {

    private static final double BASE_LIFT_VELOCITY       = 0.3;   // how much up
    private static final double BASE_FORWARD_VELOCITY    = 0.5;   // how much forward
    private static final double AGI_LIFT_MULTIPLIER      = 0.005; // extra up per Agi
    private static final double AGI_FORWARD_MULTIPLIER   = 0.02;  // extra forward per Agi
    private static final double MAX_LIFT_VELOCITY        = 0.6;
    private static final double MAX_FORWARD_VELOCITY     = 1.2;

    private boolean canDoubleJump(PlayerClass pc) {
        return pc == PlayerClass.ARCHER
            || pc == PlayerClass.ROGUE;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerClass playerClass = StatsManager.getInstance()
            .getPlayerStats(player.getUniqueId())
            .playerClass;
        player.setAllowFlight(canDoubleJump(playerClass));
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        StatsManager.PlayerStats ps = StatsManager
            .getInstance()
            .getPlayerStats(player.getUniqueId());
        if (!canDoubleJump(ps.playerClass)) return;

        if (!player.isFlying()) {
            event.setCancelled(true);
            player.setAllowFlight(false);

            int totalAgi = ps.baseAgility + ps.bonusAgility;

            // 1) compute vertical lift
            double lift = BASE_LIFT_VELOCITY + (totalAgi * AGI_LIFT_MULTIPLIER);
            lift = Math.min(lift, MAX_LIFT_VELOCITY);

            // 2) compute forward thrust
            double thrust = BASE_FORWARD_VELOCITY + (totalAgi * AGI_FORWARD_MULTIPLIER);
            thrust = Math.min(thrust, MAX_FORWARD_VELOCITY);

            // 3) build the velocity vector
            Vector lookDir = player.getLocation()
                .getDirection()
                .setY(0)               // ignore looking up/down
                .normalize()
                .multiply(thrust);    // horizontal push
            lookDir.setY(lift);                        // vertical lift

            player.setVelocity(lookDir);

            // FX/SFX
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(),
                30, 0.5, 0.1, 0.5, 0.1);
            player.getWorld().playSound(player.getLocation(),
                Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        StatsManager.PlayerStats ps = StatsManager
            .getInstance()
            .getPlayerStats(player.getUniqueId());

        if (player.isOnGround() && canDoubleJump(ps.playerClass)) {
            player.setAllowFlight(true);
        }
    }
}
