package me.nakilex.levelplugin.advancement.model;

public class BaseAdvancement extends Advancement {
    private final Advancement parent;
    public BaseAdvancement(AdvancementKey key, AdvancementDisplay display, int maxProgress, Advancement parent) {
        super(key, display, maxProgress);
        this.parent = parent;
    }
    public Advancement parent() { return parent; }
}
