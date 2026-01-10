package me.nakilex.levelplugin.hud.render;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudBossBarDisplay {
    private final int lines;
    private final BossBar.Color barColor;
    private final BossBar.Overlay barStyle;
    private final Map<UUID, List<BossBar>> barsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> lastLines = new ConcurrentHashMap<>();

    public HudBossBarDisplay(int lines, BossBar.Color barColor, BossBar.Overlay barStyle) {
        this.lines = Math.max(1, lines);
        this.barColor = barColor == null ? BossBar.Color.WHITE : barColor;
        this.barStyle = barStyle == null ? BossBar.Overlay.PROGRESS : barStyle;
    }

    public void update(Player player, HudRenderOutput output) {
        if (player == null) {
            return;
        }
        List<String> newLines = output == null ? List.of() : output.getBossBarLineTexts();
        List<String> last = lastLines.getOrDefault(player.getUniqueId(), List.of());
        if (newLines.equals(last)) {
            return;
        }
        List<Component> newComponents = output == null ? List.of() : output.getBossBarLineComponents();
        lastLines.put(player.getUniqueId(), new ArrayList<>(newLines));
        List<BossBar> bars = barsByPlayer.computeIfAbsent(player.getUniqueId(), id -> createBars(player));
        for (int index = 0; index < lines; index++) {
            BossBar bar = bars.get(index);
            String title = index < newLines.size() ? newLines.get(index) : "";
            Component component = index < newComponents.size() ? newComponents.get(index) : Component.empty();
            boolean visible = title != null && !title.isBlank();
            bar.name(visible ? component : Component.empty());
            bar.progress(0.0f);
            bar.visible(visible);
            player.showBossBar(bar);
        }
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        List<BossBar> bars = barsByPlayer.remove(player.getUniqueId());
        if (bars != null) {
            for (BossBar bar : bars) {
                player.hideBossBar(bar);
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
            BossBar bar = BossBar.bossBar(Component.empty(), 1.0f, barColor, barStyle);
            bar.visible(false);
            player.showBossBar(bar);
            bars.add(bar);
        }
        return bars;
    }
}
