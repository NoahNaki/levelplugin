package me.nakilex.levelplugin.cursormenu.display;

import me.nakilex.levelplugin.cursormenu.util.ColorParser;
import me.nakilex.levelplugin.cursormenu.util.ConfigUtils;
import me.nakilex.levelplugin.cursormenu.util.DisplayUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles simple text displays positioned in front of players. This minimal
 * implementation spawns static text without animations.
 */
public class TextDisplayManager implements DisplayManager<String>, Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Set<TextDisplay>> active = new ConcurrentHashMap<>();
    private FileConfiguration config;

    public TextDisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigUtils.loadConfig(plugin, "cursormenu/text.yml");
    }

    @Override
    public void show(Player player, String id) {
        ConfigurationSection section = config.getConfigurationSection(id);
        if (section == null) return;

        List<String> lines = section.getStringList("lines");
        String joined = String.join("\n", lines);
        String parsed = ColorParser.parse(joined);
        Component component = LegacyComponentSerializer.legacySection().deserialize(parsed);

        double forward = section.getDouble("forward", 2.0);
        double x = section.getDouble("offset.x", 0.0);
        double y = section.getDouble("offset.y", 0.0);
        double z = section.getDouble("offset.z", 0.0);

        Location loc = DisplayUtils.getRelativeLocation(player, forward, x, y, z);

        TextDisplay display = player.getWorld().spawn(loc, TextDisplay.class, d -> d.text(component));
        active.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(display);
    }

    @Override
    public void hide(Player player) {
        Set<TextDisplay> displays = active.remove(player.getUniqueId());
        if (displays != null) {
            displays.forEach(TextDisplay::remove);
        }
    }

    @Override
    public void reload() {
        this.config = ConfigUtils.loadConfig(plugin, "cursormenu/text.yml");
    }

    @Override
    public Set<String> getAllIds() {
        return config.getKeys(false);
    }

    @Override
    public void cleanup(Player player) {
        hide(player);
    }

    public Set<String> getPlayerActiveText(Player player) {
        Set<TextDisplay> displays = active.get(player.getUniqueId());
        if (displays == null) return Collections.emptySet();
        Set<String> ids = new HashSet<>();
        for (TextDisplay d : displays) ids.add(d.getUniqueId().toString());
        return ids;
    }
}
