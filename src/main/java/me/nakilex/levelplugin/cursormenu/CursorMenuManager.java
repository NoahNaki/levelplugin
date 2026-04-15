package me.nakilex.levelplugin.cursormenu;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;

public class CursorMenuManager implements Listener {
    private final Main plugin;

    private final Map<String, MenuSection> sections = new LinkedHashMap<>();
    private final Map<String, ItemPreset> itemPresets = new LinkedHashMap<>();
    private final Map<UUID, MenuSession> activeSessions = new HashMap<>();
    private final Map<UUID, PreviewSession> activePreviews = new HashMap<>();

    private CursorConfig config = CursorConfig.defaults();
    private BukkitTask tickTask;

    public CursorMenuManager(Main plugin) {
        this.plugin = plugin;
        ensureDefaultFiles();
        reload();
    }

    public void reload() {
        stopAllMenus();
        stopAllPreviews();
        loadConfig();
        loadItems();
        loadMenus();
        restartTickTask();
    }

    public Set<String> getMenuKeys() { return Collections.unmodifiableSet(sections.keySet()); }
    public Set<String> getItemPresetKeys() { return Collections.unmodifiableSet(itemPresets.keySet()); }

    public boolean runMenu(Player player, String menuKey) {
        MenuSection section = sections.get(menuKey.toLowerCase(Locale.ROOT));
        if (section == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    ChatColor.RED + "Unknown cursor menu '" + menuKey + "'.");
            return false;
        }
        stopMenu(player, false);

        Location camera = section.camera.clone();
        MenuSession session = new MenuSession(menuKey.toLowerCase(Locale.ROOT),
                player.getLocation().clone(), player.getGameMode(), camera);

        if (player.teleport(camera)) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        session.cursorAnchor = spawnCursorAnchor(camera);
        session.cursorDisplay = spawnCursorDisplay(camera);
        for (MenuButton button : section.buttons) {
            TextDisplay display = spawnButtonDisplay(section, button);
            if (display != null) {
                session.buttons.add(new ButtonState(button, display));
            }
        }

        activeSessions.put(player.getUniqueId(), session);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                ChatColor.GREEN + "Opened cursor menu: " + ChatColor.WHITE + menuKey);
        return true;
    }

    public boolean stopMenu(Player player, boolean teleportBack) {
        MenuSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return false;
        cleanupSession(session);
        if (teleportBack && session.returnLocation != null) {
            player.teleport(session.returnLocation);
            player.setGameMode(session.originalGameMode);
        }
        return true;
    }

    public boolean showItemPreview(Player player, String itemId) {
        ItemPreset preset = itemPresets.get(itemId.toLowerCase(Locale.ROOT));
        if (preset == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    ChatColor.RED + "Unknown cursor item id '" + itemId + "'.");
            return false;
        }
        hideItemPreview(player);
        ItemDisplay display = spawnItemPreview(player, preset);
        if (display == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    ChatColor.RED + "Failed to create item preview.");
            return false;
        }
        activePreviews.put(player.getUniqueId(), new PreviewSession(preset, display));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                ChatColor.GREEN + "Showing cursor item: " + ChatColor.WHITE + itemId);
        return true;
    }

    public boolean hideItemPreview(Player player) {
        PreviewSession session = activePreviews.remove(player.getUniqueId());
        if (session == null) return false;
        if (session.display != null && !session.display.isDead()) {
            session.display.remove();
        }
        return true;
    }

    public void stopAllMenus() {
        for (MenuSession session : new ArrayList<>(activeSessions.values())) cleanupSession(session);
        activeSessions.clear();
    }

    public void stopAllPreviews() {
        for (PreviewSession session : new ArrayList<>(activePreviews.values())) {
            if (session.display != null && !session.display.isDead()) session.display.remove();
        }
        activePreviews.clear();
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        stopAllMenus();
        stopAllPreviews();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        MenuSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;
        event.setCancelled(true);

        ButtonState hovered = getHoveredButton(session);
        if (hovered == null) return;
        for (String raw : hovered.button.commands) dispatchButtonCommand(player, raw);
        if (hovered.button.closeOnClick) stopMenu(player, true);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { stopMenu(event.getPlayer(), false); hideItemPreview(event.getPlayer()); }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            stopMenu(event.getPlayer(), false);
        }
    }

    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { stopMenu(event.getPlayer(), false); hideItemPreview(event.getPlayer()); }

    private void cleanupSession(MenuSession session) {
        if (session.cursorAnchor != null && !session.cursorAnchor.isDead()) session.cursorAnchor.remove();
        if (session.cursorDisplay != null && !session.cursorDisplay.isDead()) session.cursorDisplay.remove();
        for (ButtonState button : session.buttons) {
            if (button.display != null && !button.display.isDead()) button.display.remove();
        }
    }

    private void dispatchButtonCommand(Player player, String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) return;
        String resolved = applyPlaceholders(player, rawCommand.trim());
        if (resolved.startsWith("[console]")) {
            String cmd = resolved.substring("[console]".length()).trim();
            if (!cmd.isEmpty()) plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            return;
        }
        if (resolved.startsWith("[player]")) {
            String cmd = resolved.substring("[player]".length()).trim();
            if (!cmd.isEmpty()) player.performCommand(cmd);
            return;
        }
        player.performCommand(resolved.startsWith("/") ? resolved.substring(1) : resolved);
    }

    private String applyPlaceholders(Player player, String input) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, input);
        }
        return input;
    }

    private ButtonState getHoveredButton(MenuSession session) {
        if (session.cursorAnchor == null || session.cursorAnchor.isDead()) return null;
        Location cursor = session.cursorAnchor.getLocation();
        double bestDistance = 0.8;
        ButtonState best = null;
        for (ButtonState button : session.buttons) {
            if (button.display == null || button.display.isDead()) continue;
            double distance = button.display.getLocation().distance(cursor);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = button;
            }
        }
        return best;
    }

    private void tick() {
        tickMenus();
        tickItemPreviews();
    }

    private void tickMenus() {
        Iterator<Map.Entry<UUID, MenuSession>> iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MenuSession> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            MenuSession session = entry.getValue();
            if (player == null || !player.isOnline()) {
                cleanupSession(session);
                iterator.remove();
                continue;
            }
            MenuSection section = sections.get(session.menuKey);
            if (section == null) {
                cleanupSession(session);
                iterator.remove();
                continue;
            }

            Location cursorLocation = computeCursorLocation(player, session.camera, section.distance);
            if (session.cursorAnchor != null && !session.cursorAnchor.isDead()) session.cursorAnchor.teleport(cursorLocation);
            if (session.cursorDisplay != null && !session.cursorDisplay.isDead()) session.cursorDisplay.teleport(cursorLocation);

            ButtonState hovered = getHoveredButton(session);
            for (ButtonState button : session.buttons) {
                if (button.display == null || button.display.isDead()) continue;
                button.display.setGlowing(button == hovered);
            }
        }
    }

    private void tickItemPreviews() {
        Iterator<Map.Entry<UUID, PreviewSession>> iterator = activePreviews.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PreviewSession> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            PreviewSession session = entry.getValue();
            if (player == null || !player.isOnline() || session.display == null || session.display.isDead()) {
                if (session.display != null && !session.display.isDead()) session.display.remove();
                iterator.remove();
                continue;
            }
            Location target = player.getEyeLocation().add(player.getLocation().getDirection().multiply(session.preset.distance));
            target.add(session.preset.offsetX, session.preset.offsetY, session.preset.offsetZ);
            target.setYaw(player.getLocation().getYaw());
            target.setPitch(player.getLocation().getPitch());
            session.display.teleport(target);

            if (session.preset.rotateSpeed > 0.0f) {
                session.rotation += session.preset.rotateSpeed;
                session.display.setTransformation(new Transformation(
                        new Vector3f(),
                        new AxisAngle4f((float) Math.toRadians(session.rotation), 0f, 1f, 0f),
                        new Vector3f(session.preset.scale, session.preset.scale, session.preset.scale),
                        new AxisAngle4f()));
            }
        }
    }

    private Location computeCursorLocation(Player player, Location camera, double distance) {
        float dyaw = normalizeYaw(player.getLocation().getYaw() - camera.getYaw());
        float dpitch = player.getLocation().getPitch() - camera.getPitch();
        double x = clamp((-dyaw / config.maxYaw) * config.maxX, -config.maxX, config.maxX);
        double y = clamp((-dpitch / config.maxPitch) * config.maxY, -config.maxY, config.maxY);

        Vector forward = camera.getDirection().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = new Vector(0, 1, 0);
        return camera.clone().add(forward.multiply(distance)).add(right.multiply(x)).add(up.multiply(y));
    }

    private ArmorStand spawnCursorAnchor(Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setMarker(true);
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSmall(true);
        });
    }

    private ItemDisplay spawnCursorDisplay(Location location) {
        Material material = Material.matchMaterial(config.cursorMaterial);
        if (material == null) material = Material.NETHER_STAR;
        final Material cursorMaterial = material;
        return location.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(cursorMaterial));
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformation(new Transformation(
                    new Vector3f((float) config.cursorOffsetX, (float) config.cursorOffsetY, 0f),
                    new AxisAngle4f(),
                    new Vector3f((float) config.cursorScale, (float) config.cursorScale, (float) config.cursorScale),
                    new AxisAngle4f()));
        });
    }

    private TextDisplay spawnButtonDisplay(MenuSection section, MenuButton button) {
        Location loc = resolveScreenPosition(section.camera, section.distance, button.x, button.y, button.z);
        return loc.getWorld().spawn(loc, TextDisplay.class, display -> {
            display.text(net.kyori.adventure.text.Component.text(colorize(button.text)));
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setDefaultBackground(false);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f((float) button.scale, (float) button.scale, (float) button.scale),
                    new AxisAngle4f()));
        });
    }

    private ItemDisplay spawnItemPreview(Player player, ItemPreset preset) {
        Material material = Material.matchMaterial(preset.material);
        if (material == null) return null;
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(preset.distance));
        return loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setBillboard(Display.Billboard.FIXED);
            display.setInterpolationDuration(2);
            display.setTransformation(new Transformation(
                    new Vector3f((float) preset.offsetX, (float) preset.offsetY, (float) preset.offsetZ),
                    new AxisAngle4f(),
                    new Vector3f(preset.scale, preset.scale, preset.scale),
                    new AxisAngle4f()));
        });
    }

    private Location resolveScreenPosition(Location camera, double distance, double x, double y, double z) {
        Vector forward = camera.getDirection().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = new Vector(0, 1, 0);
        return camera.clone().add(forward.multiply(distance + z)).add(right.multiply(x)).add(up.multiply(y));
    }

    private void ensureDefaultFiles() {
        saveIfMissing("cursormenu.yml");
        saveIfMissing("cursormenu-items.yml");
        File menuFolder = new File(plugin.getDataFolder(), "cursormenu/menu");
        if (!menuFolder.exists()) menuFolder.mkdirs();
        saveIfMissing("cursormenu/menu/example.yml");
    }

    private void saveIfMissing(String path) {
        File target = new File(plugin.getDataFolder(), path);
        if (!target.exists()) plugin.saveResource(path, false);
    }

    private void loadConfig() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "cursormenu.yml"));
        config = new CursorConfig(
                yaml.getString("cursor.material", "NETHER_STAR"),
                yaml.getDouble("cursor.scale", 0.35),
                yaml.getDouble("cursor.offset-x", 0.0),
                yaml.getDouble("cursor.offset-y", 0.0),
                yaml.getDouble("movement.max-x", 2.2),
                yaml.getDouble("movement.max-y", 1.2),
                Math.max(5.0, yaml.getDouble("movement.max-yaw", 45.0)),
                Math.max(5.0, yaml.getDouble("movement.max-pitch", 30.0))
        );
    }

    private void loadItems() {
        itemPresets.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "cursormenu-items.yml"));
        ConfigurationSection section = yaml.getConfigurationSection("items");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null) continue;
            ItemPreset preset = new ItemPreset(
                    node.getString("material", "STONE"),
                    (float) node.getDouble("scale", 1.0),
                    node.getDouble("distance", 2.0),
                    node.getDouble("offset-x", 0.0),
                    node.getDouble("offset-y", 0.0),
                    node.getDouble("offset-z", 0.0),
                    (float) node.getDouble("rotate-speed", 0.0)
            );
            itemPresets.put(key.toLowerCase(Locale.ROOT), preset);
        }
    }

    private void loadMenus() {
        sections.clear();
        File dir = new File(plugin.getDataFolder(), "cursormenu/menu");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection menus = yaml.getConfigurationSection("menus");
            if (menus == null) continue;
            for (String key : menus.getKeys(false)) {
                ConfigurationSection node = menus.getConfigurationSection(key);
                if (node == null) continue;
                Location camera = parseLocation(node.getConfigurationSection("camera"));
                if (camera == null) continue;
                double distance = node.getDouble("distance", 4.0);
                List<MenuButton> buttons = new ArrayList<>();
                ConfigurationSection buttonSection = node.getConfigurationSection("buttons");
                if (buttonSection != null) {
                    for (String bKey : buttonSection.getKeys(false)) {
                        ConfigurationSection bNode = buttonSection.getConfigurationSection(bKey);
                        if (bNode == null) continue;
                        buttons.add(new MenuButton(
                                bNode.getString("text", bKey),
                                bNode.getDouble("x", 0.0),
                                bNode.getDouble("y", 0.0),
                                bNode.getDouble("z", 0.0),
                                bNode.getDouble("scale", 1.0),
                                bNode.getBoolean("close-on-click", true),
                                bNode.getStringList("commands")
                        ));
                    }
                }
                sections.put(key.toLowerCase(Locale.ROOT), new MenuSection(camera, distance, buttons));
            }
        }
    }

    private Location parseLocation(ConfigurationSection section) {
        if (section == null) return null;
        World world = Bukkit.getWorld(section.getString("world", "world"));
        if (world == null) return null;
        return new Location(world,
                section.getDouble("x", 0.0),
                section.getDouble("y", 100.0),
                section.getDouble("z", 0.0),
                (float) section.getDouble("yaw", 0.0),
                (float) section.getDouble("pitch", 0.0));
    }

    private void restartTickTask() {
        if (tickTask != null) tickTask.cancel();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private static String colorize(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }

    private static float normalizeYaw(float yaw) {
        float value = yaw % 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private record CursorConfig(String cursorMaterial, double cursorScale, double cursorOffsetX, double cursorOffsetY,
                                double maxX, double maxY, double maxYaw, double maxPitch) {
        static CursorConfig defaults() { return new CursorConfig("NETHER_STAR", 0.35, 0.0, 0.0, 2.2, 1.2, 45.0, 30.0); }
    }

    private record MenuSection(Location camera, double distance, List<MenuButton> buttons) {}

    private record MenuButton(String text, double x, double y, double z, double scale, boolean closeOnClick, List<String> commands) {}

    private record ItemPreset(String material, float scale, double distance, double offsetX, double offsetY, double offsetZ, float rotateSpeed) {}

    private static final class MenuSession {
        private final String menuKey;
        private final Location returnLocation;
        private final GameMode originalGameMode;
        private final Location camera;
        private ArmorStand cursorAnchor;
        private ItemDisplay cursorDisplay;
        private final List<ButtonState> buttons = new ArrayList<>();

        private MenuSession(String menuKey, Location returnLocation, GameMode originalGameMode, Location camera) {
            this.menuKey = menuKey;
            this.returnLocation = returnLocation;
            this.originalGameMode = originalGameMode;
            this.camera = camera;
        }
    }

    private static final class ButtonState {
        private final MenuButton button;
        private final TextDisplay display;

        private ButtonState(MenuButton button, TextDisplay display) {
            this.button = button;
            this.display = display;
        }
    }

    private static final class PreviewSession {
        private final ItemPreset preset;
        private final ItemDisplay display;
        private float rotation;

        private PreviewSession(ItemPreset preset, ItemDisplay display) {
            this.preset = preset;
            this.display = display;
        }
    }
}
