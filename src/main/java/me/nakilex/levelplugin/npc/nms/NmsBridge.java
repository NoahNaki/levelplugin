package me.nakilex.levelplugin.npc.nms;

import me.nakilex.levelplugin.npc.core.PlayerNpc;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface NmsBridge {
    Object createNpcHandle(UUID uuid, String name, Location location);

    void spawnNpcForViewer(PlayerNpc npc, Player viewer, boolean removeFromTabLater);

    void despawnNpcForViewer(PlayerNpc npc, Player viewer);
}
