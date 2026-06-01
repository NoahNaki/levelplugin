package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class HoldFishingMiniGame extends AbstractFishingMiniGame {
    private final double zoneWidth, burstChance, burstStrength, damping, requiredHold, controlGain, controlDecay, outsideZonePenalty;
    private final int judgementWidth;
    private final char judgementGlyph;
    private double fish = 0.5, velocity, control = 0.5, held;
    public HoldFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration c,
                               FishingDifficultyProfile profile, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Fish struggle: click to keep ◆ in the green zone!", completion);
        zoneWidth = Math.max(0.16, Math.min(0.45,
                c.getDouble("fishing-mini-games.hold.zone-width", 0.28) * profile.zoneMultiplier()));
        burstChance = c.getDouble("fishing-mini-games.hold.burst-chance", 0.08) * profile.speedMultiplier();
        burstStrength = c.getDouble("fishing-mini-games.hold.burst-strength", 0.08) * profile.speedMultiplier();
        damping = c.getDouble("fishing-mini-games.hold.damping", 0.12);
        requiredHold = c.getDouble("fishing-mini-games.hold.required-hold", 2.4) * profile.requiredProgressMultiplier();
        controlGain = c.getDouble("fishing-mini-games.hold.click-gain", 0.08) * profile.zoneMultiplier();
        controlDecay = c.getDouble("fishing-mini-games.hold.control-decay", 0.025) / profile.zoneMultiplier();
        outsideZonePenalty = 0.025 * profile.decayMultiplier();
        int normalWidth = Math.max(1, c.getInt("fishing-mini-games.hold.judgement-width",
                FishingGlyphs.HOLD_JUDGEMENT_NORMAL_WIDTH));
        judgementWidth = switch (profile.tier()) {
            case EASY -> FishingGlyphs.HOLD_JUDGEMENT_EASY_WIDTH;
            case NORMAL -> normalWidth;
            case HARD, EXTREME -> FishingGlyphs.HOLD_JUDGEMENT_HARD_WIDTH;
        };
        judgementGlyph = switch (profile.tier()) {
            case EASY -> FishingGlyphs.JUDGEMENT_EASY;
            case NORMAL -> FishingGlyphs.JUDGEMENT_NORMAL;
            case HARD, EXTREME -> FishingGlyphs.JUDGEMENT_HARD;
        };
    }
    @Override protected void tick() {
        if (ThreadLocalRandom.current().nextDouble() < burstChance) velocity += ThreadLocalRandom.current().nextDouble(-burstStrength, burstStrength);
        velocity *= 1.0 - damping;
        fish = clamp(fish + velocity);
        if (fish == 0.0 || fish == 1.0) velocity *= -0.55;
        control = clamp(control - controlDecay);
        boolean inZone = Math.abs(fish - control) <= zoneWidth / 2.0;
        held = clamp(held + (inZone ? 0.05 / requiredHold : -outsideZonePenalty));
        updateBar("Keep the fish in your control zone!", held);
        if (useResourcePack()) {
            double judgementStart = clamp(control - zoneWidth / 2.0);
            showGameTitle(FishingGlyphs.progressIcon(held),
                    FishingGlyphs.hold(fish, judgementStart, judgementWidth, judgementGlyph));
            actionBar(Component.text("Left-click to move your control area"));
        } else if (useFallbackTextUi()) {
            actionBar(ChatColor.AQUA + "Control: " + pointer(fish, control - zoneWidth / 2.0, control + zoneWidth / 2.0, 21));
        } else {
            actionBar(Component.empty());
        }
        if (held >= 1.0) finish(true);
    }
    @Override public void handleClick() { control = clamp(control + controlGain); }
}
