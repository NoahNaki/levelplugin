package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class TipBroadcastTask extends BukkitRunnable {
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
        // Wrap index if needed
        if (index >= tips.size()) index = 0;
        String raw = tips.get(index);
        // Translate '&' to section sign for colors
        String colored = ChatColor.translateAlternateColorCodes('&', raw);
        plugin.getServer().broadcastMessage(colored);
        plugin.getLogger().info("[Tips] Broadcasted tip #" + (index + 1));
        index++;
        manager.resetCountdown();
    }
}
