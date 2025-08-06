package me.nakilex.levelplugin.screenmenu;

import me.nakilex.levelplugin.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * A lightweight recreation of the CustomScreenMenu system. Menus are grouped
 * into "sections" that define camera placement while each section holds
 * multiple {@link MenuLayout} entries the player can click.
 */
public class ScreenMenuManager implements Listener {

    private final Main plugin;
    private final SectionManager sectionManager = new SectionManager();
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

    /** Reloads section definitions from disk. */
    public final void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        sectionManager.clear();
        ConfigurationSection root = config.getConfigurationSection("sections");
        if (root == null) return;
        for (String secId : root.getKeys(false)) {
            ConfigurationSection sSec = root.getConfigurationSection(secId);
            if (sSec == null) continue;

            double distance = sSec.getDouble("distance", 2.0);
            String world = sSec.getString("camera.world", "world");
            double camX = sSec.getDouble("camera.x", 0);
            double camY = sSec.getDouble("camera.y", 0);
            double camZ = sSec.getDouble("camera.z", 0);
            float yaw = (float) sSec.getDouble("camera.yaw", 0);
            float pitch = (float) sSec.getDouble("camera.pitch", 0);
            String perm = sSec.getString("permission", "");
            Section section = new Section(distance, world, camX, camY, camZ, yaw, pitch, perm);

            ConfigurationSection layouts = sSec.getConfigurationSection("layouts");
            if (layouts != null) {
                for (String lKey : layouts.getKeys(false)) {
                    ConfigurationSection lSec = layouts.getConfigurationSection(lKey);
                    if (lSec == null) continue;
                    String name = lSec.getString("text", lKey);
                    List<String> commands = lSec.getStringList("command");
                    if (commands.isEmpty()) {
                        String single = lSec.getString("command");
                        if (single != null) commands = Collections.singletonList(single);
                    }
                    boolean stop = lSec.getBoolean("stop", false);
                    double x = lSec.getDouble("x", 0);
                    double y = lSec.getDouble("y", 0);
                    double z = lSec.getDouble("z", 0);
                    boolean tp = lSec.getBoolean("teleport", false);
                    boolean tpBack = lSec.getBoolean("teleport-back", false);
                    Location tpLoc = null;
                    if (tp) {
                        String w = lSec.getString("teleport-to.world", world);
                        double tx = lSec.getDouble("teleport-to.x", camX);
                        double ty = lSec.getDouble("teleport-to.y", camY);
                        double tz = lSec.getDouble("teleport-to.z", camZ);
                        if (Bukkit.getWorld(w) != null) {
                            tpLoc = new Location(Bukkit.getWorld(w), tx, ty, tz);
                        }
                    }
                    List<String> stopCmds = lSec.getStringList("stop-commands");
                    String lPerm = lSec.getString("permission", "");

                    ItemStack item = null;
                    String matName = lSec.getString("item.material");
                    if (matName != null) {
                        Material mat = Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
                        if (mat != null) {
                            item = new ItemStack(mat);
                            int cmd = lSec.getInt("item.custom-model-data", 0);
                            if (cmd > 0) {
                                ItemMeta meta = item.getItemMeta();
                                if (meta != null) {
                                    meta.setCustomModelData(cmd);
                                    item.setItemMeta(meta);
                                }
                            }
                        }
                    }

                    section.add(lKey, new MenuLayout(secId + ":" + lKey, name, commands, stop,
                            x, y, z, tp, tpBack, tpLoc, stopCmds, lPerm, item));
                }
            }
            sectionManager.add(secId.toLowerCase(Locale.ROOT), section);
        }
    }

    /** Displays the given section to the player. */
    public boolean showMenu(Player player, String id) {
        Section section = sectionManager.get(id.toLowerCase(Locale.ROOT));
        if (section == null) return false;
        if (!section.permission.isEmpty() && !player.hasPermission(section.permission) && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "You lack permission for this menu.");
            return false;
        }

        hideMenu(player);

        Location cam = new Location(Bukkit.getWorld(section.world), section.cameraX,
                section.cameraY, section.cameraZ, section.yaw, section.pitch);
        if (cam.getWorld() != null) {
            player.teleport(cam);
        }
        Location eye = player.getEyeLocation();

        Vector forward = eye.getDirection().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = new Vector(0, 1, 0);
        Location base = eye.clone().add(forward.clone().multiply(section.distance));

        List<Entity> displays = new ArrayList<>();
        List<MenuLayout> layouts = new ArrayList<>(section.layouts.values());
        for (MenuLayout layout : layouts) {
            Location loc = base.clone()
                    .add(right.clone().multiply(layout.x()))
                    .add(up.clone().multiply(layout.y()))
                    .add(forward.clone().multiply(layout.z()));

            Entity disp;
            if (layout.item() != null) {
                disp = spawnItemDisplay(loc, layout.item().clone());
            } else {
                String text = PlaceholderAPI.setPlaceholders(player, layout.name());
                disp = spawnTextDisplay(loc, text);
            }
            displays.add(disp);
        }

        ItemDisplay cursor = spawnItemDisplay(base.clone(), new ItemStack(Material.ARROW));
        ActiveMenu active = new ActiveMenu(player, section.distance, layouts, displays, cursor);
        active.start();
        activeMenus.put(player.getUniqueId(), active);
        return true;
    }

    /** Removes any active menu for the player. */
    public void hideMenu(Player player) {
        ActiveMenu active = activeMenus.remove(player.getUniqueId());
        if (active != null) active.stop();
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

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (activeMenus.containsKey(e.getPlayer().getUniqueId())) {
            hideMenu(e.getPlayer());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        ActiveMenu active = activeMenus.get(e.getPlayer().getUniqueId());
        if (active != null && !e.getMessage().toLowerCase(Locale.ROOT).startsWith("/cursormenu")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Commands are disabled while using this menu.");
        }
    }

    /* === internal ======================================================= */
    private class ActiveMenu {
        private final Player player;
        private final double distance;
        private final List<MenuLayout> layouts;
        private final List<Entity> displays;
        private final ItemDisplay cursor;
        private int selected = -1;
        private int taskId;

        ActiveMenu(Player player, double distance, List<MenuLayout> layouts, List<Entity> displays, ItemDisplay cursor) {
            this.player = player;
            this.distance = distance;
            this.layouts = layouts;
            this.displays = displays;
            this.cursor = cursor;
        }

        void start() {
            taskId = new BukkitRunnable() {
                @Override public void run() {
                    Location eye = player.getEyeLocation();
                    Vector forward = eye.getDirection().normalize();
                    Location cursorLoc = eye.add(forward.multiply(distance));
                    cursor.teleport(cursorLoc);

                    int newSel = -1;
                    double best = Double.MAX_VALUE;
                    for (int i = 0; i < displays.size(); i++) {
                        Entity disp = displays.get(i);
                        double dist = disp.getLocation().distanceSquared(cursorLoc);
                        if (dist < 0.25 && dist < best) {
                            best = dist;
                            newSel = i;
                        }
                    }
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
            if (cursor != null && !cursor.isDead()) cursor.remove();
        }

        void handleClick() {
            if (selected < 0 || selected >= layouts.size()) return;
            layouts.get(selected).execute(player, ScreenMenuManager.this);
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
