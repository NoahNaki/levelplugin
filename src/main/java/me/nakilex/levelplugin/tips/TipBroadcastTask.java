package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.tips.BroadcastManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TipBroadcastTask extends BukkitRunnable {
    private static final String TIP_PREFIX = "&e[&a&lTIP&e] &f";

    private final Main plugin;
    private final TipsConfigManager cfg;
    private final BroadcastManager manager;

    public TipBroadcastTask(Main plugin, TipsConfigManager cfg, BroadcastManager manager) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.manager = manager;
    }

    @Override
    public void run() {
        String tipBody = cfg.nextTip();
        if (tipBody == null || tipBody.isBlank()) {
            manager.resetCountdown();
            return;
        }
        // Combine prefix and tip body, then translate '&' codes
        String raw = TIP_PREFIX + tipBody;
        String formatted = ChatColor.translateAlternateColorCodes('&', raw);

        SettingsManager settingsManager = plugin.getSettingsManager();
        int recipients = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (settingsManager != null) {
                PlayerSettings settings = settingsManager.getSettings(player);
                if (settings != null && !settings.isTipsEnabled()) {
                    continue;
                }
            }
            ChatFormatter.sendCenteredMessage(player, formatted);
            recipients++;
        }

        String stripped = ChatColor.stripColor(formatted);
        if (stripped.length() > 80) {
            stripped = stripped.substring(0, 77) + "...";
        }
        plugin.getLogger().info("[Tips] Broadcasted to " + recipients + " players: " + stripped);
        manager.resetCountdown();
    }
}