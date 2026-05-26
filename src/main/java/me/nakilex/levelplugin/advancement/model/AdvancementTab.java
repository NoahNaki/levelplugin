package me.nakilex.levelplugin.advancement.model;

import java.util.*;

public class AdvancementTab {
    private final String namespace;
    private final Map<AdvancementKey, Advancement> advancements = new HashMap<>();
    private RootAdvancement root;
    private boolean initialised;

    public AdvancementTab(String namespace) { this.namespace = namespace; }
    public String namespace() { return namespace; }
    public boolean isActive() { return initialised; }
    public RootAdvancement root() { return root; }
    public Collection<Advancement> advancements() { return Collections.unmodifiableCollection(advancements.values()); }

    public void registerAdvancements(RootAdvancement rootAdv, Set<? extends BaseAdvancement> children) {
        if (initialised) throw new IllegalStateException("Tab already initialised");
        this.root = rootAdv;
        this.advancements.put(rootAdv.key(), rootAdv);
        for (BaseAdvancement child : children) {
            if (!namespace.equals(child.key().namespace())) throw new IllegalArgumentException("Child namespace mismatch");
            advancements.put(child.key(), child);
        }
        initialised = true;
    }

    public Optional<Advancement> byKey(AdvancementKey key) { return Optional.ofNullable(advancements.get(key)); }
}
