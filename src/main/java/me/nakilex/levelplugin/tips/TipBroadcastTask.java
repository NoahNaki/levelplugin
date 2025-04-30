package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.tips.BroadcastManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class TipBroadcastTask extends BukkitRunnable {
    private static final String TIP_PREFIX = "&e[&a&lTIP&e] &f";

    private final Main plugin;
    private final TipsConfigManager cfg;
    private final BroadcastManager manager;
    private final List<String> tips;
    private int index = 0;

    public TipBroadcastTask(Main plugin, TipsConfigManager cfg, BroadcastManager manager) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.manager = manager;
        this.tips = cfg.getTips();
    }

    @Override
    public void run() {
        if (tips.isEmpty()) return;
        if (index >= tips.size()) index = 0;

        // Load tip body from config (no prefix needed in YAML)
        String tipBody = tips.get(index);
        // Combine prefix and tip body, then translate '&' codes
        String raw = TIP_PREFIX + tipBody;
        String formatted = ChatColor.translateAlternateColorCodes('&', raw);

        // Send to each online player, centered
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ChatFormatter.sendCenteredMessage(player, formatted);
        }

        plugin.getLogger().info("[Tips] Broadcasted tip #" + (index + 1));
        index++;
        manager.resetCountdown();
    }
}