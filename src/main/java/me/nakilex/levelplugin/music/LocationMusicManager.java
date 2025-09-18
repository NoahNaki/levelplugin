package me.nakilex.levelplugin.music;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles playing and stopping location-based music for players. Tracks are now
 * data-driven and support lightweight fade transitions when switching between
 * areas.
 */
public class LocationMusicManager {
    private static final long VANILLA_CHECK_INTERVAL = 200L;
    private static final long FADE_IN_TICKS = 5L;
    private static final long FADE_OUT_TICKS = 10L;
    private static final long SIEGE_INTRO_DELAY = 200L;

    private final Main plugin;
    private final Map<String, String> configuredSongs = new HashMap<>();
    private final Map<String, String> runtimeOverrides = new HashMap<>();
    private final Map<String, String> locationSongs = new HashMap<>();
    private final Map<UUID, String> playing = new HashMap<>();
    private final Map<UUID, SongTransition> transitions = new HashMap<>();
    private final Set<UUID> siegePlayers = new HashSet<>();

    public LocationMusicManager(Main plugin) {
        this.plugin = plugin;
        reload();

        Bukkit.getScheduler().runTaskTimer(plugin, this::stopVanillaForIdlePlayers, 0L, VANILLA_CHECK_INTERVAL);
    }

    /** Reload mappings from regions and the optional location_music.yml file. */
    public void reload() {
        int fastTravel = loadFromFastTravel();
        int fileOverrides = loadFromFile();
        rebuildSongCache();
        plugin.getLogger().info(String.format(
                "[LocationMusic] Loaded %d tracks (%d fast-travel, %d overrides).",
                locationSongs.size(),
                fastTravel,
                fileOverrides + runtimeOverrides.size()));
    }

    /** Register a transient song override at runtime (not persisted). */
    public void registerLocationSong(String location, String sound) {
        runtimeOverrides.put(normalize(location), sound);
        rebuildSongCache();
    }

    /** Trigger music based on the player's current fast-travel point. */
    public void update(Player player, FastTravelPoint point) {
        UUID id = player.getUniqueId();
        if (siegePlayers.contains(id)) return; // siege overrides location music
        if (point == null) return; // no new location, keep current song

        var settingsMgr = plugin.getSettingsManager();
        if (settingsMgr != null && settingsMgr.getSettings(player).isAutoSkipSongs()) {
            debug(player, "auto-skip on; ignoring location '" + point.getName() + "'");
            skip(player);
            return;
        }

        String key = normalize(point.getName());
        String sound = locationSongs.get(key);
        if (sound == null || sound.isBlank()) {
            return;
        }

        String current = playing.get(id);
        if (sound.equals(current)) {
            return; // already scheduled/playing
        }

        debug(player, "enter '" + point.getName() + "' key='" + key + "' sound='" + sound + "'");
        playWithFade(player, point, sound);
    }

    /** Stop any currently playing location song for the player. */
    public void skip(Player player) {
        UUID id = player.getUniqueId();
        siegePlayers.remove(id);
        cancelTransition(id);
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
        playing.put(id, "nexo:music.warscream");
        player.playSound(player.getLocation(), "nexo:music.warscream", SoundCategory.MUSIC, 1f, 1f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
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
        FastTravelManager ft = plugin.getFastTravelManager();
        if (ft != null) {
            FastTravelPoint pt = ft.getPointAt(player.getLocation());
            update(player, pt);
        }
    }

    private int loadFromFastTravel() {
        configuredSongs.clear();
        FastTravelManager ft = plugin.getFastTravelManager();
        if (ft == null) return 0;

        int count = 0;
        for (FastTravelPoint pt : ft.getPoints()) {
            String track = pt.getMusicTrack();
            if (track == null || track.isBlank()) continue;
            configuredSongs.put(normalize(pt.getName()), track);
            count++;
        }
        return count;
    }

    private int loadFromFile() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("[LocationMusic] Failed to create data folder for music overrides.");
            return 0;
        }

        File file = new File(dataFolder, "location_music.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("location_music.yml", false);
            } catch (IllegalArgumentException ignored) {
                // Resource is optional; ignore if not bundled.
            }
        }
        if (!file.exists()) return 0;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("locations");
        if (sec == null) return 0;

        int count = 0;
        for (String key : sec.getKeys(false)) {
            String sound = sec.getString(key);
            if (sound == null || sound.isBlank()) continue;
            configuredSongs.put(normalize(key), sound);
            count++;
        }
        return count;
    }

    private void rebuildSongCache() {
        locationSongs.clear();
        locationSongs.putAll(configuredSongs);
        locationSongs.putAll(runtimeOverrides);
    }

    private void playWithFade(Player player, FastTravelPoint point, String sound) {
        UUID id = player.getUniqueId();
        cancelTransition(id);

        String current = playing.get(id);
        if (current == null) {
            player.stopSound(SoundCategory.MUSIC);
            player.playSound(point.getLocation(), sound, SoundCategory.MUSIC, 1f, 1f);
            playing.put(id, sound);
            debug(player, "playing sound '" + sound + "'");
            return;
        }

        SongTransition transition = new SongTransition(
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.playSound(point.getLocation(), sound, SoundCategory.MUSIC, 1f, 1f);
                    debug(player, "playing sound '" + sound + "'");
                    SongTransition t = transitions.get(id);
                    if (t != null && t.markStartComplete()) {
                        transitions.remove(id);
                    }
                }, Math.max(0L, FADE_IN_TICKS)),
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.stopSound(current, SoundCategory.MUSIC);
                    SongTransition t = transitions.get(id);
                    if (t != null && t.markStopComplete()) {
                        transitions.remove(id);
                    }
                }, Math.max(0L, FADE_OUT_TICKS))
        );

        transitions.put(id, transition);
        playing.put(id, sound);
    }

    private void cancelTransition(UUID id) {
        SongTransition existing = transitions.remove(id);
        if (existing != null) {
            existing.cancel();
        }
    }

    private void stopVanillaForIdlePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!playing.containsKey(p.getUniqueId())) {
                p.stopSound(SoundCategory.MUSIC);
            }
        }
    }

    private void debug(Player player, String msg) {
        plugin.getLogger().fine("[LocationMusic] " + player.getName() + ": " + msg);
    }

    private String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
    }

    private static final class SongTransition {
        private final org.bukkit.scheduler.BukkitTask startTask;
        private final org.bukkit.scheduler.BukkitTask stopTask;
        private boolean startComplete;
        private boolean stopComplete;

        SongTransition(org.bukkit.scheduler.BukkitTask startTask,
                       org.bukkit.scheduler.BukkitTask stopTask) {
            this.startTask = startTask;
            this.stopTask = stopTask;
        }

        void cancel() {
            if (startTask != null) startTask.cancel();
            if (stopTask != null) stopTask.cancel();
        }

        boolean markStartComplete() {
            startComplete = true;
            return stopTask == null || stopComplete;
        }

        boolean markStopComplete() {
            stopComplete = true;
            return startTask == null || startComplete;
        }
    }
}
