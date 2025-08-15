package me.nakilex.levelplugin.music;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
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

    /** Handle a player entering a named location. */
    public void handleEnter(Player player, String location) {
        String key = location.toLowerCase();
        String sound = locationSongs.get(key);
        debug(player, "enter '" + location + "' key='" + key + "' sound='" + sound + "'");
        if (sound == null) return;
        PlayerSettings settings = Main.getInstance().getSettingsManager().getSettings(player);
        if (settings.isAutoSkipSongs()) {
            debug(player, "auto-skip enabled; not playing");
            return;
        }
        String current = playing.get(player.getUniqueId());
        if (current != null && !current.equals(sound)) {
            player.stopSound(current, SoundCategory.MUSIC);
            debug(player, "stopped previous sound '" + current + "'");
        }
        player.playSound(player.getLocation(), sound, SoundCategory.MUSIC, 1f, 1f);
        playing.put(player.getUniqueId(), sound);
        debug(player, "playing sound '" + sound + "'");
    }

    /** Handle a player exiting a location without stopping the music. */
    public void handleExit(Player player) {
        debug(player, "exit location (song continues)");
    }

    /** Stop the current song for the player. */
    public void skipSong(Player player) {
        String sound = playing.remove(player.getUniqueId());
        if (sound != null) {
            player.stopSound(sound, SoundCategory.MUSIC);
            debug(player, "stopped sound '" + sound + "'");
        } else {
            debug(player, "no sound to stop");
        }
    }

    private void debug(Player player, String msg) {
        Main.getInstance().getLogger().info("[LocationMusic] " + player.getName() + ": " + msg);
    }
}
