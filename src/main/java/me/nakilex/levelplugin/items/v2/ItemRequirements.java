package me.nakilex.levelplugin.items.v2;

import java.util.List;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;

public record ItemRequirements(int level, List<PlayerClass> classes) {
    public ItemRequirements {
        if (level < 1) {
            level = 1;
        }
        if (classes == null) {
            classes = List.of();
        }
    }
}
