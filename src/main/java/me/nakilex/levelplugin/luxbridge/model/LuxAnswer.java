package me.nakilex.levelplugin.luxbridge.model;

import java.util.List;

public record LuxAnswer(
        String id,
        String text,
        String gotoPage,
        List<String> replies,
        String condition,
        LuxSoundSpec sound,
        List<String> actions
) {
}
