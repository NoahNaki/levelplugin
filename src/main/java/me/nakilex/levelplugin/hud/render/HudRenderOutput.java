package me.nakilex.levelplugin.hud.render;

import java.util.Collections;
import java.util.List;

public class HudRenderOutput {
    private final List<String> bossBarLines;

    public HudRenderOutput(List<String> bossBarLines) {
        this.bossBarLines = bossBarLines == null ? List.of() : List.copyOf(bossBarLines);
    }

    public List<String> getBossBarLines() {
        return Collections.unmodifiableList(bossBarLines);
    }
}
