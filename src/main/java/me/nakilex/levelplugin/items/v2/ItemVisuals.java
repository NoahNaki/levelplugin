package me.nakilex.levelplugin.items.v2;

import org.bukkit.Material;

public record ItemVisuals(Material baseMaterial, String modelKey) {
    public ItemVisuals {
        if (baseMaterial == null) {
            baseMaterial = Material.DIAMOND;
        }
        if (modelKey == null || modelKey.isBlank()) {
            modelKey = "unassigned";
        }
    }
}
