package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.entity.Player;

public final class SpellDamageUtil {
    private SpellDamageUtil() {
    }

    public static double computeScaledDamage(Player player,
                                             double baseDamage,
                                             StatType primaryStat,
                                             double primaryStatScale,
                                             double techniqueScale) {
        if (player == null) {
            return Math.max(0.0, baseDamage);
        }
        StatsManager statsManager = StatsManager.getInstance();
        int primary = statsManager.getStatValue(player, primaryStat);
        int technique = statsManager.getStatValue(player, StatType.TEC);

        double scaled = Math.max(0.0, baseDamage) + Math.max(0, primary) * Math.max(0.0, primaryStatScale);
        return scaled * (1.0 + Math.max(0, technique) * Math.max(0.0, techniqueScale));
    }
}
