package me.nakilex.levelplugin.advancement.model;

import java.util.UUID;

public class RootAdvancement extends Advancement {
    private final String backgroundTexture;
    public RootAdvancement(AdvancementKey key, AdvancementDisplay display, int maxProgress, String backgroundTexture) {
        super(key, display, maxProgress);
        this.backgroundTexture = backgroundTexture;
    }
    public String backgroundTexture() { return backgroundTexture; }
    @Override public boolean isVisible(UUID teamId) { return true; }
}
