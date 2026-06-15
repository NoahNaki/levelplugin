package me.nakilex.levelplugin.cooking.minigame;

import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import org.bukkit.scheduler.BukkitTask;

/** Runtime state for a single active cooking mini-game stage. */
public class CookingMiniGameSession {
    private final ActiveCookingSession cookingSession;
    private final CookingStage stage;
    private final CookingMiniGameType type;
    private long elapsedTicks;
    private long targetTick;
    private long hitWindowTicks;
    private BukkitTask task;
    private boolean finished;

    public CookingMiniGameSession(ActiveCookingSession cookingSession, CookingStage stage, CookingMiniGameType type) {
        this.cookingSession = cookingSession;
        this.stage = stage;
        this.type = type;
    }

    public ActiveCookingSession cookingSession() { return cookingSession; }
    public CookingStage stage() { return stage; }
    public CookingMiniGameType type() { return type; }
    public long elapsedTicks() { return elapsedTicks; }
    public long targetTick() { return targetTick; }
    public long hitWindowTicks() { return hitWindowTicks; }
    public boolean finished() { return finished; }

    public void setTiming(long targetTick, long hitWindowTicks) {
        this.targetTick = Math.max(0L, targetTick);
        this.hitWindowTicks = Math.max(1L, hitWindowTicks);
    }

    public void setElapsedTicks(long elapsedTicks) {
        this.elapsedTicks = Math.max(0L, elapsedTicks);
    }

    public void setTask(BukkitTask task) {
        cancelTask();
        this.task = task;
    }

    public void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void finish() {
        finished = true;
        cancelTask();
    }
}
