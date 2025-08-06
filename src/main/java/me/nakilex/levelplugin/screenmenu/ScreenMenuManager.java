package me.nakilex.levelplugin.screenmenu;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.HologramUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

/**
 * Loads simple screen menus from {@code screenmenus.yml} and can display them
 * in front of players.
 */
public class ScreenMenuManager implements Listener {

    private final Main plugin;
    private final File configFile;
    private Map<String, ScreenMenu> menus = new HashMap<>();
    private final Map<UUID, List<TextDisplay>> active = new HashMap<>();

    public ScreenMenuManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "screenmenus.yml");
        if (!configFile.exists()) {
            plugin.saveResource("screenmenus.yml", false);
        }
        reload();
    }

    public void reload() {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        Map<String, ScreenMenu> loaded = new HashMap<>();
        ConfigurationSection menusSec = cfg.getConfigurationSection("menus");
        if (menusSec != null) {
            for (String key : menusSec.getKeys(false)) {
                ConfigurationSection ms = menusSec.getConfigurationSection(key);
                double distance = ms.getDouble("distance", 2.0);
                List<ScreenMenuEntry> entries = new ArrayList<>();
                for (Map<?, ?> map : ms.getMapList("entries")) {
                    String text = Objects.toString(map.get("text"), "");
                    double x = ((Number) map.getOrDefault("x", 0.0)).doubleValue();
                    double y = ((Number) map.getOrDefault("y", 0.0)).doubleValue();
                    String command = Objects.toString(map.get("command"), "");
                    entries.add(new ScreenMenuEntry(text, x, y, command));
                }
                loaded.put(key, new ScreenMenu(distance, entries));
            }
        }
        menus = loaded;
    }

    public void showMenu(Player player, String name) {
        ScreenMenu menu = menus.get(name);
        if (menu == null) return;
        closeMenu(player);

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.clone().crossProduct(dir).normalize();
        Location base = eye.add(dir.multiply(menu.getDistance()));

        List<TextDisplay> displays = new ArrayList<>();
        for (ScreenMenuEntry entry : menu.getEntries()) {
            Location loc = base.clone()
                    .add(right.clone().multiply(entry.x()))
                    .add(up.clone().multiply(entry.y()));
            TextDisplay td = HologramUtil.spawnTextDisplay(loc, disp -> {
                disp.setText(entry.text());
            });
            displays.add(td);
        }
        active.put(player.getUniqueId(), displays);
    }

    public void closeMenu(Player player) {
        List<TextDisplay> list = active.remove(player.getUniqueId());
        if (list != null) {
            for (TextDisplay td : list) {
                td.remove();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        closeMenu(e.getPlayer());
    }
}
