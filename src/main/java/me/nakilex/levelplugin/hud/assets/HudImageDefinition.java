package me.nakilex.levelplugin.hud.assets;

import java.util.Collections;
import java.util.List;

public class HudImageDefinition {
    private final String id;
    private final HudImageType type;
    private final String texture;
    private final int split;
    private final HudSplitType splitType;
    private final String current;
    private final String max;
    private final List<String> frames;

    public HudImageDefinition(String id,
                              HudImageType type,
                              String texture,
                              int split,
                              HudSplitType splitType,
                              String current,
                              String max,
                              List<String> frames) {
        this.id = id;
        this.type = type == null ? HudImageType.SINGLE : type;
        this.texture = texture;
        this.split = split;
        this.splitType = splitType == null ? HudSplitType.UP : splitType;
        this.current = current;
        this.max = max;
        this.frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public String getId() {
        return id;
    }

    public HudImageType getType() {
        return type;
    }

    public String getTexture() {
        return texture;
    }

    public int getSplit() {
        return split;
    }

    public HudSplitType getSplitType() {
        return splitType;
    }

    public String getCurrent() {
        return current;
    }

    public String getMax() {
        return max;
    }

    public List<String> getFrames() {
        return Collections.unmodifiableList(frames);
    }
}
