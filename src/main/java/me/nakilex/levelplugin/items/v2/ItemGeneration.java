package me.nakilex.levelplugin.items.v2;

public record ItemGeneration(ItemGenerationMode mode, String profileKey) {
    public ItemGeneration {
        if (mode == null) {
            mode = ItemGenerationMode.HANDMADE;
        }
    }
}
