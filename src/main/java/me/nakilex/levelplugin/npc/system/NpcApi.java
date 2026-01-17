package me.nakilex.levelplugin.npc.system;

public final class NpcApi {
    private static NpcRegistry registry;

    private NpcApi() {
    }

    public static void initialize(NpcRegistry npcRegistry) {
        registry = npcRegistry;
    }

    public static NpcRegistry getRegistry() {
        if (registry == null) {
            throw new IllegalStateException("NpcRegistry not initialized");
        }
        return registry;
    }
}
