package me.nakilex.levelplugin.advancement.model;

import me.nakilex.levelplugin.advancement.AdvancementService;

import java.util.UUID;

public abstract class Advancement {
    private final AdvancementKey key;
    private final AdvancementDisplay display;
    private final int maxProgress;

    protected Advancement(AdvancementKey key, AdvancementDisplay display, int maxProgress) {
        this.key = key;
        this.display = display;
        this.maxProgress = Math.max(1, maxProgress);
    }

    public AdvancementKey key() { return key; }
    public AdvancementDisplay display() { return display; }
    public int maxProgress() { return maxProgress; }
    public boolean isVisible(UUID teamId) { return true; }
    public int getProgression(AdvancementService service, UUID teamId) { return service.getProgression(teamId, key); }
}
