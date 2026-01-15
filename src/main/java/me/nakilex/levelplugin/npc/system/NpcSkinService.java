package me.nakilex.levelplugin.npc.system;

import me.nakilex.levelplugin.npc.system.NpcPacketService;
import me.nakilex.levelplugin.npc.system.NpcPlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class NpcSkinService {
    private static final long TAB_LIST_REMOVE_DELAY_TICKS = 20L;

    private NpcSkinService() {
    }

    public static void applySkinToViewers(NPC npc) {
        if (npc == null || npc.getType() != org.bukkit.entity.EntityType.PLAYER) {
            return;
        }
        NpcPlayer packetPlayer = npc.getPacketPlayer();
        if (packetPlayer == null) {
            return;
        }
        World world = packetPlayer.getLocation().getWorld();
        if (world == null) {
            return;
        }
        for (Player viewer : world.getPlayers()) {
            applySkinToViewer(viewer, npc, TAB_LIST_REMOVE_DELAY_TICKS);
        }
    }

    public static void applySkinToViewer(Player viewer, NPC npc, long removeDelayTicks) {
        if (viewer == null || npc == null) {
            return;
        }
        NpcPlayer packetPlayer = npc.getPacketPlayer();
        if (packetPlayer == null) {
            return;
        }
        NpcPacketService.showTo(viewer, packetPlayer);
    }
}
