package me.nakilex.levelplugin.cutscene.events;

import me.nakilex.levelplugin.cutscene.Cutscene;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.Collection;

public class CutsceneEndEvent extends CutsceneEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public CutsceneEndEvent(Cutscene cutscene, Collection<Player> viewers) {
        super(cutscene, viewers);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
