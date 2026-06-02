package me.nakilex.levelplugin.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Reusable scheduler for short player-local sound melodies. */
public final class SoundMelodyUtil {
    private SoundMelodyUtil() {
    }

    /** A single sound in a melody, scheduled relative to the start in server ticks. */
    public record Note(long delayTicks, float pitch) {
        public Note {
            delayTicks = Math.max(0L, delayTicks);
        }
    }

    /** Plays a short melody using one sound instrument and a sequence of timed pitches. */
    public static void play(JavaPlugin plugin, Player player, Sound sound, float volume, Note... notes) {
        if (plugin == null || player == null || sound == null || notes == null) return;
        for (Note note : notes) {
            if (note == null) continue;
            Runnable playNote = () -> {
                if (player.isOnline()) player.playSound(player.getLocation(), sound, volume, note.pitch());
            };
            if (note.delayTicks() == 0L) {
                playNote.run();
            } else {
                plugin.getServer().getScheduler().runTaskLater(plugin, playNote, note.delayTicks());
            }
        }
    }
}
