package me.nakilex.levelplugin.hud.core;

import java.util.Collections;
import java.util.List;

public class HudLayout {
    private final String id;
    private final List<HudElement> elements;

    public HudLayout(String id, List<HudElement> elements) {
        this.id = id;
        this.elements = elements == null ? List.of() : List.copyOf(elements);
    }

    public String getId() {
        return id;
    }

    public List<HudElement> getElements() {
        return Collections.unmodifiableList(elements);
    }
}
