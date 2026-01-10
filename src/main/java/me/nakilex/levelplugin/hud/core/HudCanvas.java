package me.nakilex.levelplugin.hud.core;

import java.util.Collections;
import java.util.List;

public class HudCanvas {
    private final List<HudResolvedElement> elements;

    public HudCanvas(List<HudResolvedElement> elements) {
        this.elements = elements == null ? List.of() : List.copyOf(elements);
    }

    public List<HudResolvedElement> getElements() {
        return Collections.unmodifiableList(elements);
    }
}
