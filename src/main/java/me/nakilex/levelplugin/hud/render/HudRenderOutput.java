package me.nakilex.levelplugin.hud.render;

import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;

public class HudRenderOutput {
    private final List<String> bossBarLineTexts;
    private final List<Component> bossBarLineComponents;

    public HudRenderOutput(List<String> bossBarLineTexts, List<Component> bossBarLineComponents) {
        this.bossBarLineTexts = bossBarLineTexts == null ? List.of() : List.copyOf(bossBarLineTexts);
        this.bossBarLineComponents = bossBarLineComponents == null ? List.of() : List.copyOf(bossBarLineComponents);
    }

    public List<String> getBossBarLineTexts() {
        return Collections.unmodifiableList(bossBarLineTexts);
    }

    public List<Component> getBossBarLineComponents() {
        return Collections.unmodifiableList(bossBarLineComponents);
    }
}
