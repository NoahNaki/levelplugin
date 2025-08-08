package me.nakilex.levelplugin.screen;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.screen.menu.MenuLayout;
import me.nakilex.levelplugin.screen.menu.Section;
import me.nakilex.levelplugin.screen.menu.SectionManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.nakilex.levelplugin.screen.display.ItemDisplayManager;
import me.nakilex.levelplugin.screen.display.TextDisplayManager;

/**
 * Core class for handling the cursor driven menus. It exposes a small API for
 * starting and stopping menus and keeps track of spawned helper entities. Most
 * heavy lifting (text/item displays) is delegated to dedicated managers to keep
 * this class generic.
 */
public class CursorMenuSystem {
    private final Main plugin;
    private final SectionManager sectionManager = new SectionManager();
    private final ItemDisplayManager itemDisplayManager;
    private final TextDisplayManager textDisplayManager;

    // state maps
    private final Map<UUID, Section> currentSection = new ConcurrentHashMap<>();
    private final Map<UUID, MenuLayout> selectedLayout = new ConcurrentHashMap<>();
    private final Map<UUID, ArmorStand> cursors = new ConcurrentHashMap<>();

    public CursorMenuSystem(Main plugin) {
        this.plugin = plugin;
        this.itemDisplayManager = new ItemDisplayManager(plugin);
        this.textDisplayManager = new TextDisplayManager(plugin);
        reloadConfig();
    }

    public SectionManager getSectionManager() { return sectionManager; }
    public ItemDisplayManager getItemDisplayManager() { return itemDisplayManager; }
    public TextDisplayManager getTextDisplayManager() { return textDisplayManager; }

    public void reloadConfig() {
        sectionManager.clear();
        FileConfiguration cfg;
        java.io.File file = new java.io.File(plugin.getDataFolder(), "cursor-config.yml");
        if (!file.exists()) {
            plugin.saveResource("cursor-config.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.isConfigurationSection("sections")) return;
        for (String key : cfg.getConfigurationSection("sections").getKeys(false)) {
            String path = "sections." + key + ".";
            World w = plugin.getServer().getWorld(cfg.getString(path + "world"));
            if (w == null) continue;
            Location cam = new Location(w,
                    cfg.getDouble(path + "x"),
                    cfg.getDouble(path + "y"),
                    cfg.getDouble(path + "z"),
                    (float) cfg.getDouble(path + "yaw"),
                    (float) cfg.getDouble(path + "pitch"));
            Section sec = new Section(key, cam,
                    (float) cfg.getDouble(path + "yaw"),
                    (float) cfg.getDouble(path + "pitch"),
                    cfg.getString(path + "permission"));
            if (cfg.isList(path + "layouts")) {
                java.util.List<java.util.Map<?, ?>> list = cfg.getMapList(path + "layouts");
                for (java.util.Map<?, ?> map : list) {
                    double lx = map.containsKey("x") ? ((Number) map.get("x")).doubleValue() : 0d;
                    double ly = map.containsKey("y") ? ((Number) map.get("y")).doubleValue() : 0d;
                    double lz = map.containsKey("z") ? ((Number) map.get("z")).doubleValue() : 0d;
                    float tilt = map.containsKey("tilt") ? ((Number) map.get("tilt")).floatValue() : 0f;
                    @SuppressWarnings("unchecked")
                    java.util.List<String> cmds = map.containsKey("commands")
                            ? (java.util.List<String>) map.get("commands")
                            : java.util.List.of();
                    sec.addLayout(new MenuLayout(lx, ly, lz, tilt, cmds, false, null, null));
                }
            }
            sectionManager.addSection(sec);
        }
    }

    /** Checks whether the player currently has a cursor menu active. */
    public boolean isInMenu(Player player) {
        return currentSection.containsKey(player.getUniqueId());
    }

    /**
     * Begin displaying a cursor menu for the player.
     */
    public void setupCursor(Player player, String sectionKey) {
        Section section = sectionManager.get(sectionKey);
        if (section == null) return;
        if (section.getPermission() != null && !section.getPermission().isEmpty()
            && !player.hasPermission(section.getPermission())) {
            return;
        }
        Location cam = section.getCamera();
        currentSection.put(player.getUniqueId(), section);
        player.teleport(cam);
        spawnCursorArmorStand(player, cam.getWorld());
    }

    /**
     * Stop the menu for the player and remove any helper entities.
     */
    public void stopCursor(Player player) {
        currentSection.remove(player.getUniqueId());
        selectedLayout.remove(player.getUniqueId());
        ArmorStand stand = cursors.remove(player.getUniqueId());
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    /**
     * Updates the cursor armor stand based on yaw/pitch.
     */
    public void updateCursorPosition(Player player, float yaw, float pitch) {
        ArmorStand stand = cursors.get(player.getUniqueId());
        if (stand == null) return;
        Location base = player.getLocation();
        base.setYaw(yaw);
        base.setPitch(pitch);
        Location target = base.add(base.getDirection().multiply(2.5));
        stand.teleport(target);
    }

    private void spawnCursorArmorStand(Player player, World world) {
        ArmorStand stand = (ArmorStand) world.spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setCustomNameVisible(false);
        cursors.put(player.getUniqueId(), stand);

        // update cursor location every tick while active
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !cursors.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }
                updateCursorPosition(player, player.getLocation().getYaw(), player.getLocation().getPitch());
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
