package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.ChatColor;

/** Shared textual or resource-pack-placeholder gauge renderer for fishing challenges. */
public final class FishingGaugeRenderer {
    private FishingGaugeRenderer() { }

    public static String render(FishingGaugeSettings settings, double pointer, double targetCenter, double targetWidth) {
        StringBuilder gauge = new StringBuilder(ChatColor.DARK_GRAY + "[");
        for (int index = 0; index < settings.width(); index++) {
            double position = index / (double) (settings.width() - 1);
            if (Math.abs(position - pointer) <= 0.5 / settings.width()) {
                gauge.append(ChatColor.WHITE).append(settings.pointer());
            } else if (Math.abs(position - targetCenter) <= targetWidth / 2.0) {
                gauge.append(ChatColor.GREEN).append(settings.target());
            } else {
                gauge.append(ChatColor.GRAY).append(settings.empty());
            }
        }
        return gauge.append(ChatColor.DARK_GRAY).append("]").toString();
    }
}
