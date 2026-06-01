package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class HoldFishingMiniGame extends AbstractFishingMiniGame {
    private final double zoneWidth, burstChance, burstStrength, damping, requiredHold, controlGain, controlDecay;
    private double fish = 0.5, velocity, control = 0.5, held;
    public HoldFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration c, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Fish struggle: click to keep ◆ in the green zone!", completion);
        zoneWidth = c.getDouble("fishing-mini-games.hold.zone-width", 0.28);
        burstChance = c.getDouble("fishing-mini-games.hold.burst-chance", 0.08);
        burstStrength = c.getDouble("fishing-mini-games.hold.burst-strength", 0.08);
        damping = c.getDouble("fishing-mini-games.hold.damping", 0.12);
        requiredHold = c.getDouble("fishing-mini-games.hold.required-hold", 2.4);
        controlGain = c.getDouble("fishing-mini-games.hold.click-gain", 0.08);
        controlDecay = c.getDouble("fishing-mini-games.hold.control-decay", 0.025);
    }
    @Override protected void tick() {
        if (ThreadLocalRandom.current().nextDouble() < burstChance) velocity += ThreadLocalRandom.current().nextDouble(-burstStrength, burstStrength);
        velocity *= 1.0 - damping;
        fish = clamp(fish + velocity);
        if (fish == 0.0 || fish == 1.0) velocity *= -0.55;
        control = clamp(control - controlDecay);
        boolean inZone = Math.abs(fish - control) <= zoneWidth / 2.0;
        held = clamp(held + (inZone ? 0.05 / requiredHold : -0.025));
        updateBar("Keep the fish in your control zone!", held);
        if (useResourcePack()) {
            actionBar(FishingGlyphs.fishWithJudgement(fish, control - zoneWidth / 2.0,
                    FishingGlyphs.JUDGEMENT_NORMAL, 49)
                    .append(Component.text("  ")).append(FishingGlyphs.icon(FishingGlyphs.PROGRESS_ICON))
                    .append(FishingGlyphs.bar(held)));
        } else if (useFallbackTextUi()) {
            actionBar(ChatColor.AQUA + "Control: " + pointer(fish, control - zoneWidth / 2.0, control + zoneWidth / 2.0, 21));
        } else {
            actionBar(Component.empty());
        }
        if (held >= 1.0) finish(true);
    }
    @Override public void handleClick() { control = clamp(control + controlGain); }
}
