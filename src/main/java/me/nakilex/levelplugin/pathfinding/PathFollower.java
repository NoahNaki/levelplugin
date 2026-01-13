package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.utils.cooldowns.CooldownManager;
import me.nakilex.levelplugin.utils.MobUtil;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class PathFollower {
    private final Plugin plugin;
    private final NPC npc;
    private final List<Location> points;
    private final PathNpc profile;
    private final boolean cleanupOnComplete;
    private final Runnable onComplete;
    private final int gearScore;
    private final CooldownManager cooldowns = CooldownManager.getInstance();
    private BukkitTask task;
    private int index = 1;
    private LivingEntity combatTarget;
    private boolean completed;
    private int tickCount;
    private static final int DEBUG_TICK_INTERVAL = 40;
    private static final double ARRIVAL_DISTANCE_SQ = 4.0;
    private static final double NAV_SPEED_BOOST = 3.0;

    public PathFollower(Plugin plugin,
                        NPC npc,
                        List<Location> points,
                        PathNpc profile,
                        boolean cleanupOnComplete,
                        Runnable onComplete,
                        int gearScore) {
        this.plugin = plugin;
        this.npc = npc;
        this.points = points;
        this.profile = profile;
        this.cleanupOnComplete = cleanupOnComplete;
        this.onComplete = onComplete;
        this.gearScore = Math.max(0, gearScore);
    }

    public PathFollower(Plugin plugin,
                        NPC npc,
                        List<Location> points,
                        PathNpc profile,
                        boolean cleanupOnComplete,
                        Runnable onComplete) {
        this(plugin, npc, points, profile, cleanupOnComplete, onComplete, 0);
    }

    public static PathFollower spawnNpc(Plugin plugin,
                                        List<Location> points,
                                        PathNpc profile,
                                        boolean cleanupOnComplete,
                                        Runnable onComplete) {
        NPC npc = NpcApi.getRegistry().createNPC(profile.type(), profile.name());
        return new PathFollower(plugin, npc, points, profile, cleanupOnComplete, onComplete, 0);
    }

    public NPC getNpc() {
        return npc;
    }

    public void start() {
        if (points == null || points.isEmpty()) {
            cleanup();
            return;
        }
        if (!ensureSameWorld(points.get(0))) {
            plugin.getLogger().warning("[PathfindingDebug] Path has no valid world; aborting.");
            cleanup();
            return;
        }
        plugin.getLogger().info("[PathfindingDebug] Starting follower for " + profile.name()
                + " points=" + points.size()
                + " start=" + formatLocation(points.get(0)));
        ensureChunkLoaded(points.get(0));
        if (!npc.isSpawned()) {
            npc.spawn(points.get(0));
            if (!npc.isSpawned()) {
                plugin.getLogger().warning("[PathfindingDebug] Failed to spawn NPC at " + points.get(0));
                cleanup();
                return;
            }
        } else {
            npc.teleport(points.get(0), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
        npc.getNavigator().cancelNavigation();

        var params = npc.getNavigator().getDefaultParameters();
        params.baseSpeed((float) (params.baseSpeed() * profile.speedMultiplier() * NAV_SPEED_BOOST));
        params.range((float) resolveRange(points.get(0), points));
        params.stuckAction(null);
        profile.equip(npc);
        if (npc.getEntity() instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
            living.setPersistent(true);
            applyCombatStats(living, gearScore);
        }

        if (points.size() <= 1) {
            completePath();
            return;
        }
        ensureChunkLoaded(points.get(1));
        npc.getNavigator().setTarget(points.get(1));
        plugin.getLogger().info("[PathfindingDebug] Moving to point 1");
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void stop() {
        cleanup();
    }

    private void tick() {
        if (!npc.isSpawned()) {
            cleanup();
            return;
        }
        tickCount++;
        if (tickCount % DEBUG_TICK_INTERVAL == 0) {
            logDebugState("tick");
        }

        if (combatTarget != null) {
            if (combatTarget.isDead() || !combatTarget.isValid()) {
                plugin.getLogger().info("[PathfindingDebug] Killed all targets in vicinity");
                combatTarget = findHostileTarget();
                if (combatTarget != null) {
                    plugin.getLogger().info("[PathfindingDebug] Targeting mob " + combatTarget.getName());
                    profile.handleCombat(npc, combatTarget, cooldowns);
                    return;
                }
            combatTarget = null;
            if (!completed) {
                npc.getNavigator().setTarget(points.get(index));
            }
            return;
            }
            profile.handleCombat(npc, combatTarget, cooldowns);
            return;
        }

        LivingEntity hostile = findHostileTarget();
        if (hostile != null) {
            combatTarget = hostile;
            plugin.getLogger().info("[PathfindingDebug] Targeting mob " + hostile.getName());
            profile.handleCombat(npc, combatTarget, cooldowns);
            return;
        }

        if (completed) {
            return;
        }

        Location current = points.get(index);
        if (!ensureSameWorld(current)) {
            plugin.getLogger().warning("[PathfindingDebug] Path point world mismatch; stopping path.");
            completePath();
            return;
        }
        ensureChunkLoaded(current);
        if (npc.getEntity().getLocation().distanceSquared(current) < ARRIVAL_DISTANCE_SQ) {
            if (++index >= points.size()) {
                completePath();
                return;
            }
            ensureChunkLoaded(points.get(index));
            npc.getNavigator().setTarget(points.get(index));
            plugin.getLogger().info("[PathfindingDebug] Moving to point " + index);
        } else if (!npc.getNavigator().isNavigating()) {
            npc.getNavigator().setTarget(current);
            plugin.getLogger().info("[PathfindingDebug] Reissuing target for point " + index);
        }
    }

    private LivingEntity findHostileTarget() {
        if (npc.getEntity() instanceof LivingEntity living) {
            return MobUtil.findNearestHostile(living, 10);
        }
        return null;
    }

    private void completePath() {
        completed = true;
        if (onComplete != null) {
            onComplete.run();
        }
        if (cleanupOnComplete) {
            cleanup();
        }
    }

    private void ensureChunkLoaded(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        var chunk = location.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load(true);
        }
    }

    private boolean ensureSameWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Entity entity = npc.getEntity();
        if (entity == null) {
            return true;
        }
        World entityWorld = entity.getWorld();
        if (entityWorld == null || entityWorld.equals(location.getWorld())) {
            return true;
        }
        entity.teleport(location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
        return true;
    }

    private void logDebugState(String reason) {
        if (npc.getEntity() == null) {
            plugin.getLogger().info("[PathfindingDebug] " + reason + " npc entity missing");
            return;
        }
        Location loc = npc.getEntity().getLocation();
        Location target = (index < (points == null ? 0 : points.size())) ? points.get(index) : null;
        plugin.getLogger().info("[PathfindingDebug] " + reason
                + " npc=" + npc.getId()
                + " idx=" + index + "/" + (points == null ? 0 : points.size())
                + " navigating=" + npc.getNavigator().isNavigating()
                + " loc=" + formatLocation(loc)
                + " target=" + (target != null ? formatLocation(target) : "none")
                + " dist=" + (target != null ? String.format("%.1f", loc.distance(target)) : "n/a"));
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return location.getWorld().getName() + ":"
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private void applyCombatStats(LivingEntity living, int gearScore) {
        if (gearScore <= 0) {
            return;
        }
        if (living instanceof org.bukkit.entity.Player player) {
            applyPlayerStats(player, gearScore);
            return;
        }
        org.bukkit.attribute.Attribute attackAttr = me.nakilex.levelplugin.utils.AttributeUtil
                .resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
        org.bukkit.attribute.AttributeInstance attack = attackAttr == null ? null : living.getAttribute(attackAttr);
        if (attack != null) {
            double base = Math.max(1.0, attack.getBaseValue());
            double bonus = Math.max(0.0, gearScore / 75.0);
            attack.setBaseValue(base + bonus);
        }
        org.bukkit.attribute.Attribute healthAttr = me.nakilex.levelplugin.utils.AttributeUtil
                .resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        org.bukkit.attribute.AttributeInstance health = healthAttr == null ? null : living.getAttribute(healthAttr);
        if (health != null) {
            double base = Math.max(1.0, health.getBaseValue());
            double bonus = Math.max(0.0, gearScore / 15.0);
            health.setBaseValue(base + bonus);
            living.setHealth(Math.min(health.getBaseValue(), health.getValue()));
        }
    }

    private void applyPlayerStats(org.bukkit.entity.Player player, int gearScore) {
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        applyMovementSpeed(player, gearScore);
        me.nakilex.levelplugin.player.attributes.managers.StatsManager statsManager =
                me.nakilex.levelplugin.player.attributes.managers.StatsManager.getInstance();
        statsManager.resetPlayer(player.getUniqueId());
        int total = Math.max(1, gearScore / 20);
        int vit = Math.max(1, total / 2);
        int str = Math.max(1, total - vit);
        var stats = statsManager.getPlayerStats(player.getUniqueId());
        statsManager.setBaseStat(stats, me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType.VIT, vit);
        statsManager.setBaseStat(stats, me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType.STR, str);
        statsManager.recalcDerivedStats(player);
    }

    private void applyMovementSpeed(org.bukkit.entity.Player player, int gearScore) {
        org.bukkit.attribute.Attribute speedAttr = me.nakilex.levelplugin.utils.AttributeUtil
                .resolve("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");
        org.bukkit.attribute.AttributeInstance speed = speedAttr == null ? null : player.getAttribute(speedAttr);
        if (speed == null) {
            return;
        }
        double base = 0.25;
        double bonus = Math.min(0.35, gearScore / 2500.0);
        speed.setBaseValue(base + bonus);
    }

    private double resolveRange(Location start, List<Location> points) {
        if (start == null || points == null || points.isEmpty()) {
            return 64;
        }
        double max = 64;
        for (Location point : points) {
            if (point != null && point.getWorld() != null && point.getWorld().equals(start.getWorld())) {
                max = Math.max(max, start.distance(point));
            }
        }
        return Math.max(64, max + 16);
    }

    private void cleanup() {
        if (task != null) {
            task.cancel();
        }
        if (npc.isSpawned()) {
            npc.despawn();
        }
        npc.destroy();
    }
}
