package me.nakilex.npc.core.event;

import me.nakilex.npc.core.model.Npc;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class NpcInteractEvent extends NpcEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public NpcInteractEvent(Npc npc, Player player) {
        super(npc);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
