package me.nakilex.levelplugin.npc.system;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.nms.NmsImpl;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class NpcSkinService {
    private static final long TAB_LIST_REMOVE_DELAY_TICKS = 20L;

    private NpcSkinService() {
    }

    public static void applySkinToViewers(NPC npc) {
        if (npc == null || npc.getEntity() == null || npc.getType() != org.bukkit.entity.EntityType.PLAYER) {
            return;
        }
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player npcPlayer)) {
            return;
        }
        World world = npcPlayer.getWorld();
        for (Player viewer : world.getPlayers()) {
            applySkinToViewer(viewer, npc, TAB_LIST_REMOVE_DELAY_TICKS);
        }
    }

    public static void applySkinToViewer(Player viewer, NPC npc, long removeDelayTicks) {
        if (viewer == null || npc == null || npc.getEntity() == null) {
            return;
        }
        if (!(npc.getEntity() instanceof Player npcPlayer)) {
            return;
        }
        SkinTrait skin = npc.getTrait(SkinTrait.class);
        if (skin == null || skin.getTexture() == null || skin.getSignature() == null) {
            return;
        }
        NmsImpl.applySkin(npcPlayer, skin);
        NmsImpl.sendTabListAdd(viewer, npcPlayer);
        if (removeDelayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                    () -> NmsImpl.sendTabListRemove(viewer, npcPlayer),
                    removeDelayTicks);
        }
    }
}
