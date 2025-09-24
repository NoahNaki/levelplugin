package me.nakilex.levelplugin.cutscene.events;

import me.nakilex.levelplugin.cutscene.Cutscene;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Collection;
import java.util.List;

public abstract class CutsceneEvent extends Event {
    private final Cutscene cutscene;
    private final List<Player> viewers;

    protected CutsceneEvent(Cutscene cutscene, Collection<Player> viewers) {
        this.cutscene = cutscene;
        this.viewers = viewers == null ? List.of() : List.copyOf(viewers);
    }

    public Cutscene getCutscene() {
        return cutscene;
    }

    public List<Player> getViewers() {
        return viewers;
    }
}
