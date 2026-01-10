package me.nakilex.levelplugin.hud.core;

import java.util.Collections;
import java.util.List;

public class HudModule {
    private final String id;
    private final List<HudLayoutPlacement> placements;

    public HudModule(String id, List<HudLayoutPlacement> placements) {
        this.id = id;
        this.placements = placements == null ? List.of() : List.copyOf(placements);
    }

    public String getId() {
        return id;
    }

    public List<HudLayoutPlacement> getPlacements() {
        return Collections.unmodifiableList(placements);
    }
}
