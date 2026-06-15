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
    private int clicks;
    private int requiredClicks;
    private int barSize;
    private int targetScore;
    private int score;
    private int health;
    private int hookIndex;
    private int targetIndex;
    private boolean movingRight = true;
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
    public int clicks() { return clicks; }
    public int requiredClicks() { return requiredClicks; }
    public int barSize() { return barSize; }
    public int targetScore() { return targetScore; }
    public int score() { return score; }
    public int health() { return health; }
    public int hookIndex() { return hookIndex; }
    public int targetIndex() { return targetIndex; }
    public boolean movingRight() { return movingRight; }
    public boolean finished() { return finished; }

    public void setTiming(long targetTick, long hitWindowTicks) {
        this.targetTick = Math.max(0L, targetTick);
        this.hitWindowTicks = Math.max(1L, hitWindowTicks);
    }

    public void setElapsedTicks(long elapsedTicks) {
        this.elapsedTicks = Math.max(0L, elapsedTicks);
    }

    public void setRequiredClicks(int requiredClicks) {
        this.requiredClicks = Math.max(1, requiredClicks);
    }

    public int incrementClicks() {
        clicks++;
        return clicks;
    }

    public void configureHitVisuals(int barSize, int targetScore, int health, int targetIndex) {
        this.barSize = Math.max(1, barSize);
        this.targetScore = Math.max(1, targetScore);
        this.health = Math.max(1, health);
        this.score = 0;
        this.hookIndex = 0;
        this.targetIndex = clamp(targetIndex, 0, this.barSize - 1);
        this.movingRight = true;
    }

    public void configureMixVisuals(int barSize, int requiredClicks) {
        this.barSize = Math.max(1, barSize);
        setRequiredClicks(requiredClicks);
    }

    public void setTargetIndex(int targetIndex) {
        this.targetIndex = clamp(targetIndex, 0, Math.max(0, barSize - 1));
    }

    public int incrementScore() {
        score++;
        return score;
    }

    public int decrementHealth() {
        health = Math.max(0, health - 1);
        return health;
    }

    public void stepHook() {
        if (barSize <= 1) {
            hookIndex = 0;
            return;
        }
        hookIndex += movingRight ? 1 : -1;
        if (hookIndex >= barSize - 1) {
            hookIndex = barSize - 1;
            movingRight = false;
        } else if (hookIndex <= 0) {
            hookIndex = 0;
            movingRight = true;
        }
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
