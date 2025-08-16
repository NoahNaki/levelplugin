package me.nakilex.levelplugin.music;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.Bukkit;
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

        // Periodically stop any vanilla music for players without a custom track.
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!playing.containsKey(p.getUniqueId())) {
                    p.stopSound(SoundCategory.MUSIC);
                }
            }
        }, 0L, 200L);
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

        var settingsMgr = Main.getInstance().getSettingsManager();
        if (settingsMgr != null && settingsMgr.getSettings(player).isAutoSkipSongs()) {
            debug(player, "auto-skip on; ignoring location '" + point.getName() + "'");
            skip(player);
            return;
        }

        UUID id = player.getUniqueId();
        String key = point.getName().toLowerCase();
        String sound = locationSongs.get(key);
        if (sound == null) return;
        String current = playing.get(id);
        if (current != null && current.equals(sound)) {
            return; // already playing this song
        }
        debug(player, "enter '" + point.getName() + "' key='" + key + "' sound='" + sound + "'");
        player.stopSound(SoundCategory.MUSIC); // stop vanilla and previous music
        playing.put(id, sound);
        player.playSound(point.getLocation(), sound, SoundCategory.MUSIC, 1f, 1f);
        debug(player, "playing sound '" + sound + "'");
    }

    /** Stop any currently playing location song for the player. */
    public void skip(Player player) {
        String current = playing.remove(player.getUniqueId());
        player.stopSound(SoundCategory.MUSIC);
        if (current != null) {
            debug(player, "stopped sound '" + current + "'");
        }
    }

    private void debug(Player player, String msg) {
        Main.getInstance().getLogger().info("[LocationMusic] " + player.getName() + ": " + msg);
    }
}
