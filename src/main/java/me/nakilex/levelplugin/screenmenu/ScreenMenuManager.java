package me.nakilex.levelplugin.screenmenu;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

/**
 * Loads simple screen menus from a YAML file and shows them to players using
 * text display entities. The implementation is intentionally lightweight – it
 * does not mirror every feature of the original CustomScreenMenu plugin but
 * provides a foundation for configurable on‑screen menus within LevelPlugin.
 */
public class ScreenMenuManager implements Listener {

    private final Main plugin;
    private final Map<String, ScreenMenu> menus = new HashMap<>();
    private final Map<UUID, ActiveMenu> activeMenus = new HashMap<>();
    private final File configFile;
    private YamlConfiguration config;

    public ScreenMenuManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "screenmenus.yml");
        if (!configFile.exists()) {
            plugin.saveResource("screenmenus.yml", false);
        }
        reload();
    }

    /** Reloads menu definitions from disk. */
    public final void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        menus.clear();
        ConfigurationSection root = config.getConfigurationSection("menus");
        if (root == null) return;
        for (String menuId : root.getKeys(false)) {
            ConfigurationSection menuSec = root.getConfigurationSection(menuId);
            if (menuSec == null) continue;
            List<MenuEntry> entries = new ArrayList<>();
            for (String key : menuSec.getKeys(false)) {
                ConfigurationSection entrySec = menuSec.getConfigurationSection(key);
                if (entrySec == null) continue;
                String text = entrySec.getString("text", "");
                double x = entrySec.getDouble("x", 0);
                double y = entrySec.getDouble("y", 0);
                String command = entrySec.getString("command", "");
                entries.add(new MenuEntry(text, x, y, command));
            }
            menus.put(menuId.toLowerCase(Locale.ROOT), new ScreenMenu(entries));
        }
    }

    /** Shows the specified menu to the player. */
    public boolean showMenu(Player player, String id) {
        ScreenMenu menu = menus.get(id.toLowerCase(Locale.ROOT));
        if (menu == null) return false;

        hideMenu(player);

        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = new Vector(0, 1, 0);
        Location base = eye.add(forward.multiply(1));

        List<TextDisplay> displays = new ArrayList<>();
        for (MenuEntry entry : menu.entries()) {
            Location loc = base.clone()
                    .add(right.clone().multiply(entry.x()))
                    .add(up.clone().multiply(entry.y()));
            TextDisplay display = player.getWorld().spawn(loc, TextDisplay.class, td -> {
                td.setText(entry.text());
                td.setBillboard(Billboard.CENTER);
                td.setBackgroundColor(0x00000000);
            });
            displays.add(display);
        }

        ActiveMenu active = new ActiveMenu(player, menu, displays);
        active.start();
        activeMenus.put(player.getUniqueId(), active);
        return true;
    }

    /** Removes any active menu for the player. */
    public void hideMenu(Player player) {
        ActiveMenu active = activeMenus.remove(player.getUniqueId());
        if (active != null) {
            active.stop();
        }
    }

    /** Clears menus for all players, used on plugin shutdown. */
    public void hideAll() {
        for (UUID id : new ArrayList<>(activeMenus.keySet())) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) hideMenu(p);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ActiveMenu active = activeMenus.get(e.getPlayer().getUniqueId());
        if (active != null) {
            e.setCancelled(true);
            active.handleClick();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        hideMenu(e.getPlayer());
    }

    /* === Data records ==================================================== */
    public record MenuEntry(String text, double x, double y, String command) {}
    public record ScreenMenu(List<MenuEntry> entries) {}

    private class ActiveMenu {
        private final Player player;
        private final ScreenMenu menu;
        private final List<TextDisplay> displays;
        private int selected = -1;
        private int taskId;

        ActiveMenu(Player player, ScreenMenu menu, List<TextDisplay> displays) {
            this.player = player;
            this.menu = menu;
            this.displays = displays;
        }

        void start() {
            taskId = new BukkitRunnable() {
                @Override public void run() {
                    Entity target = player.getTargetEntity(4);
                    int newSel = -1;
                    if (target != null) {
                        newSel = displays.indexOf(target);
                    }
                    if (newSel != selected) {
                        updateHighlight(selected, 0x00000000);
                        selected = newSel;
                        updateHighlight(selected, 0x40FFFFFF);
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L).getTaskId();
        }

        void stop() {
            Bukkit.getScheduler().cancelTask(taskId);
            for (TextDisplay td : displays) {
                if (td != null && !td.isDead()) td.remove();
            }
        }

        void handleClick() {
            if (selected < 0 || selected >= menu.entries().size()) return;
            String cmd = menu.entries().get(selected).command();
            ScreenMenuManager.this.hideMenu(player);
            if (cmd != null && !cmd.isEmpty()) {
                if ("leave".equalsIgnoreCase(cmd)) {
                    player.kickPlayer("See you next time!");
                } else {
                    player.performCommand(cmd);
                }
            }
        }

        private void updateHighlight(int index, int color) {
            if (index < 0 || index >= displays.size()) return;
            displays.get(index).setBackgroundColor(color);
        }
    }
}

