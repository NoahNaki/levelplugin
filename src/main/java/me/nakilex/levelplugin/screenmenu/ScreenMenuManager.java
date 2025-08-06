package me.nakilex.levelplugin.screenmenu;

import me.nakilex.levelplugin.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
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

            double distance = menuSec.getDouble("distance", 1.0);
            Float yaw = menuSec.isSet("yaw") ? (float) menuSec.getDouble("yaw") : null;
            Float pitch = menuSec.isSet("pitch") ? (float) menuSec.getDouble("pitch") : null;

            List<MenuEntry> entries = new ArrayList<>();
            for (String key : menuSec.getKeys(false)) {
                if ("distance".equalsIgnoreCase(key) || "yaw".equalsIgnoreCase(key)
                        || "pitch".equalsIgnoreCase(key)) {
                    continue;
                }
                ConfigurationSection entrySec = menuSec.getConfigurationSection(key);
                if (entrySec == null) continue;
                String text = entrySec.getString("text", "");
                double x = entrySec.getDouble("x", 0);
                double y = entrySec.getDouble("y", 0);
                String command = entrySec.getString("command", "");
                String item = entrySec.getString("item");
                entries.add(new MenuEntry(text, x, y, command, item));
            }
            menus.put(menuId.toLowerCase(Locale.ROOT), new ScreenMenu(entries, distance, yaw, pitch));
        }
    }

    /** Shows the specified menu to the player. */
    public boolean showMenu(Player player, String id) {
        ScreenMenu menu = menus.get(id.toLowerCase(Locale.ROOT));
        if (menu == null) return false;

        hideMenu(player);

        Location eye = player.getEyeLocation();
        if (menu.yaw() != null || menu.pitch() != null) {
            Location look = eye.clone();
            if (menu.yaw() != null) look.setYaw(menu.yaw());
            if (menu.pitch() != null) look.setPitch(menu.pitch());
            player.teleport(look);
            eye = player.getEyeLocation();
        }

        Vector forward = eye.getDirection().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = new Vector(0, 1, 0);
        Location base = eye.add(forward.multiply(menu.distance()));

        List<Entity> displays = new ArrayList<>();
        for (MenuEntry entry : menu.entries()) {
            Location loc = base.clone()
                    .add(right.clone().multiply(entry.x()))
                    .add(up.clone().multiply(entry.y()));
            Entity display;
            if (entry.item() != null && !entry.item().isEmpty()) {
                Material mat = Material.matchMaterial(entry.item());
                if (mat == null) continue;
                display = spawnItemDisplay(loc, new org.bukkit.inventory.ItemStack(mat));
            } else {
                String text = PlaceholderAPI.setPlaceholders(player, entry.text());
                display = spawnTextDisplay(loc, text);
            }
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
    public record MenuEntry(String text, double x, double y, String command, String item) {}
    public record ScreenMenu(List<MenuEntry> entries, double distance, Float yaw, Float pitch) {}

    private class ActiveMenu {
        private final Player player;
        private final ScreenMenu menu;
        private final List<Entity> displays;
        private int selected = -1;
        private int taskId;

        ActiveMenu(Player player, ScreenMenu menu, List<Entity> displays) {
            this.player = player;
            this.menu = menu;
            this.displays = displays;
        }

        void start() {
            taskId = new BukkitRunnable() {
                @Override public void run() {
                    Entity target = player.getTargetEntity(4);
                    int newSel = target != null ? displays.indexOf(target) : -1;
                    if (newSel != selected) {
                        updateHighlight(selected, false);
                        selected = newSel;
                        updateHighlight(selected, true);
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L).getTaskId();
        }

        void stop() {
            Bukkit.getScheduler().cancelTask(taskId);
            for (Entity e : displays) {
                if (e != null && !e.isDead()) e.remove();
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

        private void updateHighlight(int index, boolean highlight) {
            if (index < 0 || index >= displays.size()) return;
            Entity e = displays.get(index);
            if (e instanceof TextDisplay td) {
                td.setBackgroundColor(highlight
                        ? org.bukkit.Color.fromARGB(64, 255, 255, 255)
                        : org.bukkit.Color.fromARGB(0, 0, 0, 0));
            } else {
                e.setGlowing(highlight);
            }
        }
    }

    private TextDisplay spawnTextDisplay(Location loc, String text) {
        return loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.setText(text);
            td.setBillboard(Billboard.CENTER);
            td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        });
    }

    private ItemDisplay spawnItemDisplay(Location loc, org.bukkit.inventory.ItemStack item) {
        return loc.getWorld().spawn(loc, ItemDisplay.class, id -> id.setItemStack(item));
    }

    /** Spawn a temporary item display in front of the player for demonstration. */
    public void showItem(Player player, org.bukkit.inventory.ItemStack item) {
        Location loc = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize());
        Entity e = spawnItemDisplay(loc, item);
        Bukkit.getScheduler().runTaskLater(plugin, e::remove, 100L);
    }
}

