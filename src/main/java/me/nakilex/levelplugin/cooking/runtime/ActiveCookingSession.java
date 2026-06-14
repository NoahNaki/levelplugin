package me.nakilex.levelplugin.cooking.runtime;

import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.util.CookingLocationKey;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.UUID;

/** Runtime-only lock, stage progress, timer, and display references for a selected cooking recipe. */
public class ActiveCookingSession {
    private final UUID playerId;
    private final CookingLocationKey workstationKey;
    private final String recipeId;
    private final CookingStageProgress progress;
    private final Instant startedAt;
    private CookingStageType activeStageType;
    private BukkitTask waitTask;
    private ItemDisplay itemDisplay;
    private TextDisplay textDisplay;

    public ActiveCookingSession(UUID playerId, CookingLocationKey workstationKey, String recipeId) {
        this.playerId = playerId;
        this.workstationKey = workstationKey;
        this.recipeId = recipeId;
        this.progress = new CookingStageProgress();
        this.startedAt = Instant.now();
    }

    public UUID playerId() { return playerId; }
    public CookingLocationKey workstationKey() { return workstationKey; }
    public String recipeId() { return recipeId; }
    public CookingStageProgress progress() { return progress; }
    public Instant startedAt() { return startedAt; }
    public CookingStageType activeStageType() { return activeStageType; }
    public BukkitTask waitTask() { return waitTask; }
    public ItemDisplay itemDisplay() { return itemDisplay; }
    public TextDisplay textDisplay() { return textDisplay; }

    public void setActiveStageType(CookingStageType activeStageType) {
        this.activeStageType = activeStageType;
    }

    public void clearActiveStageType() {
        this.activeStageType = null;
    }

    public void setWaitTask(BukkitTask waitTask) {
        cancelWaitTask();
        this.waitTask = waitTask;
    }

    public void cancelWaitTask() {
        if (waitTask != null) {
            waitTask.cancel();
            waitTask = null;
        }
    }

    public void attachDisplayEntities(ItemDisplay itemDisplay, TextDisplay textDisplay) {
        removeDisplayEntities();
        this.itemDisplay = itemDisplay;
        this.textDisplay = textDisplay;
    }

    public void updateTextDisplay(String text) {
        if (textDisplay != null && textDisplay.isValid()) {
            textDisplay.setText(text);
        }
    }

    public void removeDisplayEntities() {
        if (itemDisplay != null && itemDisplay.isValid()) {
            itemDisplay.remove();
        }
        if (textDisplay != null && textDisplay.isValid()) {
            textDisplay.remove();
        }
        itemDisplay = null;
        textDisplay = null;
    }
}
