package me.nakilex.levelplugin.cutscene;

import me.nakilex.levelplugin.cutscene.frames.Frame;

import java.util.List;

public class Cutscene {
    private final String id;
    private final List<Frame> frames;

    public Cutscene(String id, List<Frame> frames) {
        this.id = id;
        this.frames = frames;
    }

    public String getId() {
        return id;
    }

    public List<Frame> getFrames() {
        return frames;
    }
}
