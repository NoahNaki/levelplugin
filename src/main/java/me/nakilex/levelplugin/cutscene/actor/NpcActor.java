package me.nakilex.levelplugin.cutscene.actor;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.CutsceneIO;
import me.nakilex.levelplugin.cutscene.effects.CutsceneEffects;
import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;
import me.nakilex.levelplugin.pathfinding.PathSession;
import me.nakilex.levelplugin.pathfinding.PathfindingManager;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.pathfinding.npc.PathNpcFactory;
import me.nakilex.levelplugin.pathfinding.npc.RogueMercenary;
import me.nakilex.levelplugin.utils.MobUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Citizens backed actor driven by the pathfinding module. */
public class NpcActor implements CutsceneActor {
    private final ActorDefinition definition;
    private final Main plugin;
    private final PathNpc profile;
    private NPC npc;
    private PathSession activeSession;

    public NpcActor(ActorDefinition definition, Main plugin) {
        this.definition = definition;
        this.plugin = plugin;
        this.profile = PathNpcFactory.fromId(definition.profileId()).orElseGet(RogueMercenary::new);
    }

    @Override
    public String getName() {
        return definition.name();
    }

    @Override
    public void spawn(CutsceneContext context) {
        if (npc != null && npc.isSpawned()) {
            return;
        }
        npc = CitizensAPI.getNPCRegistry().createNPC(profile.type(), profile.name());
        Location spawn = definition.spawn();
        if (spawn == null && definition.defaultPath() != null) {
            List<Location> points = plugin.getPathfindingManager().getPathPoints(definition.defaultPath());
            if (!points.isEmpty()) {
                spawn = points.get(0);
            }
        }
        if (spawn == null) {
            spawn = context.getViewer().getLocation();
        }
        npc.spawn(spawn);
        profile.equip(npc);
        if (definition.lookAt() != null && npc.getEntity() instanceof LivingEntity living) {
            MobUtil.faceEntity(living, definition.lookAt());
        }
        if (!definition.spawnEffects().isEmpty()) {
            context.playEffectsToAll(definition.spawnEffects(), spawn);
        }
    }

    @Override
    public void despawn() {
        if (activeSession != null) {
            activeSession.stop();
            activeSession = null;
        }
        if (npc != null) {
            if (npc.isSpawned()) {
                npc.despawn();
            }
            npc.destroy();
            npc = null;
        }
    }

    @Override
    public CompletableFuture<Void> performAction(String action, CutsceneContext context, Map<String, Object> parameters) {
        if (action == null) {
            return CompletableFuture.completedFuture(null);
        }
        String normalized = action.toLowerCase();
        switch (normalized) {
            case "path" -> {
                return runPath(parameters);
            }
            case "look" -> {
                lookAt(context, parameters);
                return CompletableFuture.completedFuture(null);
            }
            case "effect" -> {
                playEffect(context, parameters);
                return CompletableFuture.completedFuture(null);
            }
            default -> {
                return CompletableFuture.completedFuture(null);
            }
        }
    }

    private CompletableFuture<Void> runPath(Map<String, Object> parameters) {
        ensureNpc();
        String pathName = parameters != null && parameters.get("path") instanceof String s ? s : definition.defaultPath();
        if (pathName == null) {
            return CompletableFuture.completedFuture(null);
        }
        PathfindingManager manager = plugin.getPathfindingManager();
        List<Location> points = manager.getPathPoints(pathName);
        if (points.isEmpty()) {
            plugin.getLogger().warning("Cutscene actor '" + definition.name() + "' missing path '" + pathName + "'");
            return CompletableFuture.completedFuture(null);
        }
        if (activeSession != null) {
            activeSession.stop();
        }
        activeSession = new PathSession(plugin, points, profile, npc);
        CompletableFuture<Void> future = activeSession.getCompletionFuture();
        future.thenRun(() -> activeSession = null);
        activeSession.start();
        return future;
    }

    private void lookAt(CutsceneContext context, Map<String, Object> parameters) {
        if (npc == null || !npc.isSpawned() || !(npc.getEntity() instanceof LivingEntity living)) {
            return;
        }
        Location target = context.getViewer().getLocation();
        if (parameters != null) {
            Object pos = parameters.get("pos");
            if (pos instanceof String str) {
                Location parsed = CutsceneIO.parseVector(plugin, living.getWorld().getName(), str);
                if (parsed != null) {
                    target = parsed;
                }
            } else if (parameters.containsKey("viewer")) {
                // default already viewer
            }
        }
        MobUtil.faceEntity(living, target);
    }

    private void playEffect(CutsceneContext context, Map<String, Object> parameters) {
        if (parameters == null) {
            return;
        }
        Object effectName = parameters.get("bundle");
        if (effectName instanceof String name) {
            CutsceneEffects.play(context.getViewer(), context.resolveEffectBundle(name), getCurrentLocation(), plugin);
        }
    }

    private void ensureNpc() {
        if (npc == null) {
            npc = CitizensAPI.getNPCRegistry().createNPC(profile.type(), profile.name());
            Location spawn = definition.spawn();
            if (spawn != null) {
                npc.spawn(spawn);
            }
            profile.equip(npc);
        }
    }

    @Override
    public Location getCurrentLocation() {
        if (npc != null && npc.isSpawned()) {
            return npc.getEntity().getLocation().clone();
        }
        return definition.spawn();
    }

    @Override
    public boolean isPersistent() {
        return definition.persistent();
    }
}
