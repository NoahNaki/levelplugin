package me.nakilex.levelplugin.cutscene.playback;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.Cutscene;
import me.nakilex.levelplugin.cutscene.actor.ActorDefinition;
import me.nakilex.levelplugin.cutscene.actor.CutsceneActor;
import me.nakilex.levelplugin.cutscene.effects.CutsceneEffects;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import me.nakilex.levelplugin.cutscene.frames.Frame;
import me.nakilex.levelplugin.cutscene.events.CutsceneEndEvent;
import me.nakilex.levelplugin.cutscene.events.CutsceneStartEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** Handles the runtime state of an executing cutscene. */
public class CutscenePlayback {
    private final Main plugin;
    private final Cutscene cutscene;
    private final List<Player> viewers;
    private final Map<UUID, PlayerState> states = new LinkedHashMap<>();
    private final Map<UUID, List<BukkitTask>> movementTasks = new LinkedHashMap<>();
    private final List<BukkitTask> scheduled = new ArrayList<>();
    private final Map<String, CutsceneActor> actors = new LinkedHashMap<>();
    private final Map<String, CompletableFuture<Void>> actorFutures = new LinkedHashMap<>();
    private final List<CompletableFuture<Void>> awaits = new ArrayList<>();
    private final List<CutsceneContext> contexts = new ArrayList<>();
    private final Runnable onFinish;
    private boolean stopped;
    private int currentFrame = -1;
    private long elapsedTicks = 0L;
    private long totalTicks = 0L;
    private Location endLocation;

    public CutscenePlayback(Main plugin, Cutscene cutscene, List<Player> viewers, Runnable onFinish) {
        this.plugin = plugin;
        this.cutscene = cutscene;
        this.viewers = new ArrayList<>(viewers);
        this.onFinish = onFinish;
    }

    public Main getPlugin() {
        return plugin;
    }

    public Cutscene getCutscene() {
        return cutscene;
    }

    public Collection<Player> getViewers() {
        return viewers.stream().filter(Player::isOnline).collect(Collectors.toList());
    }

    public Optional<CutsceneActor> getActor(String name) {
        return Optional.ofNullable(actors.get(name));
    }

    public void start() {
        if (viewers.isEmpty()) {
            finish();
            return;
        }
        Location end = cutscene.resolveEndLocation();
        this.endLocation = end == null ? null : end.clone();
        int index = 0;
        for (Player viewer : viewers) {
            states.put(viewer.getUniqueId(), new PlayerState(viewer));
            viewer.setGameMode(GameMode.SPECTATOR);
            viewer.setAllowFlight(true);
            viewer.setFlying(true);
            var sb = plugin.getScoreboardManager();
            if (sb != null) sb.removeBoard(viewer);
            contexts.add(new CutsceneContext(this, viewer, index++ == 0));
        }
        instantiateActors();
        totalTicks = cutscene.getFrames().stream()
                .mapToLong(frame -> Math.max(1L, frame.getDuration() / 50L))
                .sum();
        Bukkit.getPluginManager().callEvent(new CutsceneStartEvent(cutscene, getViewers()));
        playNextFrame(0);
    }

    private void instantiateActors() {
        if (cutscene.getActors().isEmpty()) {
            return;
        }
        CutsceneContext primary = contexts.isEmpty() ? null : contexts.get(0);
        for (Map.Entry<String, ActorDefinition> entry : cutscene.getActors().entrySet()) {
            CutsceneActor actor = entry.getValue().create(plugin);
            actors.put(entry.getKey(), actor);
            if (primary != null) {
                actor.spawn(primary);
            }
        }
    }

    private void playNextFrame(int frameIndex) {
        if (stopped) {
            return;
        }
        currentFrame = frameIndex;
        if (frameIndex >= cutscene.getFrames().size()) {
            finish();
            return;
        }
        Frame frame = cutscene.getFrames().get(frameIndex);
        for (CutsceneContext context : contexts) {
            BukkitTask task = frame.play(context);
            if (task != null) {
                movementTasks.computeIfAbsent(context.getViewer().getUniqueId(), k -> new ArrayList<>()).add(task);
            }
        }
        long ticks = Math.max(1L, frame.getDuration() / 50L);
        elapsedTicks += ticks;
        sendProgress();
        scheduleNext(frameIndex + 1, ticks);
    }

    private void scheduleNext(int nextIndex, long delay) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            CompletableFuture<Void> gate = combineAwaits();
            if (gate != null && !gate.isDone()) {
                gate.thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> playNextFrame(nextIndex)));
            } else {
                playNextFrame(nextIndex);
            }
        }, delay);
        scheduled.add(task);
    }

    private CompletableFuture<Void> combineAwaits() {
        if (awaits.isEmpty()) {
            return null;
        }
        CompletableFuture<Void> combined = CompletableFuture.allOf(awaits.toArray(new CompletableFuture[0]));
        awaits.clear();
        return combined;
    }

    public void await(CompletableFuture<?> future) {
        if (future == null) {
            return;
        }
        CompletableFuture<Void> converted = future.thenRun(() -> {});
        awaits.add(converted);
    }

    public void registerActorFuture(String actor, CompletableFuture<Void> future) {
        if (actor == null || future == null) {
            return;
        }
        actorFutures.put(actor.toLowerCase(), future);
        future.thenRun(() -> actorFutures.remove(actor.toLowerCase()));
    }

    public CompletableFuture<Void> getActorFuture(String actor) {
        if (actor == null) {
            return null;
        }
        return actorFutures.get(actor.toLowerCase());
    }

    private void sendProgress() {
        if (totalTicks <= 0) {
            return;
        }
        double pct = Math.min(1.0, Math.max(0.0, elapsedTicks / (double) totalTicks));
        int percent = (int) Math.round(pct * 100);
        TextComponent component = new TextComponent("Cutscene " + cutscene.getId() + " " + percent + "%");
        for (CutsceneContext context : contexts) {
            Player viewer = context.getViewer();
            if (viewer.isOnline()) {
                viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
            }
        }
    }

    public void playEffectsToAll(EffectSettings effects, Location origin) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        for (CutsceneContext context : contexts) {
            Player viewer = context.getViewer();
            CutsceneEffects.play(viewer, effects, origin, plugin);
        }
    }

    public EffectSettings resolveEffectBundle(String name) {
        EffectSettings settings = cutscene.getEffectBundle(name);
        return settings == null ? EffectSettings.empty() : settings;
    }

    public void skip(Player viewer) {
        if (viewer == null) {
            return;
        }
        UUID id = viewer.getUniqueId();
        contexts.removeIf(ctx -> ctx.getViewer().equals(viewer));
        viewers.remove(viewer);
        cancelMovement(id);
        restore(viewer);
        if (endLocation != null) {
            viewer.teleport(endLocation);
        }
        if (contexts.isEmpty()) {
            stop();
        }
    }

    private void cancelMovement(UUID id) {
        List<BukkitTask> tasks = movementTasks.remove(id);
        if (tasks != null) {
            for (BukkitTask task : tasks) {
                task.cancel();
            }
        }
    }

    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        for (BukkitTask task : scheduled) {
            task.cancel();
        }
        scheduled.clear();
        for (UUID id : movementTasks.keySet()) {
            cancelMovement(id);
        }
        movementTasks.clear();
        for (Player viewer : new ArrayList<>(states.keySet()).stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .collect(Collectors.toList())) {
            restore(viewer);
        }
        for (CutsceneActor actor : actors.values()) {
            if (!actor.isPersistent()) {
                actor.despawn();
            }
        }
        actors.clear();
        Bukkit.getPluginManager().callEvent(new CutsceneEndEvent(cutscene, getViewers()));
        if (onFinish != null) {
            onFinish.run();
        }
        contexts.clear();
        states.clear();
    }

    private void finish() {
        stop();
    }

    private void restore(Player viewer) {
        if (viewer == null) {
            return;
        }
        PlayerState state = states.remove(viewer.getUniqueId());
        if (state != null) {
            viewer.setGameMode(state.mode);
            viewer.setAllowFlight(state.allowFlight);
            viewer.setFlying(state.flying);
        }
        var sb = plugin.getScoreboardManager();
        if (sb != null) sb.createBoard(viewer);
    }

    private static final class PlayerState {
        final GameMode mode;
        final boolean allowFlight;
        final boolean flying;

        PlayerState(Player player) {
            this.mode = player.getGameMode();
            this.allowFlight = player.getAllowFlight();
            this.flying = player.isFlying();
        }
    }
}
