package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class LifeSkillBossBarUtil {

    private LifeSkillBossBarUtil() {
    }

    public static void updateBossBar(Player player,
                                     Map<UUID, BossBar> xpBars,
                                     Map<UUID, Boolean> activeBars,
                                     BarColor barColor,
                                     ChatColor titleColor,
                                     String skillName,
                                     int level,
                                     int xp,
                                     int required,
                                     boolean atMax) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        BossBar bar = xpBars.computeIfAbsent(uuid, id -> {
            BossBar created = Bukkit.createBossBar("", barColor, BarStyle.SOLID);
            created.addPlayer(player);
            created.setVisible(false);
            return created;
        });

        double progress = atMax ? 1.0 : required <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, xp / (double) required));
        boolean showBar = activeBars.getOrDefault(uuid, false);
        if (!showBar) {
            bar.removePlayer(player);
            bar.setVisible(false);
            return;
        }
        String progressLabel = atMax ? (ChatColor.GREEN + "MAX") : (ChatColor.WHITE + String.valueOf(xp)
                + ChatColor.GRAY + "/" + ChatColor.WHITE + required);

        String title = titleColor + "" + ChatColor.BOLD + skillName + " "
                + ChatColor.GRAY + "(Lv. " + ChatColor.WHITE + level + ChatColor.GRAY + ") "
                + ChatColor.DARK_GRAY + "| "
                + progressLabel;

        bar.setTitle(title);
        bar.setProgress(progress);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
        bar.setVisible(true);
    }
}
