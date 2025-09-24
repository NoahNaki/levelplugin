package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.event.NavigationStuckEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reusable Citizens navigation session that walks an NPC along a stored set of
 * path points while reusing the combat behaviour from {@link PathNpc}
 * implementations. The previous {@code PathRunner} inner class in
 * {@link PathfindingManager} has been promoted so other systems (cutscenes)
 * can orchestrate NPC movement without duplicating the logic.
 */
public class PathSession implements Listener {
    private final Plugin plugin;
    private final List<Location> points;
    private final PathNpc profile;
    private final NPC npc;
    private final boolean ownsNpc;
    private final CooldownManager cooldowns = CooldownManager.getInstance();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();
    private BukkitTask task;
    private LivingEntity combatTarget;
    private int index = 1;
    private Runnable completion = () -> {};
    private final CompletableFuture<Void> completionFuture = new CompletableFuture<>();

    public PathSession(Plugin plugin, List<Location> points, PathNpc profile) {
        this(plugin, points, profile, null);
    }

    public PathSession(Plugin plugin, List<Location> points, PathNpc profile, NPC existing) {
        this.plugin = plugin;
        this.points = points;
        this.profile = profile;
        if (existing == null) {
            EntityType type = profile.type();
            this.npc = CitizensAPI.getNPCRegistry().createNPC(type, profile.name());
            this.ownsNpc = true;
        } else {
            this.npc = existing;
            this.ownsNpc = false;
        }
    }

    /** Starts the navigation session if it hasn't begun yet. */
    public void start() {
        if (started.getAndSet(true)) {
            return;
        }
        if (points == null || points.isEmpty()) {
            finish();
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Location first = points.get(0);
        if (!npc.isSpawned()) {
            npc.spawn(first);
        } else {
            npc.teleport(first, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }

        var params = npc.getNavigator().getDefaultParameters();
        params.baseSpeed(params.baseSpeed() * profile.speedMultiplier());
        profile.equip(npc);

        if (points.size() <= 1) {
            return;
        }
        npc.getNavigator().setTarget(points.get(1));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    private void tick() {
        if (finished.get()) {
            return;
        }
        if (combatTarget != null) {
            if (combatTarget.isDead() || !combatTarget.isValid()) {
                combatTarget = null;
                npc.getNavigator().setTarget(points.get(index));
                return;
            }
            profile.handleCombat(npc, combatTarget, cooldowns);
            return;
        }

        if (npc.getEntity() instanceof LivingEntity living) {
            LivingEntity hostile = me.nakilex.levelplugin.utils.MobUtil.findNearestHostile(living, 10);
            if (hostile != null) {
                combatTarget = hostile;
                profile.handleCombat(npc, hostile, cooldowns);
                return;
            }
        }

        Location current = points.get(index);
        if (npc.getEntity().getLocation().distanceSquared(current) < 1) {
            if (++index >= points.size()) {
                finish();
                return;
            }
            npc.getNavigator().setTarget(points.get(index));
        } else if (!npc.getNavigator().isNavigating()) {
            npc.getNavigator().setTarget(current);
        }
    }

    @EventHandler
    public void onStuck(NavigationStuckEvent event) {
        if (!event.getNPC().equals(npc) || finished.get()) {
            return;
        }
        if (combatTarget != null && combatTarget.isValid()) {
            npc.getNavigator().setTarget(combatTarget, true);
        } else {
            npc.getNavigator().setTarget(points.get(index));
        }
    }

    /**
     * Stops the session, despawning the NPC and cleaning up listeners. Safe to
     * call multiple times.
     */
    public void stop() {
        if (finished.getAndSet(true)) {
            return;
        }
        if (task != null) {
            task.cancel();
        }
        if (ownsNpc) {
            if (npc.isSpawned()) {
                npc.despawn();
            }
            npc.destroy();
        } else {
            npc.getNavigator().cancelNavigation();
        }
        HandlerList.unregisterAll(this);
        completion.run();
        completionFuture.complete(null);
    }

    private void finish() {
        stop();
    }

    /**
     * @return the Citizens NPC backing this session. Will be spawned once
     * {@link #start()} is invoked.
     */
    public NPC getNpc() {
        return npc;
    }

    /**
     * Provides a future that is completed when the navigation finishes or the
     * session is stopped manually.
     */
    public CompletableFuture<Void> getCompletionFuture() {
        return completionFuture;
    }

    /**
     * Sets a callback invoked when the path completes or the session ends.
     */
    public void setCompletion(Runnable completion) {
        this.completion = completion == null ? () -> {} : completion;
        if (finished.get()) {
            this.completion.run();
        }
    }
}
