package me.nakilex.levelplugin.advancement.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AdvancementDisplay(Material icon, String title, List<String> description, FrameType frameType,
                                 float x, float y, boolean showToast, boolean announceChat) {
    public enum FrameType { TASK, GOAL, CHALLENGE }

    public AdvancementDisplay {
        description = description == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(description));
        if (icon == null) icon = Material.PAPER;
        if (frameType == null) frameType = FrameType.TASK;
    }

    public static final class Builder {
        private final Material icon;
        private String title = "";
        private final List<String> description = new ArrayList<>();
        private FrameType frameType = FrameType.TASK;
        private float x; private float y;
        private boolean showToast = true;
        private boolean announceChat = false;
        public Builder(Material icon) { this.icon = icon; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder descriptionLine(String line) { if (line != null) this.description.add(line); return this; }
        public Builder frameType(FrameType frameType) { this.frameType = frameType; return this; }
        public Builder coordinates(float x, float y) { this.x = x; this.y = y; return this; }
        public Builder showToast(boolean showToast) { this.showToast = showToast; return this; }
        public Builder announceChat(boolean announceChat) { this.announceChat = announceChat; return this; }
        public AdvancementDisplay build() { return new AdvancementDisplay(icon, title, description, frameType, x, y, showToast, announceChat); }
    }
}
