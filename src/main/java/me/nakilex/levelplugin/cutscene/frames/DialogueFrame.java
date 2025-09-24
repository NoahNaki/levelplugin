package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.cutscene.effects.CutsceneEffects;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DialogueFrame implements Frame {
    private final String speaker;
    private final String message;
    private final String subtitle;
    private final long durationMs;
    private final EffectSettings effects;

    public DialogueFrame(String speaker, String message, String subtitle, long durationMs, EffectSettings effects) {
        this.speaker = speaker;
        this.message = message;
        this.subtitle = subtitle;
        this.durationMs = durationMs;
        this.effects = effects == null ? EffectSettings.empty() : effects;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getMessage() {
        return message;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public EffectSettings getEffects() {
        return effects;
    }

    @Override
    public org.bukkit.scheduler.BukkitTask play(CutsceneContext context) {
        Player viewer = context.getViewer();
        if (viewer == null) {
            return null;
        }
        String prefix = speaker == null || speaker.isEmpty() ? "" : ChatColor.GOLD + speaker + ChatColor.GRAY + ": ";
        if (message != null) {
            viewer.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', message));
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            viewer.sendTitle("", ChatColor.translateAlternateColorCodes('&', subtitle), 10, 40, 10);
        }
        CutsceneEffects.play(viewer, effects, viewer.getLocation(), context.getPlugin());
        return null;
    }
}
