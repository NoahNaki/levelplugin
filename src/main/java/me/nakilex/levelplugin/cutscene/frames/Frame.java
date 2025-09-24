package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;
import org.bukkit.Location;

public interface Frame {
    long getDuration();

    /**
     * Play this frame for the given context.
     *
     * @return a BukkitTask representing the movement task, or null if none
     */
    org.bukkit.scheduler.BukkitTask play(CutsceneContext context);

    /**
     * @return the location a viewer should end up after this frame. Used to
     * determine cutscene end positions for skipping.
     */
    default Location getTargetLocation() {
        return null;
    }
}
