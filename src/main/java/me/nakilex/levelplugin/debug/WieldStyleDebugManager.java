package me.nakilex.levelplugin.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Developer-only prototype for custom wield/attack visuals.
 *
 * <p>The manager intentionally keeps the visual weapon separate from combat
 * logic: a lightweight {@link ItemDisplay} follows the player's view while
 * enabled, then left-clicking animates it through a slash arc. All pose values
 * are held in a runtime config so the debug GUI can tune the swing in game.</p>
 */
public class WieldStyleDebugManager implements Listener {
    private static final long RANDOM_SWING_INTERVAL_TICKS = 40L;

    private final Main plugin;
    private final NamespacedKey debugHandleKey;
    private final Map<UUID, WieldSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> randomSwingTasks = new ConcurrentHashMap<>();

    private Material defaultMaterial = Material.DIAMOND_SWORD;
    private String defaultNexoModelId;
    private WieldStylePreset activePreset = WieldStylePreset.OVERHEAD_SLASH;
    private WieldStyleConfig config = activePreset.config();

    public WieldStyleDebugManager(Main plugin) {
        this.plugin = plugin;
        this.debugHandleKey = new NamespacedKey(plugin, "debug_wield_handle");
    }

    public boolean isEnabled(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void toggle(Player player) {
        if (isEnabled(player)) {
            disable(player, "Custom wield preview disabled.");
            return;
        }
        enable(player, createVisualItem(player));
    }

    public void enable(Player player, ItemStack visualItem) {
        disable(player, null);
        ItemStack item = sanitizeVisualItem(visualItem);
        ItemDisplay display = spawnDisplay(player, item);
        WieldSession session = new WieldSession(player.getUniqueId(), display, item);
        session.followTask = new BukkitRunnable() {
            @Override
            public void run() {
                Player online = plugin.getServer().getPlayer(session.playerId);
                if (online == null || !online.isOnline()) {
                    disable(session.playerId);
                    return;
                }
                if (!session.swinging) {
                    moveDisplay(session, idlePose(online, config));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        sessions.put(player.getUniqueId(), session);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Custom wield preview enabled. Left click to test the slash animation.");
    }

    public void disable(Player player, String message) {
        disable(player.getUniqueId());
        if (message != null && !message.isBlank()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, message);
        }
    }

    public void disable(UUID playerId) {
        stopRandomTesting(playerId);
        WieldSession session = sessions.remove(playerId);
        if (session == null) {
            return;
        }
        if (session.followTask != null) {
            session.followTask.cancel();
        }
        if (session.swingTask != null) {
            session.swingTask.cancel();
        }
        if (session.display != null && !session.display.isDead()) {
            session.display.remove();
        }
    }

    public void playOnce(Player player) {
        WieldSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            enable(player, createVisualItem(player));
            session = sessions.get(player.getUniqueId());
        }
        if (session != null) {
            startSwing(player, session, true);
        }
    }

    public void clearAll() {
        List<UUID> ids = new ArrayList<>(sessions.keySet());
        for (UUID id : ids) {
            disable(id);
        }
        for (UUID id : new ArrayList<>(randomSwingTasks.keySet())) {
            stopRandomTesting(id);
        }
    }

    public boolean isRandomTestingEnabled(Player player) {
        return randomSwingTasks.containsKey(player.getUniqueId());
    }

    public void toggleRandomTesting(Player player) {
        UUID playerId = player.getUniqueId();
        if (stopRandomTesting(playerId)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Random wield swing testing disabled.");
            return;
        }
        enable(player, createVisualItem(player));
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player online = plugin.getServer().getPlayer(playerId);
                if (online == null || !online.isOnline()) {
                    stopRandomTesting(playerId);
                    cancel();
                    return;
                }
                WieldStyleConfig randomConfig = activePreset.config();
                randomizeSwingConfig(randomConfig);
                applyConfig(randomConfig);
                logConfig(randomConfig);
                playOnce(online);
            }
        }.runTaskTimer(plugin, 0L, RANDOM_SWING_INTERVAL_TICKS);
        randomSwingTasks.put(playerId, task);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Random wield swing testing enabled. A randomized swing will play every 2 seconds and log to console.");
    }

    private boolean stopRandomTesting(UUID playerId) {
        BukkitTask task = randomSwingTasks.remove(playerId);
        if (task == null) {
            return false;
        }
        task.cancel();
        return true;
    }

    public ItemStack createDebugHandle() {
        ItemStack stack = new ItemStack(Material.LIGHT_GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Debug Wield Handle");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Use this while /debug wield is enabled.");
            lore.addAll(TooltipUtil.bulletList(
                    "The real weapon is rendered as an ItemDisplay.",
                    "A resource-pack invisible model can replace this later.",
                    "Vanilla first-person hand rendering is not fully packet-hideable."));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to play a slash preview", null));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(debugHandleKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void giveDebugHandle(Player player) {
        ItemStack handle = createDebugHandle();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(handle);
        if (!leftovers.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), handle);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Inventory full; dropped the debug wield handle at your feet.");
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Gave you a debug wield handle. Use a resource-pack invisible model here later.");
    }

    public void setDefaultMaterial(Material material) {
        this.defaultMaterial = material == null || material.isAir() ? Material.DIAMOND_SWORD : material;
    }

    public Material getDefaultMaterial() {
        return defaultMaterial;
    }

    public void setDefaultNexoModelId(String defaultNexoModelId) {
        this.defaultNexoModelId = defaultNexoModelId == null || defaultNexoModelId.isBlank()
                ? null
                : defaultNexoModelId.trim();
    }

    public String getDefaultNexoModelId() {
        return defaultNexoModelId;
    }

    public void setCooldownTicks(int cooldownTicks) {
        WieldStyleConfig copy = config();
        copy.setCooldownTicks(cooldownTicks);
        applyConfig(copy);
    }

    public int getCooldownTicks() {
        return config.cooldownTicks();
    }

    public void setSwingTicks(int swingTicks) {
        WieldStyleConfig copy = config();
        copy.setSwingTicks(swingTicks);
        applyConfig(copy);
    }

    public int getSwingTicks() {
        return config.swingTicks();
    }

    public WieldStyleConfig config() {
        return config.copy();
    }

    public void applyConfig(WieldStyleConfig config) {
        this.config = config.copy();
        for (WieldSession session : sessions.values()) {
            Player player = plugin.getServer().getPlayer(session.playerId);
            if (player != null && player.isOnline() && !session.swinging) {
                moveDisplay(session, idlePose(player, this.config));
            }
        }
    }

    public void resetConfig() {
        applyPreset(WieldStylePreset.OVERHEAD_SLASH);
    }

    public WieldStylePreset activePreset() {
        return activePreset;
    }

    public WieldStylePreset applyPreset(WieldStylePreset preset) {
        WieldStylePreset safePreset = preset == null ? WieldStylePreset.OVERHEAD_SLASH : preset;
        activePreset = safePreset;
        applyConfig(safePreset.config());
        return safePreset;
    }

    public WieldStylePreset applyPreset(String name) {
        WieldStylePreset preset = WieldStylePreset.fromString(name);
        return applyPreset(preset == null ? activePreset : preset);
    }

    public WieldStylePreset applyNextPreset(int direction) {
        return applyPreset(activePreset.relative(direction));
    }

    public List<String> presetSuggestions(String prefix) {
        return WieldStylePreset.suggestions(prefix);
    }

    public void logConfig(WieldStyleConfig config) {
        plugin.getLogger().info(() -> "[WieldStyleDebug] " + config.describe());
    }

    public void randomizeSwingConfig(WieldStyleConfig config) {
        config.randomizeSwingValues(ThreadLocalRandom.current());
    }

    public String describeSettings() {
        return "material=" + defaultMaterial
                + ", nexo=" + (defaultNexoModelId == null ? "none" : defaultNexoModelId)
                + ", preset=" + activePreset.id()
                + ", " + config.describe();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        triggerSwing(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        triggerSwing(event.getPlayer());
    }

    private void triggerSwing(Player player) {
        WieldSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        long now = player.getWorld().getFullTime();
        if (session.lastInputTick == now) {
            return;
        }
        session.lastInputTick = now;
        startSwing(player, session, false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        disable(event.getPlayer().getUniqueId());
    }

    private ItemStack createVisualItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && !hand.getType().isAir() && !isDebugHandle(hand)) {
            return hand.clone();
        }
        ItemStack fallback = new ItemStack(defaultMaterial);
        if (defaultNexoModelId != null) {
            ItemUtil.applyNexoModel(fallback, defaultNexoModelId);
        }
        return fallback;
    }

    private boolean isDebugHandle(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(debugHandleKey, PersistentDataType.BYTE);
    }

    private ItemStack sanitizeVisualItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return new ItemStack(defaultMaterial);
        }
        ItemStack copy = item.clone();
        copy.setAmount(1);
        return copy;
    }

    private ItemDisplay spawnDisplay(Player player, ItemStack item) {
        Pose pose = idlePose(player, config);
        World world = player.getWorld();
        return world.spawn(pose.location(), ItemDisplay.class, display -> {
            display.setItemStack(item);
            display.setBillboard(Display.Billboard.FIXED);
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setPersistent(false);
            display.setSilent(true);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(Math.max(0, config.interpolationDuration()));
            applyTransformation(display, pose);
        });
    }

    private void startSwing(Player player, WieldSession session, boolean force) {
        long now = player.getWorld().getFullTime();
        WieldStyleConfig activeConfig = config.copy();
        if (!force && now < session.nextAllowedSwingTick) {
            long remaining = session.nextAllowedSwingTick - now;
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Wield preview cooling down for " + remaining + " tick(s).");
            return;
        }
        if (session.swingTask != null) {
            session.swingTask.cancel();
        }
        session.swinging = true;
        session.nextAllowedSwingTick = now + activeConfig.cooldownTicks();
        session.display.setInterpolationDuration(Math.max(0, activeConfig.interpolationDuration()));
        session.swingTask = new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                Player online = plugin.getServer().getPlayer(session.playerId);
                if (online == null || !online.isOnline()) {
                    disable(session.playerId);
                    cancel();
                    return;
                }
                int total = Math.max(2, activeConfig.swingTicks());
                double progress = Math.min(1.0, tick / (double) (total - 1));
                moveDisplay(session, swingPose(online, easeOut(progress), activeConfig));
                tick++;
                if (tick >= total) {
                    session.swinging = false;
                    session.swingTask = null;
                    moveDisplay(session, idlePose(online, config));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Pose idlePose(Player player, WieldStyleConfig config) {
        Basis basis = Basis.from(player);
        Location location = player.getEyeLocation().clone()
                .add(basis.forward().multiply(config.idleDistance()))
                .add(basis.right().multiply(config.idleRightOffset()))
                .add(basis.up().multiply(config.idleUpOffset()));
        location.setYaw(player.getLocation().getYaw() + (float) config.idleYawOffset());
        location.setPitch((float) config.idlePitch());
        return new Pose(location, config.idleLeftRotation(), config.idleRightRotation(), (float) config.scale());
    }

    private Pose swingPose(Player player, double progress, WieldStyleConfig config) {
        Basis basis = Basis.from(player);
        double angle = Math.toRadians(config.swingAngleStart() + (config.swingAngleSweep() * progress));
        double side = Math.cos(angle) * config.swingSideRadius();
        double up = Math.sin(angle) * config.swingUpRadius() + config.swingUpOffset();
        double forwardDistance = config.swingForwardBase() + Math.sin(progress * Math.PI) * config.swingForwardPeak();
        Location location = player.getEyeLocation().clone()
                .add(basis.forward().multiply(forwardDistance))
                .add(basis.right().multiply(side))
                .add(basis.up().multiply(up));
        location.setYaw(player.getLocation().getYaw() + (float) (config.swingYawStart() + (config.swingYawSweep() * progress)));
        location.setPitch((float) (config.swingPitchStart() + (config.swingPitchSweep() * progress)));
        return new Pose(location,
                config.swingLeftRotationStart() + (config.swingLeftRotationSweep() * progress),
                config.swingRightRotationStart() + (config.swingRightRotationSweep() * progress),
                (float) config.scale());
    }

    private void moveDisplay(WieldSession session, Pose pose) {
        if (session.display == null || session.display.isDead()) {
            return;
        }
        session.display.teleport(pose.location());
        session.display.setInterpolationDuration(Math.max(0, config.interpolationDuration()));
        applyTransformation(session.display, pose);
    }

    private void applyTransformation(ItemDisplay display, Pose pose) {
        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f((float) Math.toRadians(pose.leftRotationDegrees()), 1f, 0f, 0f),
                new Vector3f(pose.scale(), pose.scale(), pose.scale()),
                new AxisAngle4f((float) Math.toRadians(pose.rightRotationDegrees()), 0f, 1f, 0f)
        ));
    }

    private double easeOut(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return 1.0 - Math.pow(1.0 - clamped, 3.0);
    }

    public List<String> materialSuggestions(String prefix) {
        String normalized = prefix == null ? "" : prefix.toUpperCase(Locale.ROOT);
        return List.of(Material.values()).stream()
                .map(Material::name)
                .filter(name -> name.startsWith(normalized))
                .limit(30)
                .toList();
    }

    private record Pose(Location location, double leftRotationDegrees, double rightRotationDegrees, float scale) {
    }

    private record Basis(Vector forward, Vector right, Vector up) {
        private static Basis from(Player player) {
            Location flat = player.getLocation().clone();
            flat.setPitch(0f);
            Vector forward = flat.getDirection().normalize();
            Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            return new Basis(forward, right, new Vector(0, 1, 0));
        }
    }

    private static class WieldSession {
        private final UUID playerId;
        private final ItemDisplay display;
        private final ItemStack visualItem;
        private BukkitTask followTask;
        private BukkitTask swingTask;
        private boolean swinging;
        private long nextAllowedSwingTick;
        private long lastInputTick = -1L;

        private WieldSession(UUID playerId, ItemDisplay display, ItemStack visualItem) {
            this.playerId = playerId;
            this.display = display;
            this.visualItem = visualItem;
        }
    }


    public enum WieldStylePreset {
        COMPACT_DIAGONAL_SLASH("compact_diagonal_slash", "Compact Diagonal Slash",
                "Logged diagonal slash preset with a tighter side/up/forward radius.",
                new WieldStyleConfig(13, 27, 1, 0.75,
                        1.1, 0.38, -0.34, -25.0, -6.0, -4.0, 80.0,
                        145.4499543227431, -380.8752464785158, -1.15, 0.48, -1.25, 1.75, 0.85,
                        315.6740869951584, 256.9218267732739, -98.2675636003412, 208.78945940749134,
                        -257.6486823365941, 667.5503535958458, 96.63660285774324, 440.7803504474682)),
        ROGUE_DIAGONAL_OUTWARD("rogue_diagonal_outward", "Rogue Diagonal Outward",
                "Blade points away from the player and cuts down a diagonal line.",
                new WieldStyleConfig(12, 8, 1, 0.75,
                        1.05, 0.42, -0.38, -30.0, -8.0, -12.0, 70.0,
                        125.0, -155.0, 0.66, 0.58, -0.12, 1.08, 0.26,
                        -55.0, 70.0, -22.0, 42.0, -18.0, 38.0, 122.0, -58.0)),
        ROGUE_DIAGONAL_REVERSE("rogue_diagonal_reverse", "Rogue Diagonal Reverse",
                "Backhand diagonal slash with the blade still carried outward.",
                new WieldStyleConfig(12, 8, 1, 0.75,
                        1.02, -0.38, -0.36, 30.0, -8.0, -10.0, -70.0,
                        55.0, 155.0, 0.64, 0.56, -0.10, 1.08, 0.24,
                        52.0, -72.0, -18.0, 38.0, -16.0, 36.0, -122.0, 58.0)),
        HORIZONTAL_CUT("horizontal_cut", "Horizontal Cut",
                "Overhead slash motion reused from a side angle for a horizontal cut.",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        0.0, 180.0, 0.9877825861829582,
                        0.15980664918631562, 0.036862218691507104, 1.2711515689082424,
                        0.4006004088814077, -41.38846296298364, 48.91458472939911,
                        -53.61977811440525, 81.38337452682886, -66.83012338117723,
                        132.66759822330394, 99.1515139506437, -30.49929654416185)),
        OVERHEAD_SLASH("overhead_slash", "Overhead Slash",
                "High-to-low chop for heavier weapons.",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        106.78813936115861, -186.6909558808558, 0.15980664918631562,
                        0.9877825861829582, 0.036862218691507104, 1.2711515689082424,
                        0.4006004088814077, -41.38846296298364, 48.91458472939911,
                        -53.61977811440525, 81.38337452682886, -66.83012338117723,
                        132.66759822330394, 99.1515139506437, -30.49929654416185)),
        OVERHEAD_SLASH_FAST("overhead_slash_fast", "Overhead Slash Fast",
                "Faster overhead variant with a sharper downward chop.",
                new WieldStyleConfig(16, 11, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        122.69805286283032, -219.79276734727617, 0.09941444397651734,
                        0.7782469006587427, -0.02179501157733743, 1.345426382453776,
                        0.28529788167312564, -41.51715369264876, 77.53952611707317,
                        -53.1975348914812, 51.391897452131516, -43.84932730689409,
                        146.43582895968382, 88.22229430075258, 12.096337079361504)),
        OVERHEAD_SLASH_TALL("overhead_slash_tall", "Overhead Slash Tall",
                "Taller overhead variant with a larger rise before the blade drops.",
                new WieldStyleConfig(16, 16, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        111.75131052236503, -170.18217274118342, 0.19185369339891759,
                        1.2285472519736524, 0.04685218055089024, 1.2713424468915178,
                        0.40709861031553646, -28.116648465865232, 33.131035246395484,
                        -46.96594649453628, 59.823057138114095, -82.74728973163121,
                        148.25992300801448, 118.79815482426172, -41.431821027602865)),
        STAB("stab", "Forward Stab",
                "Short thrust forward instead of a broad cut.",
                new WieldStyleConfig(10, 6, 1, 0.72,
                        1.00, 0.34, -0.36, -12.0, -5.0, 0.0, 80.0,
                        0.0, 20.0, 0.08, 0.06, -0.24, 0.92, 0.95,
                        -12.0, 18.0, -6.0, 8.0, 0.0, 8.0, 92.0, -10.0));

        private final String id;
        private final String displayName;
        private final String description;
        private final WieldStyleConfig config;

        WieldStylePreset(String id, String displayName, String description, WieldStyleConfig config) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.config = config;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }

        public WieldStyleConfig config() {
            return config.copy();
        }

        public WieldStylePreset relative(int direction) {
            WieldStylePreset[] values = values();
            int next = (ordinal() + direction) % values.length;
            if (next < 0) {
                next += values.length;
            }
            return values[next];
        }

        public static WieldStylePreset fromString(String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            String normalized = input.toLowerCase(Locale.ROOT);
            for (WieldStylePreset preset : values()) {
                if (preset.id.equals(normalized) || preset.name().equalsIgnoreCase(input)) {
                    return preset;
                }
            }
            return null;
        }

        public static List<String> suggestions(String prefix) {
            String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
            return java.util.Arrays.stream(values())
                    .map(WieldStylePreset::id)
                    .filter(id -> id.startsWith(normalized))
                    .toList();
        }
    }

    public static class WieldStyleConfig {
        private int cooldownTicks;
        private int swingTicks;
        private int interpolationDuration;
        private double scale;
        private double idleDistance;
        private double idleRightOffset;
        private double idleUpOffset;
        private double idleYawOffset;
        private double idlePitch;
        private double idleLeftRotation;
        private double idleRightRotation;
        private double swingAngleStart;
        private double swingAngleSweep;
        private double swingSideRadius;
        private double swingUpRadius;
        private double swingUpOffset;
        private double swingForwardBase;
        private double swingForwardPeak;
        private double swingYawStart;
        private double swingYawSweep;
        private double swingPitchStart;
        private double swingPitchSweep;
        private double swingLeftRotationStart;
        private double swingLeftRotationSweep;
        private double swingRightRotationStart;
        private double swingRightRotationSweep;

        public WieldStyleConfig(int cooldownTicks, int swingTicks, int interpolationDuration, double scale,
                                double idleDistance, double idleRightOffset, double idleUpOffset,
                                double idleYawOffset, double idlePitch, double idleLeftRotation,
                                double idleRightRotation, double swingAngleStart, double swingAngleSweep,
                                double swingSideRadius, double swingUpRadius, double swingUpOffset,
                                double swingForwardBase, double swingForwardPeak, double swingYawStart,
                                double swingYawSweep, double swingPitchStart, double swingPitchSweep,
                                double swingLeftRotationStart, double swingLeftRotationSweep,
                                double swingRightRotationStart, double swingRightRotationSweep) {
            this.cooldownTicks = Math.max(1, cooldownTicks);
            this.swingTicks = Math.max(2, swingTicks);
            this.interpolationDuration = Math.max(0, interpolationDuration);
            this.scale = scale;
            this.idleDistance = idleDistance;
            this.idleRightOffset = idleRightOffset;
            this.idleUpOffset = idleUpOffset;
            this.idleYawOffset = idleYawOffset;
            this.idlePitch = idlePitch;
            this.idleLeftRotation = idleLeftRotation;
            this.idleRightRotation = idleRightRotation;
            this.swingAngleStart = swingAngleStart;
            this.swingAngleSweep = swingAngleSweep;
            this.swingSideRadius = swingSideRadius;
            this.swingUpRadius = swingUpRadius;
            this.swingUpOffset = swingUpOffset;
            this.swingForwardBase = swingForwardBase;
            this.swingForwardPeak = swingForwardPeak;
            this.swingYawStart = swingYawStart;
            this.swingYawSweep = swingYawSweep;
            this.swingPitchStart = swingPitchStart;
            this.swingPitchSweep = swingPitchSweep;
            this.swingLeftRotationStart = swingLeftRotationStart;
            this.swingLeftRotationSweep = swingLeftRotationSweep;
            this.swingRightRotationStart = swingRightRotationStart;
            this.swingRightRotationSweep = swingRightRotationSweep;
        }

        public static WieldStyleConfig defaultConfig() {
            return new WieldStyleConfig(12, 8, 1, 0.75,
                    1.05, 0.42, -0.38, -35.0, -8.0, 0.0, 35.0,
                    -85.0, 190.0, 0.72, 0.62, -0.16, 1.0, 0.55,
                    -115.0, 230.0, -30.0, 80.0, -35.0, 95.0, 95.0, -190.0);
        }

        public WieldStyleConfig copy() {
            return new WieldStyleConfig(cooldownTicks, swingTicks, interpolationDuration, scale,
                    idleDistance, idleRightOffset, idleUpOffset, idleYawOffset, idlePitch,
                    idleLeftRotation, idleRightRotation, swingAngleStart, swingAngleSweep,
                    swingSideRadius, swingUpRadius, swingUpOffset, swingForwardBase,
                    swingForwardPeak, swingYawStart, swingYawSweep, swingPitchStart,
                    swingPitchSweep, swingLeftRotationStart, swingLeftRotationSweep,
                    swingRightRotationStart, swingRightRotationSweep);
        }

        public void randomizeSwingValues(ThreadLocalRandom random) {
            setSwingTicks(random.nextInt(10, 21));
            setSwingAngleStart(jitter(random, swingAngleStart, 25.0));
            setSwingAngleSweep(jitter(random, swingAngleSweep, 45.0));
            setSwingSideRadius(jitter(random, swingSideRadius, 0.15));
            setSwingUpRadius(jitter(random, swingUpRadius, 0.25));
            setSwingUpOffset(jitter(random, swingUpOffset, 0.15));
            setSwingForwardBase(jitter(random, swingForwardBase, 0.20));
            setSwingForwardPeak(jitter(random, swingForwardPeak, 0.18));
            setSwingYawStart(jitter(random, swingYawStart, 20.0));
            setSwingYawSweep(jitter(random, swingYawSweep, 30.0));
            setSwingPitchStart(jitter(random, swingPitchStart, 25.0));
            setSwingPitchSweep(jitter(random, swingPitchSweep, 35.0));
            setSwingLeftRotationStart(jitter(random, swingLeftRotationStart, 30.0));
            setSwingLeftRotationSweep(jitter(random, swingLeftRotationSweep, 45.0));
            setSwingRightRotationStart(jitter(random, swingRightRotationStart, 30.0));
            setSwingRightRotationSweep(jitter(random, swingRightRotationSweep, 45.0));
        }

        private static double jitter(ThreadLocalRandom random, double base, double radius) {
            return base + random.nextDouble(-radius, radius);
        }

        public String describe() {
            return "cooldownTicks=" + cooldownTicks
                    + ", swingTicks=" + swingTicks
                    + ", interpolationDuration=" + interpolationDuration
                    + ", scale=" + scale
                    + ", idleDistance=" + idleDistance
                    + ", idleRightOffset=" + idleRightOffset
                    + ", idleUpOffset=" + idleUpOffset
                    + ", idleYawOffset=" + idleYawOffset
                    + ", idlePitch=" + idlePitch
                    + ", idleLeftRotation=" + idleLeftRotation
                    + ", idleRightRotation=" + idleRightRotation
                    + ", swingAngleStart=" + swingAngleStart
                    + ", swingAngleSweep=" + swingAngleSweep
                    + ", swingSideRadius=" + swingSideRadius
                    + ", swingUpRadius=" + swingUpRadius
                    + ", swingUpOffset=" + swingUpOffset
                    + ", swingForwardBase=" + swingForwardBase
                    + ", swingForwardPeak=" + swingForwardPeak
                    + ", swingYawStart=" + swingYawStart
                    + ", swingYawSweep=" + swingYawSweep
                    + ", swingPitchStart=" + swingPitchStart
                    + ", swingPitchSweep=" + swingPitchSweep
                    + ", swingLeftRotationStart=" + swingLeftRotationStart
                    + ", swingLeftRotationSweep=" + swingLeftRotationSweep
                    + ", swingRightRotationStart=" + swingRightRotationStart
                    + ", swingRightRotationSweep=" + swingRightRotationSweep;
        }

        public int cooldownTicks() { return cooldownTicks; }
        public void setCooldownTicks(int cooldownTicks) { this.cooldownTicks = Math.max(1, cooldownTicks); }
        public int swingTicks() { return swingTicks; }
        public void setSwingTicks(int swingTicks) { this.swingTicks = Math.max(2, swingTicks); }
        public int interpolationDuration() { return interpolationDuration; }
        public void setInterpolationDuration(int interpolationDuration) { this.interpolationDuration = Math.max(0, interpolationDuration); }
        public double scale() { return scale; }
        public void setScale(double scale) { this.scale = scale; }
        public double idleDistance() { return idleDistance; }
        public void setIdleDistance(double idleDistance) { this.idleDistance = idleDistance; }
        public double idleRightOffset() { return idleRightOffset; }
        public void setIdleRightOffset(double idleRightOffset) { this.idleRightOffset = idleRightOffset; }
        public double idleUpOffset() { return idleUpOffset; }
        public void setIdleUpOffset(double idleUpOffset) { this.idleUpOffset = idleUpOffset; }
        public double idleYawOffset() { return idleYawOffset; }
        public void setIdleYawOffset(double idleYawOffset) { this.idleYawOffset = idleYawOffset; }
        public double idlePitch() { return idlePitch; }
        public void setIdlePitch(double idlePitch) { this.idlePitch = idlePitch; }
        public double idleLeftRotation() { return idleLeftRotation; }
        public void setIdleLeftRotation(double idleLeftRotation) { this.idleLeftRotation = idleLeftRotation; }
        public double idleRightRotation() { return idleRightRotation; }
        public void setIdleRightRotation(double idleRightRotation) { this.idleRightRotation = idleRightRotation; }
        public double swingAngleStart() { return swingAngleStart; }
        public void setSwingAngleStart(double swingAngleStart) { this.swingAngleStart = swingAngleStart; }
        public double swingAngleSweep() { return swingAngleSweep; }
        public void setSwingAngleSweep(double swingAngleSweep) { this.swingAngleSweep = swingAngleSweep; }
        public double swingSideRadius() { return swingSideRadius; }
        public void setSwingSideRadius(double swingSideRadius) { this.swingSideRadius = swingSideRadius; }
        public double swingUpRadius() { return swingUpRadius; }
        public void setSwingUpRadius(double swingUpRadius) { this.swingUpRadius = swingUpRadius; }
        public double swingUpOffset() { return swingUpOffset; }
        public void setSwingUpOffset(double swingUpOffset) { this.swingUpOffset = swingUpOffset; }
        public double swingForwardBase() { return swingForwardBase; }
        public void setSwingForwardBase(double swingForwardBase) { this.swingForwardBase = swingForwardBase; }
        public double swingForwardPeak() { return swingForwardPeak; }
        public void setSwingForwardPeak(double swingForwardPeak) { this.swingForwardPeak = swingForwardPeak; }
        public double swingYawStart() { return swingYawStart; }
        public void setSwingYawStart(double swingYawStart) { this.swingYawStart = swingYawStart; }
        public double swingYawSweep() { return swingYawSweep; }
        public void setSwingYawSweep(double swingYawSweep) { this.swingYawSweep = swingYawSweep; }
        public double swingPitchStart() { return swingPitchStart; }
        public void setSwingPitchStart(double swingPitchStart) { this.swingPitchStart = swingPitchStart; }
        public double swingPitchSweep() { return swingPitchSweep; }
        public void setSwingPitchSweep(double swingPitchSweep) { this.swingPitchSweep = swingPitchSweep; }
        public double swingLeftRotationStart() { return swingLeftRotationStart; }
        public void setSwingLeftRotationStart(double swingLeftRotationStart) { this.swingLeftRotationStart = swingLeftRotationStart; }
        public double swingLeftRotationSweep() { return swingLeftRotationSweep; }
        public void setSwingLeftRotationSweep(double swingLeftRotationSweep) { this.swingLeftRotationSweep = swingLeftRotationSweep; }
        public double swingRightRotationStart() { return swingRightRotationStart; }
        public void setSwingRightRotationStart(double swingRightRotationStart) { this.swingRightRotationStart = swingRightRotationStart; }
        public double swingRightRotationSweep() { return swingRightRotationSweep; }
        public void setSwingRightRotationSweep(double swingRightRotationSweep) { this.swingRightRotationSweep = swingRightRotationSweep; }
    }
}
