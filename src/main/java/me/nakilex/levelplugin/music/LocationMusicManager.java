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
        String sound = locationSongs.get(location.toLowerCase());
        if (sound == null) return;
        PlayerSettings settings = Main.getInstance().getSettingsManager().getSettings(player);
        if (settings.isAutoSkipSongs()) return;
        player.playSound(player.getLocation(), sound, SoundCategory.MUSIC, 1f, 1f);
        playing.put(player.getUniqueId(), sound);
    }

    /** Stop any song playing for the player when they exit a location. */
    public void handleExit(Player player) {
        skipSong(player);
    }

    /** Stop the current song for the player. */
    public void skipSong(Player player) {
        String sound = playing.remove(player.getUniqueId());
        if (sound != null) {
            player.stopSound(sound, SoundCategory.MUSIC);
        }
    }
}
