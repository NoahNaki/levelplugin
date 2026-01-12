package me.nakilex.npc.core.event;

import me.nakilex.npc.core.model.Npc;
import org.bukkit.event.Event;

public abstract class NpcEvent extends Event {
    private final Npc npc;

    protected NpcEvent(Npc npc) {
        this.npc = npc;
    }

    public Npc getNpc() {
        return npc;
    }
}
