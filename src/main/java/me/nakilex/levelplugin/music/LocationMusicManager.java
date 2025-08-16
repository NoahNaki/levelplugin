package me.nakilex.levelplugin.music;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles playing and stopping location-based music for players.
 */
public class LocationMusicManager {
    private final Map<String, String> locationSongs = new HashMap<>();
    private final Map<UUID, String> playing = new HashMap<>();

    public LocationMusicManager() {
        registerLocationSong("rowan", "nexo:music.greennature");
    }

    /** Register a song to play when entering the given location name. */
    public void registerLocationSong(String location, String sound) {
        locationSongs.put(location.toLowerCase(), sound);
    }

    /**
     * Trigger music based on the player's current location. Once a song starts
     * it keeps playing until another song is triggered or manually skipped.
     */
    public void update(Player player, FastTravelPoint point) {
        if (point == null) return; // no new location, keep current song

        UUID id = player.getUniqueId();
        String key = point.getName().toLowerCase();
        String sound = locationSongs.get(key);
        debug(player, "enter '" + point.getName() + "' key='" + key + "' sound='" + sound + "'");
        if (sound == null) return;
        String current = playing.get(id);
        if (current != null && current.equals(sound)) {
            return; // already playing this song
        }
        if (current != null) {
            player.stopSound(current, SoundCategory.MUSIC);
            debug(player, "stopped previous sound '" + current + "'");
        }
        player.playSound(point.getLocation(), sound, SoundCategory.MUSIC, 1f, 1f);
        playing.put(id, sound);
        debug(player, "playing sound '" + sound + "'");
    }

    private void debug(Player player, String msg) {
        Main.getInstance().getLogger().info("[LocationMusic] " + player.getName() + ": " + msg);
    }
}
