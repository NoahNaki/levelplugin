package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.cutscene.actor.CutsceneActor;
import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ActorActionFrame implements Frame {
    private final String actorName;
    private final String action;
    private final Map<String, Object> parameters;
    private final long durationMs;

    public ActorActionFrame(String actorName, String action, Map<String, Object> parameters, long durationMs) {
        this.actorName = actorName;
        this.action = action;
        this.parameters = parameters == null ? Collections.emptyMap() : parameters;
        this.durationMs = durationMs;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    public String getActorName() {
        return actorName;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public org.bukkit.scheduler.BukkitTask play(CutsceneContext context) {
        if (!context.isPrimary() || actorName == null) {
            return null;
        }
        CutsceneActor actor = context.getActor(actorName).orElse(null);
        if (actor == null) {
            return null;
        }
        CompletableFuture<Void> future = actor.performAction(action, context, parameters);
        if (future != null) {
            context.getPlayback().registerActorFuture(actorName, future);
        }
        return null;
    }
}
