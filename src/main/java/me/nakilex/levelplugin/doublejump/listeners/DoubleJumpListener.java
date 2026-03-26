package me.nakilex.levelplugin.doublejump.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DoubleJumpListener implements Listener {

    private static final double BASE_LIFT_VELOCITY       = 0.3;   // how much up
    private static final double BASE_FORWARD_VELOCITY    = 0.5;   // how much forward
    private static final double AGI_LIFT_MULTIPLIER      = 0.005; // extra up per Agi
    private static final double AGI_FORWARD_MULTIPLIER   = 0.02;  // extra forward per Agi
    private static final double MAX_LIFT_VELOCITY        = 0.6;
    private static final double MAX_FORWARD_VELOCITY     = 1.2;
    private final Map<UUID, Integer> remainingJumps = new HashMap<>();

    private int getBaseAirJumps(PlayerClass playerClass) {
        if (ClassUtil.isArcherFamily(playerClass) || ClassUtil.isRogueFamily(playerClass)) {
            // Archer/Rogue families keep their existing multi-jump behavior.
            return 2;
        }
        return 0;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerClass playerClass = StatsManager.getInstance()
            .getPlayerStats(player.getUniqueId())
            .playerClass;
        refreshJumpCharges(player, playerClass);
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (Main.getInstance().getDialogManager() != null
                && Main.getInstance().getDialogManager().hasSession(player)) {
            return;
        }

        StatsManager.PlayerStats ps = StatsManager
            .getInstance()
            .getPlayerStats(player.getUniqueId());
        int remaining = remainingJumps.getOrDefault(player.getUniqueId(), 0);
        if (remaining <= 0) return;

        if (!player.isFlying()) {
            event.setCancelled(true);
            remaining--;
            remainingJumps.put(player.getUniqueId(), remaining);
            player.setAllowFlight(remaining > 0);

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
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(),
                30, 0.5, 0.1, 0.5, 0.1);
            player.getWorld().playSound(player.getLocation(),
                Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);

            if (ClassUtil.isRogueFamily(ps.playerClass)) {
                Main plugin = Main.getInstance();
                ArcSlashCombatUtil.strikeForward(player, 1.35, 1.0, 3.8, 1.65);
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> ArcSlashCombatUtil.strikeForward(player, 1.85, 1.1, 4.4, 1.75), 2L);
            }
        }
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        StatsManager.PlayerStats ps = StatsManager
            .getInstance()
            .getPlayerStats(player.getUniqueId());

        if (player.isOnGround()) {
            refreshJumpCharges(player, ps.playerClass);
        }
    }

    private void refreshJumpCharges(Player player, PlayerClass playerClass) {
        int baseJumps = getBaseAirJumps(playerClass);
        int bonusJumps = getBonusJumps(player.getUniqueId());
        int total = Math.max(0, baseJumps + bonusJumps);
        remainingJumps.put(player.getUniqueId(), total);
        if (player.getGameMode() == GameMode.ADVENTURE) {
            player.setAllowFlight(total > 0);
        }
    }

    private int getBonusJumps(UUID playerId) {
        PetManager petManager = Main.getInstance().getPetManager();
        if (petManager == null) {
            return 0;
        }
        double bonus = petManager.getActiveEffectValue(playerId, PetEffectType.EXTRA_JUMP);
        return (int) Math.floor(Math.max(0.0, bonus));
    }
}
