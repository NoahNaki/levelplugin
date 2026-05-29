package me.nakilex.levelplugin.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final int RETURN_TO_IDLE_TICKS = 20;

    private final Main plugin;
    private final NamespacedKey debugHandleKey;
    private final Map<UUID, WieldSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> randomSwingTasks = new ConcurrentHashMap<>();
    private final Set<UUID> inputDebugPlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask tickTask;
    private long tickCounter;

    private Material defaultMaterial = Material.DIAMOND_SWORD;
    private String defaultNexoModelId;
    private WieldStylePreset activePreset = WieldStylePreset.OVERHEAD_SLASH;
    private WieldStyleConfig config = activePreset.config();

    public WieldStyleDebugManager(Main plugin) {
        this.plugin = plugin;
        this.debugHandleKey = new NamespacedKey(plugin, "debug_wield_handle");
        restartTickTask();
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        clearAll();
    }

    private void restartTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tickCounter++, 1L, 1L);
    }

    private long currentTick() {
        return tickCounter;
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
        inputDebugPlayers.remove(playerId);
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

    public boolean isInputDebugEnabled(Player player) {
        return inputDebugPlayers.contains(player.getUniqueId());
    }

    public boolean toggleInputDebug(Player player) {
        UUID playerId = player.getUniqueId();
        if (!inputDebugPlayers.add(playerId)) {
            inputDebugPlayers.remove(playerId);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Wield input debugging disabled.");
            return false;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Wield input debugging enabled. Click attempts will be logged to console.");
        debugInput(player, "debug-toggle", "enabled");
        return true;
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
                WieldStyleConfig randomConfig = WieldStyleConfig.defaultConfig();
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

    public WieldStyleConfig config() {
        return config.copy();
    }

    public void applyConfig(WieldStyleConfig config) {
        this.config = config.copy();
        this.activePreset = null;
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
        this.config = safePreset.config();
        for (WieldSession session : sessions.values()) {
            Player player = plugin.getServer().getPlayer(session.playerId);
            if (player != null && player.isOnline() && !session.swinging) {
                moveDisplay(session, idlePose(player, this.config));
            }
        }
        return safePreset;
    }

    public WieldStylePreset applyPreset(String name) {
        WieldStylePreset preset = WieldStylePreset.fromString(name);
        return preset == null ? null : applyPreset(preset);
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
                + ", preset=" + (activePreset == null ? "custom" : activePreset.id())
                + ", " + config.describe();
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        triggerSwing(event.getPlayer(), "animation", false);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        triggerSwing(event.getPlayer(), "interact:" + action.name().toLowerCase(Locale.ROOT), event.isCancelled());
    }

    private void triggerSwing(Player player, String source, boolean eventCancelled) {
        WieldSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            debugInput(player, source, "ignored=no-active-session, cancelled=" + eventCancelled);
            return;
        }
        long now = currentTick();
        if (session.lastInputTick == now) {
            debugInput(player, source, "ignored=duplicate-same-tick, cancelled=" + eventCancelled);
            return;
        }
        if (now < session.nextAllowedSwingTick) {
            debugInput(player, source, "ignored=cooldown, remainingTicks=" + (session.nextAllowedSwingTick - now)
                    + ", cancelled=" + eventCancelled);
            return;
        }
        session.lastInputTick = now;
        WieldStylePreset preset = nextComboPreset(session);
        debugInput(player, source, "accepted, preset=" + preset.id() + ", cancelled=" + eventCancelled);
        startSwing(player, session, false, preset.config(), source);
    }

    private WieldStylePreset nextComboPreset(WieldSession session) {
        WieldStylePreset[] combo = {
                WieldStylePreset.BASIC_ATTACK,
                WieldStylePreset.BASIC_ATTACK_TWO,
                WieldStylePreset.OVERHEAD_SLASH,
                WieldStylePreset.COOL_SWEEP
        };
        WieldStylePreset preset = combo[session.comboIndex % combo.length];
        session.comboIndex = (session.comboIndex + 1) % combo.length;
        return preset;
    }

    private void debugInput(Player player, String source, String outcome) {
        if (!isInputDebugEnabled(player)) {
            return;
        }
        WieldSession session = sessions.get(player.getUniqueId());
        long now = currentTick();
        String state = session == null
                ? "session=none"
                : "session=active"
                + ", swinging=" + session.swinging
                + ", swingTask=" + (session.swingTask != null)
                + ", displayValid=" + (session.display != null && !session.display.isDead())
                + ", serverTick=" + now
                + ", lastInputTick=" + session.lastInputTick
                + ", nextAllowedSwingTick=" + session.nextAllowedSwingTick
                + ", cooldownRemaining=" + Math.max(0L, session.nextAllowedSwingTick - now)
                + ", comboIndex=" + session.comboIndex;
        plugin.getLogger().info(() -> "[WieldStyleDebugInput] player=" + player.getName()
                + ", source=" + source
                + ", " + outcome
                + ", " + state);
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
        startSwing(player, session, force, config.copy(), force ? "force" : "manual");
    }

    private void startSwing(Player player, WieldSession session, boolean force, WieldStyleConfig activeConfig, String source) {
        long now = currentTick();
        if (!force && now < session.nextAllowedSwingTick) {
            debugInput(player, source, "start-blocked=cooldown, remainingTicks=" + (session.nextAllowedSwingTick - now));
            return;
        }
        boolean cancelledPreviousTask = session.swingTask != null;
        if (session.swingTask != null) {
            session.swingTask.cancel();
        }
        session.swinging = true;
        session.nextAllowedSwingTick = now + activeConfig.cooldownTicks();
        session.display.setInterpolationDuration(Math.max(0, activeConfig.interpolationDuration()));
        debugInput(player, source, "started, cancelledPreviousTask=" + cancelledPreviousTask
                + ", cooldownTicks=" + activeConfig.cooldownTicks()
                + ", swingTicks=" + activeConfig.swingTicks());
        session.swingTask = new BukkitRunnable() {
            private int tick = 0;
            private Pose returnStartPose;

            @Override
            public void run() {
                Player online = plugin.getServer().getPlayer(session.playerId);
                if (online == null || !online.isOnline()) {
                    disable(session.playerId);
                    cancel();
                    return;
                }
                int swingTotal = Math.max(2, activeConfig.swingTicks());
                if (tick < swingTotal) {
                    double progress = Math.min(1.0, tick / (double) (swingTotal - 1));
                    Pose pose = swingPose(online, easeOut(progress), activeConfig);
                    moveDisplay(session, pose);
                    returnStartPose = pose;
                    tick++;
                    return;
                }

                int returnTick = tick - swingTotal;
                int returnTotal = Math.max(2, RETURN_TO_IDLE_TICKS);
                double returnProgress = Math.min(1.0, returnTick / (double) (returnTotal - 1));
                Pose idle = idlePose(online, config);
                moveDisplay(session, interpolatePose(returnStartPose == null ? idle : returnStartPose,
                        idle, easeOut(returnProgress)));
                tick++;
                if (returnTick + 1 >= returnTotal) {
                    session.swinging = false;
                    session.swingTask = null;
                    moveDisplay(session, idlePose(online, config));
                    debugInput(online, source, "completed-return-to-idle");
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

    private Pose interpolatePose(Pose from, Pose to, double progress) {
        Location location = from.location().clone();
        location.setX(lerp(from.location().getX(), to.location().getX(), progress));
        location.setY(lerp(from.location().getY(), to.location().getY(), progress));
        location.setZ(lerp(from.location().getZ(), to.location().getZ(), progress));
        location.setYaw((float) lerpAngle(from.location().getYaw(), to.location().getYaw(), progress));
        location.setPitch((float) lerp(from.location().getPitch(), to.location().getPitch(), progress));
        return new Pose(location,
                lerp(from.leftRotationDegrees(), to.leftRotationDegrees(), progress),
                lerp(from.rightRotationDegrees(), to.rightRotationDegrees(), progress),
                (float) lerp(from.scale(), to.scale(), progress));
    }

    private double lerp(double from, double to, double progress) {
        return from + ((to - from) * progress);
    }

    private double lerpAngle(double from, double to, double progress) {
        double delta = ((to - from + 540.0) % 360.0) - 180.0;
        return from + (delta * progress);
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
        private int comboIndex;
        private long nextAllowedSwingTick;
        private long lastInputTick = -1L;

        private WieldSession(UUID playerId, ItemDisplay display, ItemStack visualItem) {
            this.playerId = playerId;
            this.display = display;
            this.visualItem = visualItem;
        }
    }


    public enum WieldStylePreset {
        OVERHEAD_SLASH("overhead_slash", "Overhead Slash",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        106.78813936115861, -186.6909558808558, 0.15980664918631562,
                        0.9877825861829582, 0.036862218691507104, 1.2711515689082424,
                        0.4006004088814077, -41.38846296298364, 48.91458472939911,
                        -53.61977811440525, 81.38337452682886, -66.83012338117723,
                        132.66759822330394, 99.1515139506437, -30.49929654416185)),
        BEYBLADE_SWIRL("beyblade_swirl", "Beyblade Swirl",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        149.69386305872723, -171.96775775595756, 1.1393345553474903,
                        0.8506799627353459, -0.36832261979426884, 1.2973677239379766,
                        0.4424320994105332, 5.7231808755518045, 185.31289826600295,
                        -35.89715186429072, 190.9657973506081, -97.00310424138125,
                        -423.9848043587916, -1.5009839314438977, 372.082944972278)),
        COOL_SWEEP("cool_sweep", "Cool Sweep",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        55.01033770127748, -298.06188822354045, 0.9024002861771805,
                        0.6039956910541824, 0.06503697955364385, 1.1567669547934205,
                        0.4147886779934885, 124.46899839538139, -83.92384491011171,
                        -22.865147709861816, 210.69150810349333, 125.24722177342,
                        -177.45824996471913, 60.07567107365088, 161.6478692857686)),
        COOL_SWIRL("cool_swirl", "Cool Swirl",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        -123.2797945004859, -326.88824337384665, -1.2929481246029473,
                        1.0850175545906546, -0.07845679609481981, 1.3829925186475112,
                        0.2843594103411882, 100.41132146621277, -139.45950615941555,
                        -19.099799951032765, 202.27736608788462, 171.37924907810157,
                        -433.8053333171781, 134.99582825105335, -268.5415701430878)),
        PARRY_TYPE("parry_type", "Parry Type",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        -157.70547416035907, 391.3088240022212, 0.8931485413847031,
                        -0.8684853539984333, 0.2650746358058361, 1.2321292243985533,
                        0.36050623086962186, 23.51300724555844, -155.64026598650247,
                        -97.81968552087531, -198.02284776870925, -36.72350683795469,
                        -497.9896943862577, -37.62978480829449, -447.6378094901502)),
        BASIC_ATTACK("basic_attack", "Basic Attack",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        -7.597476350519571, -396.6678009578013, -1.002474704638901,
                        -0.10955255504269501, -0.49299159608471477, 1.1246103768763687,
                        0.43023693341752944, -34.03258465878355, 105.8781397276856,
                        -26.468320533805482, 111.74781919098103, -77.1466018303899,
                        258.9278522064494, 74.59162027705167, -280.6300726398954)),
        BASIC_ATTACK_TWO("basic_attack_2", "Basic Attack 2",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        -85.68423014032777, -202.81888790329361, 1.2802284526314247,
                        -0.5111532543229289, -0.07972367059891083, 1.179850083916887,
                        0.5010660807049536, 4.879281831669914, 165.93664880282688,
                        71.18506108827745, 72.11240325338264, 155.02853975191795,
                        -406.01305010724536, 187.880697581937, 52.44780271370257)),
        HORIZONTAL_SLASH("horizontal_slash", "Horizontal Slash",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        1.12, 0.34, -0.28, -18.0, -12.0, -35.0, 60.0,
                        107.31430466731575, 256.21725634659435, -1.3957143565347,
                        0.2494978117191653, -0.45385151312779304, 1.3654522266504672,
                        0.47968886574028236, -257.5833908646365, -103.99355332936875,
                        -180.0, 4.523226173149709, -11.787335414423495,
                        373.94624250109575, 111.0100161511391, -282.823982305912));

        private final String id;
        private final String displayName;
        private final WieldStyleConfig config;

        WieldStylePreset(String id, String displayName, WieldStyleConfig config) {
            this.id = id;
            this.displayName = displayName;
            this.config = config;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public WieldStyleConfig config() {
            return config.copy();
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
            return WieldStylePreset.OVERHEAD_SLASH.config();
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
            WieldStyleConfig baseline = WieldStylePreset.HORIZONTAL_SLASH.config();
            setSwingAngleStart(jitter(random, baseline.swingAngleStart, 18.0));
            setSwingAngleSweep(jitter(random, baseline.swingAngleSweep, 35.0));
            setSwingSideRadius(jitter(random, baseline.swingSideRadius, 0.18));
            setSwingUpRadius(jitter(random, baseline.swingUpRadius, 0.14));
            setSwingUpOffset(jitter(random, baseline.swingUpOffset, 0.12));
            setSwingForwardBase(jitter(random, baseline.swingForwardBase, 0.08));
            setSwingForwardPeak(jitter(random, baseline.swingForwardPeak, 0.06));
            setSwingYawStart(jitter(random, baseline.swingYawStart, 24.0));
            setSwingYawSweep(jitter(random, baseline.swingYawSweep, 24.0));
            setSwingPitchStart(jitter(random, baseline.swingPitchStart, 8.0));
            setSwingPitchSweep(jitter(random, baseline.swingPitchSweep, 12.0));
            setSwingLeftRotationStart(jitter(random, baseline.swingLeftRotationStart, 28.0));
            setSwingLeftRotationSweep(jitter(random, baseline.swingLeftRotationSweep, 35.0));
            setSwingRightRotationStart(jitter(random, baseline.swingRightRotationStart, 28.0));
            setSwingRightRotationSweep(jitter(random, baseline.swingRightRotationSweep, 35.0));
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
