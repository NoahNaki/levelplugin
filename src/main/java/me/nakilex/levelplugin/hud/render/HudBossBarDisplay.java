package me.nakilex.levelplugin.hud.render;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudBossBarDisplay {
    private final int lines;
    private final BarColor barColor;
    private final BarStyle barStyle;
    private final Map<UUID, List<BossBar>> barsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> lastLines = new ConcurrentHashMap<>();

    public HudBossBarDisplay(int lines, BarColor barColor, BarStyle barStyle) {
        this.lines = Math.max(1, lines);
        this.barColor = barColor == null ? BarColor.WHITE : barColor;
        this.barStyle = barStyle == null ? BarStyle.SOLID : barStyle;
    }

    public void update(Player player, HudRenderOutput output) {
        if (player == null) {
            return;
        }
        List<String> newLines = output == null ? List.of() : output.getBossBarLines();
        List<String> last = lastLines.getOrDefault(player.getUniqueId(), List.of());
        if (newLines.equals(last)) {
            return;
        }
        lastLines.put(player.getUniqueId(), new ArrayList<>(newLines));
        List<BossBar> bars = barsByPlayer.computeIfAbsent(player.getUniqueId(), id -> createBars(player));
        for (int index = 0; index < lines; index++) {
            BossBar bar = bars.get(index);
            String title = index < newLines.size() ? newLines.get(index) : "";
            bar.setTitle(title == null ? "" : title);
            bar.setProgress(1.0);
            bar.setVisible(!title.isBlank());
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        List<BossBar> bars = barsByPlayer.remove(player.getUniqueId());
        if (bars != null) {
            for (BossBar bar : bars) {
                bar.removeAll();
            }
        }
        lastLines.remove(player.getUniqueId());
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
        barsByPlayer.clear();
        lastLines.clear();
    }

    private List<BossBar> createBars(Player player) {
        List<BossBar> bars = new ArrayList<>(lines);
        for (int i = 0; i < lines; i++) {
            BossBar bar = Bukkit.createBossBar("", barColor, barStyle);
            bar.addPlayer(player);
            bar.setVisible(false);
            bars.add(bar);
        }
        return bars;
    }
}
