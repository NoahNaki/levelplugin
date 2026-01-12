package me.nakilex.npc.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NpcHologramConfig {
    private final List<String> lines = new ArrayList<>();
    private double offset = 0.4;
    private double spacing = 0.25;
    private double viewRange = 32.0;

    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public void setLines(List<String> updatedLines) {
        lines.clear();
        if (updatedLines != null) {
            lines.addAll(updatedLines);
        }
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public double getSpacing() {
        return spacing;
    }

    public void setSpacing(double spacing) {
        this.spacing = spacing;
    }

    public double getViewRange() {
        return viewRange;
    }

    public void setViewRange(double viewRange) {
        this.viewRange = viewRange;
    }
}
