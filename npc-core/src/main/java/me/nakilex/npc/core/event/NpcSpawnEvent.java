package me.nakilex.npc.core.event;

import me.nakilex.npc.core.model.Npc;
import org.bukkit.event.HandlerList;

public class NpcSpawnEvent extends NpcEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public NpcSpawnEvent(Npc npc) {
        super(npc);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
