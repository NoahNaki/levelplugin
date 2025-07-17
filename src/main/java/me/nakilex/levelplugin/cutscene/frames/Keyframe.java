package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Keyframe implements Frame {
    private final Location location;
    private final long durationMs;

    public Keyframe(Location location, long durationMs) {
        this.location = location;
        this.durationMs = durationMs;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    @Override
    public void play(Player player, Main plugin) {
        // For now just teleport instantly (no interpolation)
        if (location != null) {
            player.teleport(location);
        }
    }
}
