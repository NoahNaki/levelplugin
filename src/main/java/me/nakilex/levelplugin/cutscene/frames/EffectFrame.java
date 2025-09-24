package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.cutscene.effects.CutsceneEffects;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;
import org.bukkit.entity.Player;

public class EffectFrame implements Frame {
    private final EffectSettings effects;
    private final long durationMs;

    public EffectFrame(EffectSettings effects, long durationMs) {
        this.effects = effects == null ? EffectSettings.empty() : effects;
        this.durationMs = durationMs;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    public EffectSettings getEffects() {
        return effects;
    }

    @Override
    public org.bukkit.scheduler.BukkitTask play(CutsceneContext context) {
        Player viewer = context.getViewer();
        CutsceneEffects.play(viewer, effects, viewer.getLocation(), context.getPlugin());
        return null;
    }
}
