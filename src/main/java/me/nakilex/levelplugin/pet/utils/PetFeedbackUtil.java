package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public final class PetFeedbackUtil {
    private PetFeedbackUtil() {
    }

    public static void playMenuOpen(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.65f, 1.25f);
    }

    public static void playMenuSelect(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.55f, 1.45f);
    }

    public static void playLockToggle(Player player, boolean locked) {
        play(player,
                locked ? Sound.BLOCK_IRON_TRAPDOOR_CLOSE : Sound.BLOCK_IRON_TRAPDOOR_OPEN,
                0.7f,
                locked ? 0.9f : 1.1f);
    }

    public static void playMergePulse(Player player, int step) {
        if (player == null) {
            return;
        }
        float pitch = 1.0f + (Math.max(0, step) * 0.08f);
        play(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.55f, pitch);
        Location loc = player.getLocation().clone().add(0, 1.1, 0);
        player.spawnParticle(Particle.ENCHANT, loc, 12, 0.2, 0.2, 0.2, 0.01);
    }

    public static void playMergeResult(Player player, boolean success) {
        play(player,
                success ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_NOTE_BLOCK_BASS,
                success ? 1.0f : 0.8f,
                success ? 1.05f : 0.8f);
        if (success && player != null) {
            player.spawnParticle(Particle.FIREWORK, player.getLocation().clone().add(0, 1.1, 0),
                    20, 0.4, 0.25, 0.4, 0.02);
        }
    }

    public static void playSummonTransition(Player player) {
        play(player, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.05f);
    }

    public static void playSummonComplete(Player player) {
        play(player, Sound.ENTITY_PLAYER_LEVELUP, 0.85f, 1.15f);
        play(player, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.55f, 1.25f);
    }

    public static void applyBlindnessTransition(Player player, int ticks) {
        if (player == null) {
            return;
        }
        int duration = Math.max(1, ticks);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, false, false));
    }

    public static void runMergeAnimation(Player player, Runnable onFinish) {
        if (player == null) {
            if (onFinish != null) {
                onFinish.run();
            }
            return;
        }
        Plugin plugin = Main.getInstance();
        if (plugin == null) {
            if (onFinish != null) {
                onFinish.run();
            }
            return;
        }
        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (step >= 4) {
                    cancel();
                    if (onFinish != null) {
                        onFinish.run();
                    }
                    return;
                }
                playMergePulse(player, step);
                step++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private static void play(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
