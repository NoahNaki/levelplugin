package me.nakilex.levelplugin.dungeon;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class DungeonMobSpawnListener implements Listener {
    private final DungeonManager manager;
    private final Set<Dungeon.RoomInstance> triggered = new HashSet<>();

    public DungeonMobSpawnListener(DungeonManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ())) return;

        for (Dungeon dungeon : manager.getActiveDungeons()) {
            if (!dungeon.getRooms().isEmpty() && !player.getWorld().equals(dungeon.getRooms().get(0).center.getWorld())) continue;
            for (Dungeon.RoomInstance room : dungeon.getRooms()) {
                if (room.mob == null || triggered.contains(room)) continue;
                if (room.contains(to)) {
                    for (int i = 0; i < 5; i++) {
                        MythicBukkit.inst().getMobManager().spawnMob(room.mob, room.center, 1.0);
                    }
                    triggered.add(room);
                }
            }
        }
    }
}
