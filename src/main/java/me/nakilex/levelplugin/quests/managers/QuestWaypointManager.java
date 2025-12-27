package me.nakilex.levelplugin.quests.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestWaypointManager implements Listener {

    private static final int COMPASS_SEGMENTS = 31;
    private static final int ACTIONBAR_SEGMENTS = 13;
    private static final double MIN_DISTANCE_FOR_INDICATORS = 2.0;

    private final Main plugin;
    private final BeaconManager beaconManager;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, MultiLineHologram> holograms = new HashMap<>();
    private final Map<UUID, BlinkState> blinkingBlocks = new HashMap<>();
    private final Map<UUID, Location> previousCompassTargets = new HashMap<>();

    public QuestWaypointManager(Main plugin, BeaconManager beaconManager) {
        this.plugin = plugin;
        this.beaconManager = beaconManager;
    }

    public void update(Player player, Location target, String label) {
        if (player == null) {
            return;
        }
        if (target == null || target.getWorld() == null || !target.getWorld().equals(player.getWorld())) {
            clear(player);
            return;
        }

        Location playerLoc = player.getLocation();
        double distance = playerLoc.distance(target);
        boolean withinRange = distance >= MIN_DISTANCE_FOR_INDICATORS;

        updateBeacon(player, target, distance, withinRange);
        updateBossBar(player, target, distance, withinRange);
        updateActionBar(player, target, distance, withinRange);
        updateCompass(player, target, withinRange);
        updateParticles(player, target, distance, withinRange);
        updateTrail(player, target, distance, withinRange);
        updateHologram(player, target, label, distance, withinRange);
        updateBlinkingBlock(player, target, distance, withinRange);

    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        beaconManager.removeBeam(player);
        clearBossBar(player);
        clearActionBar(player);
        clearCompass(player);
        clearHologram(player);
        clearBlinkingBlock(player);
    }

    public void removeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        holograms.values().forEach(MultiLineHologram::despawn);
        holograms.clear();
        blinkingBlocks.clear();
        previousCompassTargets.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clear(event.getPlayer());
    }

    private void updateBeacon(Player player, Location target, double distance, boolean withinRange) {
        if (!isEnabled("quest-tracking.indicators.beacon", true)) {
            beaconManager.removeBeam(player);
            return;
        }
        double hideDistance = getDouble("quest-tracking.distances.beacon-hide", 10.0);
        if (!withinRange || distance < hideDistance) {
            beaconManager.removeBeam(player);
            return;
        }
        Location beaconTarget = target.clone();
        double leadStart = getDouble("quest-tracking.distances.beacon-lead-start", 64.0);
        if (distance > leadStart) {
            double leadFactor = getDouble("quest-tracking.distances.beacon-lead-factor", 0.6);
            double maxLead = getDouble("quest-tracking.distances.beacon-lead-max", 80.0);
            double lead = Math.min(maxLead, distance * leadFactor);
            Vector dir = target.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
            beaconTarget = player.getLocation().clone().add(dir.multiply(lead));
            beaconTarget.setY(player.getLocation().getY());
        }
        beaconManager.showBeam(player, beaconTarget);
    }

    private void updateBossBar(Player player, Location target, double distance, boolean withinRange) {
        if (!isEnabled("quest-tracking.indicators.bossbar", true)) {
            clearBossBar(player);
            return;
        }
        if (!withinRange) {
            clearBossBar(player);
            return;
        }
        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), id -> {
            BossBar created = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
            created.addPlayer(player);
            return created;
        });
        bar.addPlayer(player);
        bar.setProgress(1.0);
        String compass = buildCompass(player, target, COMPASS_SEGMENTS);
        bar.setTitle(compass + ChatColor.GRAY + " " + Math.round(distance) + "m");
    }

    private void clearBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    private void updateActionBar(Player player, Location target, double distance, boolean withinRange) {
        if (!isEnabled("quest-tracking.indicators.actionbar", true)) {
            clearActionBar(player);
            return;
        }
        if (!withinRange) {
            clearActionBar(player);
            return;
        }
        String indicator = buildCompass(player, target, ACTIONBAR_SEGMENTS);
        String message = ChatColor.YELLOW + indicator + ChatColor.GRAY + " " + Math.round(distance) + "m";
        player.sendActionBar(Component.text(message));
    }

    private void clearActionBar(Player player) {
        player.sendActionBar(Component.text(""));
    }

    private void updateCompass(Player player, Location target, boolean withinRange) {
        if (!isEnabled("quest-tracking.indicators.compass", true) || !withinRange) {
            clearCompass(player);
            return;
        }
        if (!hasCompass(player)) {
            return;
        }
        previousCompassTargets.putIfAbsent(player.getUniqueId(), player.getCompassTarget());
        player.setCompassTarget(target);
    }

    private void clearCompass(Player player) {
        Location previous = previousCompassTargets.remove(player.getUniqueId());
        if (previous != null) {
            player.setCompassTarget(previous);
        }
    }

    private void updateParticles(Player player, Location target, double distance, boolean withinRange) {
        if (!isEnabled("quest-tracking.indicators.particles", true)) {
            return;
        }
        double maxDistance = getDouble("quest-tracking.distances.particles-range", 48.0);
        if (!withinRange || distance > maxDistance) {
            return;
        }
        Location base = player.getLocation().clone().add(0, 0.2, 0);
        Vector dir = target.toVector().subtract(base.toVector()).setY(0).normalize();
        for (int i = 1; i <= 3; i++) {
            Location point = base.clone().add(dir.clone().multiply(0.6 * i));
            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }
    }

    private void updateTrail(Player player, Location target, double distance, boolean withinRange) {
        if (!isEnabled("quest-tracking.indicators.trail", true)) {
            return;
        }
        double maxDistance = getDouble("quest-tracking.distances.trail-range", 48.0);
        if (!withinRange || distance > maxDistance) {
            return;
        }
        Vector dir = target.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
        int steps = Math.min(8, (int) Math.max(3, distance / 6.0));
        for (int i = 1; i <= steps; i++) {
            Location step = player.getLocation().clone().add(dir.clone().multiply(2.0 * i));
            Location surface = LocationUtils.surfaceBelow(step, true);
            if (surface == null || surface.getWorld() == null) {
                continue;
            }
            Location point = surface.clone().add(0, 0.15, 0);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, point, 2, 0.15, 0, 0.15, 0);
        }
    }

    private void updateHologram(Player player, Location target, String label, double distance, boolean withinRange) {
        if (!isEnabled("quest-tracking.indicators.hologram", true)) {
            clearHologram(player);
            return;
        }
        double maxDistance = getDouble("quest-tracking.distances.hologram-range", 96.0);
        if (!withinRange || distance > maxDistance) {
            clearHologram(player);
            return;
        }
        Location base = LocationUtils.aboveSurface(target);
        if (base == null || base.getWorld() == null) {
            clearHologram(player);
            return;
        }
        Location holoLoc = base.clone().add(0, 2.2, 0);
        String title = label == null || label.isBlank() ? "Tracked Objective" : label;
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Waypoint");
        lines.add(ChatColor.WHITE + title);
        MultiLineHologram hologram = holograms.get(player.getUniqueId());
        boolean needsRespawn = hologram == null
                || hologram.getLocation() == null
                || !hologram.getLocation().getWorld().equals(holoLoc.getWorld())
                || hologram.getLocation().distanceSquared(holoLoc) > 1.0;
        if (needsRespawn) {
            if (hologram != null) {
                hologram.despawn();
            }
            hologram = new MultiLineHologram(holoLoc);
            hologram.spawn(lines);
            hideHologramFromOthers(player, hologram);
            holograms.put(player.getUniqueId(), hologram);
        } else {
            hologram.setLines(lines);
        }
    }

    private void clearHologram(Player player) {
        MultiLineHologram hologram = holograms.remove(player.getUniqueId());
        if (hologram != null) {
            hologram.despawn();
        }
    }

    private void updateBlinkingBlock(Player player, Location target, double distance, boolean withinRange) {
        if (!isEnabled("quest-tracking.indicators.blinking-block", true)) {
            clearBlinkingBlock(player);
            return;
        }
        double maxDistance = getDouble("quest-tracking.distances.blinking-block-range", 12.0);
        if (!withinRange || distance > maxDistance) {
            clearBlinkingBlock(player);
            return;
        }
        Location surface = LocationUtils.surfaceBelow(target, true);
        if (surface == null || surface.getWorld() == null) {
            clearBlinkingBlock(player);
            return;
        }
        Location blockLoc = surface.getBlock().getLocation();
        BlinkState state = blinkingBlocks.get(player.getUniqueId());
        if (state == null || !state.isSameBlock(blockLoc)) {
            clearBlinkingBlock(player);
            BlockData original = blockLoc.getBlock().getBlockData();
            state = new BlinkState(blockLoc, original);
            blinkingBlocks.put(player.getUniqueId(), state);
        }
        state.toggle();
        BlockData display = (state.isAlternate() ? Material.SEA_LANTERN : Material.BEACON).createBlockData();
        player.sendBlockChange(state.blockLocation, display);
    }

    private void clearBlinkingBlock(Player player) {
        BlinkState state = blinkingBlocks.remove(player.getUniqueId());
        if (state != null && state.originalData != null) {
            player.sendBlockChange(state.blockLocation, state.originalData);
        }
    }

    private void hideHologramFromOthers(Player viewer, MultiLineHologram hologram) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(viewer)) {
                continue;
            }
            for (TextDisplay display : hologram.getDisplays()) {
                other.hideEntity(plugin, display);
            }
        }
    }

    private boolean hasCompass(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.getType() == Material.COMPASS) {
            return true;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.COMPASS) {
            return true;
        }
        return player.getInventory().contains(Material.COMPASS);
    }

    private String buildCompass(Player player, Location target, int segments) {
        double playerYaw = normalizeYaw(player.getLocation().getYaw());
        double targetYaw = calculateYawToTarget(player.getLocation(), target);
        double delta = wrapDegrees(targetYaw - playerYaw);
        int pointerIndex = (int) Math.round((delta + 180.0) / 360.0 * (segments - 1));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments; i++) {
            double relative = (i / (double) (segments - 1)) * 360.0 - 180.0;
            double worldYaw = wrapDegrees(playerYaw + relative);
            String marker = markerForYaw(worldYaw);
            boolean highlight = i == pointerIndex;
            if (highlight) {
                builder.append(ChatColor.GOLD).append('▲');
                continue;
            }
            if (marker != null) {
                builder.append(ChatColor.WHITE).append(marker);
            } else {
                builder.append(ChatColor.DARK_GRAY).append('•');
            }
        }
        return builder.toString();
    }

    private String markerForYaw(double yaw) {
        double north = 180.0;
        if (Math.abs(wrapDegrees(yaw - north)) < 8.0) {
            return "N";
        }
        if (Math.abs(wrapDegrees(yaw - -90.0)) < 8.0) {
            return "E";
        }
        if (Math.abs(wrapDegrees(yaw - 0.0)) < 8.0) {
            return "S";
        }
        if (Math.abs(wrapDegrees(yaw - 90.0)) < 8.0) {
            return "W";
        }
        return null;
    }

    private double calculateYawToTarget(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    private double normalizeYaw(double yaw) {
        double adjusted = yaw % 360.0;
        if (adjusted < -180.0) {
            adjusted += 360.0;
        } else if (adjusted > 180.0) {
            adjusted -= 360.0;
        }
        return adjusted;
    }

    private double wrapDegrees(double degrees) {
        double value = degrees % 360.0;
        if (value >= 180.0) {
            value -= 360.0;
        }
        if (value < -180.0) {
            value += 360.0;
        }
        return value;
    }

    private boolean isEnabled(String path, boolean def) {
        FileConfiguration cfg = plugin.getCustomConfig();
        return cfg == null ? def : cfg.getBoolean(path, def);
    }

    private double getDouble(String path, double def) {
        FileConfiguration cfg = plugin.getCustomConfig();
        return cfg == null ? def : cfg.getDouble(path, def);
    }

    private static class BlinkState {
        private final Location blockLocation;
        private final BlockData originalData;
        private boolean alternate;

        private BlinkState(Location blockLocation, BlockData originalData) {
            this.blockLocation = blockLocation;
            this.originalData = originalData;
        }

        private void toggle() {
            alternate = !alternate;
        }

        private boolean isAlternate() {
            return alternate;
        }

        private boolean isSameBlock(Location other) {
            if (other == null || blockLocation == null) {
                return false;
            }
            if (blockLocation.getWorld() == null || other.getWorld() == null) {
                return false;
            }
            return blockLocation.getWorld().equals(other.getWorld())
                    && blockLocation.getBlockX() == other.getBlockX()
                    && blockLocation.getBlockY() == other.getBlockY()
                    && blockLocation.getBlockZ() == other.getBlockZ();
        }
    }
}
