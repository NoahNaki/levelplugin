package me.nakilex.levelplugin.npc.core;

import me.nakilex.levelplugin.npc.nms.NmsBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NpcManager {
    private final Plugin plugin;
    private final NpcRegistry registry;
    private final NmsBridge nmsBridge;

    public NpcManager(Plugin plugin, NpcRegistry registry, NmsBridge nmsBridge) {
        this.plugin = plugin;
        this.registry = registry;
        this.nmsBridge = nmsBridge;
    }

    public PlayerNpc create(String name, Location location) {
        if (registry.exists(name)) {
            throw new IllegalArgumentException("NPC already exists: " + name);
        }
        UUID uuid = UUID.randomUUID();
        PlayerNpc npc = new PlayerNpc(uuid, name, location.clone(), nmsBridge.createNpcHandle(uuid, name, location));
        registry.add(npc);
        return npc;
    }

    public void spawnFor(Player viewer, PlayerNpc npc) {
        if (viewer == null || npc == null) {
            return;
        }
        if (npc.hasViewer(viewer.getUniqueId())) {
            return;
        }
        nmsBridge.spawnNpcForViewer(npc, viewer, true);
        npc.addViewer(viewer.getUniqueId());
    }

    public void despawnFor(Player viewer, PlayerNpc npc) {
        if (viewer == null || npc == null) {
            return;
        }
        if (!npc.hasViewer(viewer.getUniqueId())) {
            return;
        }
        nmsBridge.despawnNpcForViewer(npc, viewer);
        npc.removeViewer(viewer.getUniqueId());
    }

    public void spawnForNearby(PlayerNpc npc, double radius) {
        if (npc == null || npc.getLocation() == null || npc.getLocation().getWorld() == null) {
            return;
        }
        double radiusSq = radius * radius;
        for (Player player : npc.getLocation().getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(npc.getLocation()) <= radiusSq) {
                spawnFor(player, npc);
            }
        }
    }

    public void shutdown() {
        Set<PlayerNpc> npcs = new HashSet<>(registry.getAll());
        for (PlayerNpc npc : npcs) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                nmsBridge.despawnNpcForViewer(npc, player);
            }
            npc.clearViewers();
            registry.remove(npc);
        }
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public NpcRegistry getRegistry() {
        return registry;
    }
}
