package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;

public class WaitFrame implements Frame {
    private final long durationMs;
    private final String actorToAwait;

    public WaitFrame(long durationMs, String actorToAwait) {
        this.durationMs = durationMs;
        this.actorToAwait = actorToAwait;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    @Override
    public org.bukkit.scheduler.BukkitTask play(CutsceneContext context) {
        if (actorToAwait != null && context.isPrimary()) {
            context.awaitActor(actorToAwait);
        }
        return null;
    }

    public String getActorToAwait() {
        return actorToAwait;
    }
}
