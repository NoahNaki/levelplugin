package me.nakilex.levelplugin.fishing.core.feedback;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.core.FishingConfigManager;
import me.nakilex.levelplugin.player.attributes.managers.CooldownIndicatorManager;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FeedbackService {
    private final FishingConfigManager configManager;

    public FeedbackService(FishingConfigManager configManager) {
        this.configManager = configManager;
    }

    public void playBite(FishingContext context) {
        playPreset(context, configManager.getBitePreset());
    }

    public void playHooked(FishingContext context) {
        playPreset(context, configManager.getHookedPreset());
    }

    public void playSuccess(FishingContext context) {
        playPreset(context, configManager.getSuccessPreset());
    }

    public void playFail(FishingContext context) {
        playPreset(context, configManager.getFailPreset());
    }

    public void showActionBar(Player player, String message, long durationMs) {
        if (player == null || message == null) {
            return;
        }
        String formatted = ChatColor.translateAlternateColorCodes('&', ChatUtil.applyEmojis(message));
        CooldownIndicatorManager.getInstance().showActionBar(player, formatted, durationMs);
    }

    public void playLineParticles(FishingContext context) {
        FeedbackPreset preset = configManager.getLinePreset();
        if (preset == null) {
            return;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return;
        }
        Particle particle = resolveParticle(preset.particle());
        if (particle == null) {
            return;
        }
        Location start = player.getLocation().add(0, 1.2, 0);
        Location end = context.getLocation().clone().add(0, 0.2, 0);
        int points = Math.max(4, preset.particleCount());
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            Location point = start.clone().lerp(end, t);
            player.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    public String formatBossBarTitle(String raw) {
        return raw == null ? "" : ChatColor.translateAlternateColorCodes('&', ChatUtil.applyEmojis(raw));
    }

    private void playPreset(FishingContext context, FeedbackPreset preset) {
        if (preset == null) {
            return;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return;
        }
        Sound sound = resolveSound(preset.sound());
        if (sound != null) {
            player.playSound(player.getLocation(), sound, preset.volume(), preset.pitch());
        }
        Particle particle = resolveParticle(preset.particle());
        if (particle != null) {
            player.getWorld().spawnParticle(
                    particle,
                    context.getLocation().clone().add(0, 0.2, 0),
                    preset.particleCount(),
                    preset.particleOffset(),
                    preset.particleOffset(),
                    preset.particleOffset(),
                    0.0
            );
        }
        if (preset.title() != null || preset.subtitle() != null) {
            String title = formatText(preset.title());
            String subtitle = formatText(preset.subtitle());
            player.sendTitle(title, subtitle, preset.titleFadeIn(), preset.titleStay(), preset.titleFadeOut());
        }
        if (preset.actionBar() != null) {
            showActionBar(player, preset.actionBar(), preset.titleStay() * 50L);
        }
        if (!preset.messages().isEmpty()) {
            for (String message : preset.messages()) {
                player.sendMessage(formatText(message));
            }
        }
    }

    private String formatText(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', ChatUtil.applyEmojis(text));
    }

    private Sound resolveSound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Particle resolveParticle(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Particle.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
