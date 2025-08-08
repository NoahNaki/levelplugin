package me.nakilex.levelplugin.cursormenu;

import me.nakilex.levelplugin.cursormenu.display.ItemDisplayManager;
import me.nakilex.levelplugin.cursormenu.display.TextDisplayManager;
import me.nakilex.levelplugin.cursormenu.menu.MenuLayout;
import me.nakilex.levelplugin.cursormenu.menu.Section;
import me.nakilex.levelplugin.cursormenu.menu.SectionManager;
import me.nakilex.levelplugin.cursormenu.placeholder.CursorMenuPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core manager for the cursor menu system. Handles configuration loading,
 * simple cursor setup/cleanup and exposes display managers.
 */
public class CursorMenuManager implements Listener {
    private final JavaPlugin plugin;
    private final SectionManager sectionManager = new SectionManager();
    private final Map<UUID, String> currentMenu = new ConcurrentHashMap<>();
    private final Map<UUID, ArmorStand> cursors = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> cursorTasks = new ConcurrentHashMap<>();
    private final ItemDisplayManager itemDisplayManager;
    private final TextDisplayManager textDisplayManager;
    private final File configFile;

    public CursorMenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "cursormenu");
        if (!folder.exists()) folder.mkdirs();
        this.configFile = new File(folder, "config.yml");
        File itemFile = new File(folder, "items.yml");
        File textFile = new File(folder, "text.yml");
        mergeYamlFile(configFile, "cursormenu/config.yml");
        mergeYamlFile(itemFile, "cursormenu/items.yml");
        mergeYamlFile(textFile, "cursormenu/text.yml");
        loadConfig();
        itemDisplayManager = new ItemDisplayManager(plugin, itemFile);
        textDisplayManager = new TextDisplayManager(plugin, textFile);
        // register placeholder if PAPI present
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CursorMenuPlaceholder(this).register();
        }
    }

    public ItemDisplayManager getItemDisplayManager() { return itemDisplayManager; }
    public TextDisplayManager getTextDisplayManager() { return textDisplayManager; }
    public SectionManager getSectionManager() { return sectionManager; }

    public String getCurrentMenu(Player player) { return currentMenu.get(player.getUniqueId()); }

    public void setupCursor(Player player, String key) {
        Section section = sectionManager.get(key);
        if (section == null) return;
        currentMenu.put(player.getUniqueId(), key);
        player.teleport(section.getCamera());

        // Spawn cursor armour stand slightly in front of camera
        Location base = inFront(section.getCamera(), 2);
        ArmorStand stand = spawnCursorArmorStand(base);
        cursors.put(player.getUniqueId(), stand);

        // Show demo item/text displays using the menu key
        itemDisplayManager.showItem(player, key, base.clone());
        textDisplayManager.showText(player, key, base.clone().add(0, 1, 0));

        // Update cursor position based on player's view
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ArmorStand cursor = cursors.get(player.getUniqueId());
            if (cursor == null) {
                BukkitTask t = cursorTasks.remove(player.getUniqueId());
                if (t != null) t.cancel();
                return;
            }
            Location eye = player.getEyeLocation();
            cursor.teleport(inFront(eye, 2));
        }, 0L, 1L);
        cursorTasks.put(player.getUniqueId(), task);
    }

    public void stopCursor(Player player, boolean clean) {
        currentMenu.remove(player.getUniqueId());
        ArmorStand stand = cursors.remove(player.getUniqueId());
        if (stand != null) stand.remove();
        BukkitTask task = cursorTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (clean) {
            itemDisplayManager.hideItem(player);
            textDisplayManager.hideText(player);
        }
    }

    public void reloadPluginConfig() {
        mergeYamlFile(configFile, "cursormenu/config.yml");
        loadConfig();
        itemDisplayManager.reloadConfig();
        textDisplayManager.reloadConfig();
    }

    private void loadConfig() {
        sectionManager.clear();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection menus = cfg.getConfigurationSection("menus");
        if (menus == null) return;
        for (String key : menus.getKeys(false)) {
            ConfigurationSection sec = menus.getConfigurationSection(key);
            if (sec == null) continue;
            Location cam = new Location(
                    Bukkit.getWorld(sec.getString("world", "world")),
                    sec.getDouble("x"),
                    sec.getDouble("y"),
                    sec.getDouble("z"),
                    (float) sec.getDouble("yaw"),
                    (float) sec.getDouble("pitch")
            );
            Section section = new Section(key, cam, sec.getString("permission"));
            ConfigurationSection layouts = sec.getConfigurationSection("layouts");
            if (layouts != null) {
                for (String id : layouts.getKeys(false)) {
                    ConfigurationSection lc = layouts.getConfigurationSection(id);
                    if (lc == null) continue;
                    MenuLayout layout = new MenuLayout(
                            id,
                            lc.getDouble("x"),
                            lc.getDouble("y"),
                            lc.getDouble("z"),
                            lc.getStringList("commands"),
                            null,
                            lc.getBoolean("stop", false),
                            lc.getString("permission")
                    );
                    section.addLayout(layout);
                }
            }
            sectionManager.addSection(key, section);
        }
    }

    private void mergeYamlFile(File file, String resourcePath) {
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
            return;
        }
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return;
            FileConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
            FileConfiguration existing = YamlConfiguration.loadConfiguration(file);
            boolean modified = false;
            for (String key : def.getKeys(true)) {
                if (!existing.contains(key)) {
                    existing.set(key, def.get(key));
                    modified = true;
                }
            }
            if (modified) existing.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to merge config for " + resourcePath);
        }
    }

    private ArmorStand spawnCursorArmorStand(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setMarker(true);
            a.getEquipment().setHelmet(new ItemStack(Material.BARRIER));
        });
    }

    private Location inFront(Location origin, double distance) {
        return origin.clone().add(origin.getDirection().normalize().multiply(distance));
    }
}
