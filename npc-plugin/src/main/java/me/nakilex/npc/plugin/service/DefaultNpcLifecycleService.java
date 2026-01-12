package me.nakilex.npc.plugin.service;

import me.nakilex.npc.core.event.NpcDespawnEvent;
import me.nakilex.npc.core.event.NpcSpawnEvent;
import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.nms.NmsBridge;
import me.nakilex.npc.core.registry.DefaultNpcRegistry;
import me.nakilex.npc.core.service.NpcLifecycleService;
import me.nakilex.npc.core.trait.Trait;
import me.nakilex.npc.core.trait.TraitRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class DefaultNpcLifecycleService implements NpcLifecycleService {
    private final Plugin plugin;
    private final DefaultNpcRegistry registry;
    private final NmsBridge bridge;
    private final TraitRegistry traitRegistry;
    private final Set<Integer> spawned = new HashSet<>();

    public DefaultNpcLifecycleService(Plugin plugin, DefaultNpcRegistry registry, NmsBridge bridge, TraitRegistry traitRegistry) {
        this.plugin = plugin;
        this.registry = registry;
        this.bridge = bridge;
        this.traitRegistry = traitRegistry;
    }

    @Override
    public void spawn(Npc npc) {
        if (npc == null || spawned.contains(npc.getId())) {
            return;
        }
        if (npc.getPosition() == null) {
            return;
        }
        Location location = npc.getPosition().toLocation();
        if (location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }
        if (npc.getType() == EntityType.PLAYER) {
            // Placeholder until NMS-backed player NPCs are implemented.
            return;
        }
        bridge.spawnNpc(npc);
        spawned.add(npc.getId());
        traitRegistry.list().forEach(trait -> trait.onSpawn(npc));
        Bukkit.getPluginManager().callEvent(new NpcSpawnEvent(npc));
    }

    @Override
    public void despawn(Npc npc) {
        if (npc == null || !spawned.contains(npc.getId())) {
            return;
        }
        traitRegistry.list().forEach(trait -> trait.onDespawn(npc));
        bridge.despawnNpc(npc);
        spawned.remove(npc.getId());
        Bukkit.getPluginManager().callEvent(new NpcDespawnEvent(npc));
    }

    @Override
    public void respawn(Npc npc) {
        despawn(npc);
        spawn(npc);
    }

    @Override
    public boolean isSpawned(Npc npc) {
        return npc != null && spawned.contains(npc.getId());
    }

    public void spawnAll() {
        registry.list().forEach(this::spawn);
    }

    public void despawnAll() {
        registry.list().forEach(this::despawn);
        spawned.clear();
    }

    public void tickTraits() {
        for (Npc npc : registry.list()) {
            for (Trait trait : traitRegistry.list()) {
                if (trait.requiresTicking()) {
                    trait.onTick(npc);
                }
            }
        }
    }
}
