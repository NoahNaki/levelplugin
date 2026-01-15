package me.nakilex.levelplugin.npc.system;

import org.bukkit.entity.Entity;

public final class NpcTagUtil {
    public static final String NPC_TAG = "NPC";

    private NpcTagUtil() {
    }

    public static boolean isNpc(Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(NPC_TAG);
    }

    public static void tagNpc(Entity entity) {
        if (entity != null) {
            entity.addScoreboardTag(NPC_TAG);
        }
    }
}
