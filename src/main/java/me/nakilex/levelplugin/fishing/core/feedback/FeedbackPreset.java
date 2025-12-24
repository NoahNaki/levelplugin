package me.nakilex.levelplugin.fishing.core.feedback;

import java.util.Collections;
import java.util.List;

public record FeedbackPreset(String sound,
                             float volume,
                             float pitch,
                             String particle,
                             int particleCount,
                             double particleOffset,
                             String title,
                             String subtitle,
                             String actionBar,
                             List<String> messages,
                             int titleFadeIn,
                             int titleStay,
                             int titleFadeOut) {
    public FeedbackPreset {
        messages = messages == null ? Collections.emptyList() : List.copyOf(messages);
    }
}
