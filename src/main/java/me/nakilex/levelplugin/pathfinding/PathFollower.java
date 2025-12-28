package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.utils.MobUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.event.NavigationStuckEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class PathFollower implements Listener {
    private final Plugin plugin;
    private final NPC npc;
    private final List<Location> points;
    private final PathNpc profile;
    private final boolean cleanupOnComplete;
    private final Runnable onComplete;
    private final CooldownManager cooldowns = CooldownManager.getInstance();
    private BukkitTask task;
    private int index = 1;
    private LivingEntity combatTarget;
    private boolean completed;
    private int tickCount;
    private static final int DEBUG_TICK_INTERVAL = 40;

    public PathFollower(Plugin plugin,
                        NPC npc,
                        List<Location> points,
                        PathNpc profile,
                        boolean cleanupOnComplete,
                        Runnable onComplete) {
        this.plugin = plugin;
        this.npc = npc;
        this.points = points;
        this.profile = profile;
        this.cleanupOnComplete = cleanupOnComplete;
        this.onComplete = onComplete;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static PathFollower spawnNpc(Plugin plugin,
                                        List<Location> points,
                                        PathNpc profile,
                                        boolean cleanupOnComplete,
                                        Runnable onComplete) {
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(profile.type(), profile.name());
        return new PathFollower(plugin, npc, points, profile, cleanupOnComplete, onComplete);
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
        params.baseSpeed(params.baseSpeed() * profile.speedMultiplier());
        params.range(64);
        params.stuckAction(null);
        profile.equip(npc);
        if (npc.getEntity() instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
            living.setPersistent(true);
        }

        if (points.size() <= 1) {
            completePath();
            return;
        }
        ensureChunkLoaded(points.get(1));
        if (!npc.getNavigator().setTarget(points.get(1))) {
            plugin.getLogger().warning("[PathfindingDebug] Failed to set target point 1 " + formatLocation(points.get(1)));
        }
        plugin.getLogger().info("[PathfindingDebug] Moving to point 1");
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
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
        if (npc.getEntity().getLocation().distanceSquared(current) < 1) {
            if (++index >= points.size()) {
                completePath();
                return;
            }
            ensureChunkLoaded(points.get(index));
            if (!npc.getNavigator().setTarget(points.get(index))) {
                plugin.getLogger().warning("[PathfindingDebug] Failed to advance to point " + index
                        + " target=" + formatLocation(points.get(index)));
            }
            plugin.getLogger().info("[PathfindingDebug] Moving to point " + index);
        } else if (!npc.getNavigator().isNavigating()) {
            if (!npc.getNavigator().setTarget(current)) {
                plugin.getLogger().warning("[PathfindingDebug] Failed to reissue point " + index
                        + " target=" + formatLocation(current));
            }
            plugin.getLogger().info("[PathfindingDebug] Reissuing target for point " + index);
        }
    }

    @EventHandler
    public void onStuck(NavigationStuckEvent event) {
        if (!event.getNPC().equals(npc)) {
            return;
        }
        if (combatTarget != null && combatTarget.isValid()) {
            npc.getNavigator().setTarget(combatTarget, true);
            return;
        }
        if (!completed && points != null && index < points.size()) {
            npc.getNavigator().setTarget(points.get(index));
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
        plugin.getLogger().info("[PathfindingDebug] " + reason
                + " npc=" + npc.getId()
                + " idx=" + index + "/" + (points == null ? 0 : points.size())
                + " navigating=" + npc.getNavigator().isNavigating()
                + " loc=" + formatLocation(loc)
                + " target=" + (index < (points == null ? 0 : points.size()) ? formatLocation(points.get(index)) : "none"));
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return location.getWorld().getName() + ":"
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private void cleanup() {
        if (task != null) {
            task.cancel();
        }
        if (npc.isSpawned()) {
            npc.despawn();
        }
        npc.destroy();
        HandlerList.unregisterAll(this);
    }
}
