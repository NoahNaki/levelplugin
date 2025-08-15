package me.nakilex.levelplugin.music;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles playing and stopping location-based music for players.
 */
public class LocationMusicManager {
    private static class ActiveSong {
        final String sound;
        final Location center;
        final double radius;
        ActiveSong(String sound, Location center, double radius) {
            this.sound = sound;
            this.center = center;
            this.radius = radius;
        }
    }

    private final Map<String, String> locationSongs = new HashMap<>();
    private final Map<UUID, ActiveSong> playing = new HashMap<>();

    public LocationMusicManager() {
        registerLocationSong("rowan", "nexo:music.greennature");
    }

    /** Register a song to play when entering the given location name. */
    public void registerLocationSong(String location, String sound) {
        locationSongs.put(location.toLowerCase(), sound);
    }

    /** Update player music based on their current location and distance from the origin. */
    public void update(Player player, FastTravelPoint point, Location loc) {
        UUID id = player.getUniqueId();
        if (point != null) {
            String key = point.getName().toLowerCase();
            String sound = locationSongs.get(key);
            debug(player, "enter '" + point.getName() + "' key='" + key + "' sound='" + sound + "'");
            if (sound == null) return;
            PlayerSettings settings = Main.getInstance().getSettingsManager().getSettings(player);
            if (settings.isAutoSkipSongs()) {
                debug(player, "auto-skip enabled; not playing");
                return;
            }
            ActiveSong current = playing.get(id);
            if (current != null && current.sound.equals(sound)) {
                return; // already playing this song
            }
            if (current != null) {
                player.stopSound(current.sound, SoundCategory.MUSIC);
                debug(player, "stopped previous sound '" + current.sound + "'");
            }
            player.playSound(point.getLocation(), sound, SoundCategory.MUSIC, 1f, 1f);
            playing.put(id, new ActiveSong(sound, point.getLocation(), point.getRadius()));
            debug(player, "playing sound '" + sound + "'");
        } else {
            ActiveSong current = playing.get(id);
            if (current == null) return;
            double distance = loc.distance(current.center);
            if (distance > current.radius + 20) {
                player.stopSound(current.sound, SoundCategory.MUSIC);
                playing.remove(id);
                debug(player, "stopped sound '" + current.sound + "' after moving " + distance + " blocks away");
            }
        }
    }

    /** Stop the current song for the player. */
    public void skipSong(Player player) {
        ActiveSong current = playing.remove(player.getUniqueId());
        if (current != null) {
            player.stopSound(current.sound, SoundCategory.MUSIC);
            debug(player, "stopped sound '" + current.sound + "'");
        } else {
            debug(player, "no sound to stop");
        }
    }

    private void debug(Player player, String msg) {
        Main.getInstance().getLogger().info("[LocationMusic] " + player.getName() + ": " + msg);
    }
}
