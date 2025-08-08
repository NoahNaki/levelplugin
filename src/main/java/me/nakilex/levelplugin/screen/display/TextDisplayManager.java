package me.nakilex.levelplugin.screen.display;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.screen.util.ColorParser;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates simple text displays for menus. Animations are intentionally minimal;
 * more complex behaviour can be added on top of this generic base.
 */
public class TextDisplayManager {
    private final Main plugin;
    private final Map<String, List<String>> templates = new ConcurrentHashMap<>();
    private final Map<UUID, List<TextDisplay>> active = new ConcurrentHashMap<>();

    public TextDisplayManager(Main plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        templates.clear();
        File file = new File(plugin.getDataFolder(), "cursor-text.yml");
        if (!file.exists()) {
            plugin.saveResource("cursor-text.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            List<String> lines = cfg.getStringList(key);
            templates.put(key.toLowerCase(), lines);
        }
    }

    public void showTextDisplays(Player player, String id) {
        List<String> lines = templates.get(id.toLowerCase());
        if (lines == null) return;
        Location base = player.getLocation().add(player.getLocation().getDirection().multiply(3));
        List<TextDisplay> displays = new ArrayList<>();
        double yOffset = 0;
        for (String line : lines) {
            Location loc = base.clone().add(0, yOffset, 0);
            TextDisplay disp = player.getWorld().spawn(loc, TextDisplay.class, d -> {
                d.setBillboard(Display.Billboard.CENTER);
                d.setText(ColorParser.parse(line));
            });
            displays.add(disp);
            yOffset -= 0.25;
        }
        active.put(player.getUniqueId(), displays);
    }

    public void clearPlayerDisplays(Player player) {
        List<TextDisplay> displays = active.remove(player.getUniqueId());
        if (displays != null) {
            displays.forEach(TextDisplay::remove);
        }
    }

    public Iterable<String> getAllTextIds() {
        return templates.keySet();
    }
}
