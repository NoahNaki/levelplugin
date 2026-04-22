package me.nakilex.levelplugin.stronghold;

public final class StrongholdTemplateData {
    public static final String SOURCE_WORLD = "flatland";
    public static final String GENERATED_WORLD_PREFIX = "stronghold_debug_";

    private StrongholdTemplateData() {
    }

    public static String sourceWorldName() {
        return SOURCE_WORLD;
    }

    public static String generatedWorldPrefix() {
        return GENERATED_WORLD_PREFIX;
    }
}
