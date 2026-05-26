package me.nakilex.levelplugin.advancement.events;

import me.nakilex.levelplugin.advancement.model.AdvancementKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class AdvancementProgressionUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID teamId;
    private final AdvancementKey key;
    private final int oldProgress;
    private final int newProgress;

    public AdvancementProgressionUpdateEvent(UUID teamId, AdvancementKey key, int oldProgress, int newProgress) {
        this.teamId = teamId; this.key = key; this.oldProgress = oldProgress; this.newProgress = newProgress;
    }
    public UUID getTeamId() { return teamId; }
    public AdvancementKey getKey() { return key; }
    public int getOldProgress() { return oldProgress; }
    public int getNewProgress() { return newProgress; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
