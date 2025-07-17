package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;

public interface Frame {
    long getDuration();
    void play(Player player, Main plugin);
}
