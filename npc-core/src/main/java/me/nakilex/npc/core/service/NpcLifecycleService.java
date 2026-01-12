package me.nakilex.npc.core.service;

import me.nakilex.npc.core.model.Npc;

public interface NpcLifecycleService {
    void spawn(Npc npc);

    void despawn(Npc npc);

    void respawn(Npc npc);

    boolean isSpawned(Npc npc);
}
