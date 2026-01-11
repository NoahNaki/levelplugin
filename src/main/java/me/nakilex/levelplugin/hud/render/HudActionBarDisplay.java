package me.nakilex.levelplugin.hud.render;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class HudActionBarDisplay implements HudDisplay {
    @Override
    public void update(Player player, HudRenderOutput output) {
        if (player == null) {
            return;
        }
        List<Component> components = output == null ? List.of() : output.getBossBarLineComponents();
        Component combined = joinLines(components);
        player.sendActionBar(combined);
    }

    @Override
    public void clear(Player player) {
        if (player == null) {
            return;
        }
        player.sendActionBar(Component.empty());
    }

    @Override
    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
    }

    private Component joinLines(List<Component> lines) {
        if (lines == null || lines.isEmpty()) {
            return Component.empty();
        }
        Component combined = Component.empty();
        boolean first = true;
        for (Component line : lines) {
            if (line == null || line.equals(Component.empty())) {
                continue;
            }
            if (!first) {
                combined = combined.append(Component.newline());
            }
            combined = combined.append(line);
            first = false;
        }
        return combined;
    }
}
