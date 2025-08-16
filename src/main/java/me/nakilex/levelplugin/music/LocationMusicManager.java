package me.nakilex.levelplugin.music;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles playing and stopping location-based music for players.
 */
public class LocationMusicManager {
    private final Map<String, String> locationSongs = new HashMap<>();
    private final Map<UUID, String> playing = new HashMap<>();
    private final Set<UUID> siegePlayers = new HashSet<>();
    private static final long SIEGE_INTRO_DELAY = 200L;

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
        UUID id = player.getUniqueId();
        if (siegePlayers.contains(id)) return; // siege overrides location music
        if (point == null) return; // no new location, keep current song

        var settingsMgr = Main.getInstance().getSettingsManager();
        if (settingsMgr != null && settingsMgr.getSettings(player).isAutoSkipSongs()) {
            debug(player, "auto-skip on; ignoring location '" + point.getName() + "'");
            skip(player);
            return;
        }

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
        UUID id = player.getUniqueId();
        siegePlayers.remove(id);
        String current = playing.remove(id);
        player.stopSound(SoundCategory.MUSIC);
        if (current != null) {
            debug(player, "stopped sound '" + current + "'");
        }
    }

    /** Start siege music for the player, overriding location tracks. */
    public void startSiege(Player player) {
        UUID id = player.getUniqueId();
        skip(player); // stop any current music and clear flags
        siegePlayers.add(id);
        player.playSound(player.getLocation(), "nexo:music.warscream", SoundCategory.MUSIC, 1f, 1f);
        playing.put(id, "nexo:music.warscream");
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (siegePlayers.contains(id)) {
                player.playSound(player.getLocation(), "nexo:music.siege", SoundCategory.MUSIC, 1f, 1f);
                playing.put(id, "nexo:music.siege");
            }
        }, SIEGE_INTRO_DELAY);
    }

    /** Stop siege music and resume location-based tracks. */
    public void stopSiege(Player player) {
        siegePlayers.remove(player.getUniqueId());
        skip(player);
        FastTravelPoint pt = Main.getInstance().getFastTravelManager().getPointAt(player.getLocation());
        update(player, pt);
    }

    private void debug(Player player, String msg) {
        Main.getInstance().getLogger().info("[LocationMusic] " + player.getName() + ": " + msg);
    }
}
