package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;

public interface Frame {
    long getDuration();

    /**
     * Play this frame for the given player.
     *
     * @return a BukkitTask representing the movement task, or null if none
     */
    org.bukkit.scheduler.BukkitTask play(Player player, Main plugin);
}
