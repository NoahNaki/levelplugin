package me.nakilex.levelplugin.debug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.items.utils.ItemUtil.ItemVisualModelState;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.CombatTargetUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
    private static final int BASE_RETURN_TO_IDLE_TICKS = 20;
    private static final double BASE_ANIMATION_ATTACK_SPEED = 0.8D;
    private static final double SWING_ATTACK_SPEED_WEIGHT = 0.45D;
    private static final double RETURN_ATTACK_SPEED_WEIGHT = 1.0D;
    private static final double DEFAULT_IDLE_DISTANCE = 0.76D;
    private static final double DEFAULT_IDLE_RIGHT_OFFSET = 0.62D;
    private static final int MIN_SWING_TICKS = 8;
    private static final int MAX_SWING_TICKS = 24;
    private static final int MIN_RETURN_TO_IDLE_TICKS = 6;
    private static final int MAX_RETURN_TO_IDLE_TICKS = 36;
    private static final double SWING_HIT_RADIUS = 1.15D;
    private static final double SWING_DAMAGE_BASE_FALLBACK = 1.0D;
    private static final String AUTO_HAND_MODEL_NEXO_ID = "knight_assortment-key";
    private static final double SWORD_PATH_SLASH_DAMAGE_MULTIPLIER = 0.30D;
    private static final double SWORD_PATH_SLASH_RADIUS = 0.65D;
    private static final double SWORD_PATH_SLASH_TRAVEL_DISTANCE = 6.0D;
    private static final int SWORD_PATH_SLASH_TRAVEL_TICKS = 9;

    private final Main plugin;
    private final NamespacedKey debugHandleKey;
    private final Map<UUID, WieldSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> randomSwingTasks = new ConcurrentHashMap<>();
    private final Set<UUID> inputDebugPlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask tickTask;
    private long tickCounter;

    private Material defaultMaterial = Material.DIAMOND_SWORD;
    private String defaultNexoModelId;
    private HandVisibilityMode handVisibilityMode = HandVisibilityMode.NORMAL;
    private Material handCloakMaterial = Material.LIGHT_GRAY_DYE;
    private String handCloakNexoModelId;
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

    public void refreshAutoWield(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        WieldSession session = sessions.get(player.getUniqueId());
        if (!ItemUtil.canUseWeapon(player, weapon)) {
            if (session != null && session.autoManaged) {
                disable(player.getUniqueId());
            }
            return;
        }
        enableForEquippedWeapon(player, weapon);
    }

    private void enableForEquippedWeapon(Player player, ItemStack weapon) {
        enable(player, createVisualItemFromEquippedWeapon(weapon));
        WieldSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            session.autoManaged = true;
            applyEquippedHandModel(session, weapon);
        }
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
        applyHandVisibility(player, session);
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
        restoreEquippedHandModel(session);
        restoreHandVisibility(session);
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
        ItemStack stack = new ItemStack(handCloakMaterial);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Debug Wield Handle");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Use this while /debug wield is enabled.");
            lore.addAll(TooltipUtil.bulletList(
                    "The real weapon is rendered as an ItemDisplay.",
                    "Enable hand cloak mode to place this placeholder in-hand while previewing.",
                    "A resource-pack invisible model can make this placeholder visually disappear."));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to play a slash preview", null));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(debugHandleKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        if (handCloakNexoModelId != null) {
            ItemUtil.applyNexoModel(stack, handCloakNexoModelId);
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

    public HandVisibilityMode getHandVisibilityMode() {
        return handVisibilityMode;
    }

    public HandVisibilityMode setHandVisibilityMode(HandVisibilityMode mode) {
        HandVisibilityMode safeMode = mode == null ? HandVisibilityMode.NORMAL : mode;
        this.handVisibilityMode = safeMode;
        refreshHandVisibility();
        return safeMode;
    }

    public void setHandCloakMaterial(Material material) {
        this.handCloakMaterial = material == null || material.isAir() ? Material.LIGHT_GRAY_DYE : material;
        refreshHandVisibility();
    }

    public Material getHandCloakMaterial() {
        return handCloakMaterial;
    }

    public void setHandCloakNexoModelId(String handCloakNexoModelId) {
        this.handCloakNexoModelId = handCloakNexoModelId == null || handCloakNexoModelId.isBlank()
                ? null
                : handCloakNexoModelId.trim();
        refreshHandVisibility();
    }

    public String getHandCloakNexoModelId() {
        return handCloakNexoModelId;
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
                + ", handMode=" + handVisibilityMode.id()
                + ", handCloakMaterial=" + handCloakMaterial
                + ", handCloakNexo=" + (handCloakNexoModelId == null ? "none" : handCloakNexoModelId)
                + ", weaponTrailParticles=disabled"
                + ", forwardParticle=" + ArcSlashCombatUtil.swordPathSlashParticle()
                + ", preset=" + (activePreset == null ? "custom" : activePreset.id())
                + ", " + config.describe();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponEquip(WeaponEquipEvent event) {
        if (event.getHandSlot() != WeaponEquipEvent.HandSlot.MAIN_HAND) {
            return;
        }
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshAutoWield(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshAutoWield(player));
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
        if (session.swinging) {
            debugInput(player, source, "ignored=returning-to-idle, cancelled=" + eventCancelled);
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
                WieldStylePreset.COOL_SWEEP,
                WieldStylePreset.HORIZONTAL_SLASH
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


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVanillaHandDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        WieldSession session = sessions.get(player.getUniqueId());
        if (session == null || player.hasMetadata(SpellEffectUtil.BYPASS_STAT_SCALING_META)) {
            return;
        }
        event.setCancelled(true);
        debugInput(player, "vanilla-damage", "cancelled=custom-wield-session");
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

    private ItemStack createVisualItemFromEquippedWeapon(ItemStack weapon) {
        var customItem = ItemManager.getInstance().getCustomItemFromItemStack(weapon);
        Material material = customItem != null ? customItem.getMaterial() : ItemUtil.getTemplateMaterial(weapon);
        if (material == null || material.isAir()) {
            material = defaultMaterial;
        }
        return new ItemStack(material);
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
        if (session.swinging) {
            debugInput(player, source, "start-blocked=returning-to-idle");
            return;
        }
        if (!force && now < session.nextAllowedSwingTick) {
            debugInput(player, source, "start-blocked=cooldown, remainingTicks=" + (session.nextAllowedSwingTick - now));
            return;
        }
        int swingTotal = swingTicks(player, activeConfig);
        int returnTotal = returnToIdleTicks(player);
        session.swinging = true;
        session.nextAllowedSwingTick = now + swingTotal + returnTotal;
        session.display.setInterpolationDuration(Math.max(0, activeConfig.interpolationDuration()));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.15f);
        debugInput(player, source, "started"
                + ", cooldownTicks=" + activeConfig.cooldownTicks()
                + ", baseSwingTicks=" + activeConfig.swingTicks()
                + ", scaledSwingTicks=" + swingTotal
                + ", returnTicks=" + returnTotal
                + ", weaponTrailParticles=disabled"
                + ", forwardParticle=" + ArcSlashCombatUtil.swordPathSlashParticle());
        session.swingTask = new BukkitRunnable() {
            private final Set<UUID> hitTargets = new HashSet<>();
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
                if (tick < swingTotal) {
                    double progress = Math.min(1.0, tick / (double) (swingTotal - 1));
                    Pose pose = swingPose(online, easeOut(progress), activeConfig);
                    moveDisplay(session, pose);
                    damageSwingTargets(online, pose, hitTargets);
                    launchSwordPathSlash(online, pose.location(), hitTargets);
                    returnStartPose = pose;
                    tick++;
                    return;
                }

                int returnTick = tick - swingTotal;
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

    private void launchSwordPathSlash(Player player, Location swordLocation, Set<UUID> hitTargets) {
        if (swordLocation == null || swordLocation.getWorld() == null) {
            return;
        }
        Vector forward = player.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            return;
        }
        forward.normalize();
        double baseDamage = playerAttackDamage(player);
        Function<LivingEntity, Double> particleDamage = target ->
                StatsEffectListener.computeBasicAttackDamage(player, target, baseDamage, ThreadLocalRandom.current())
                        * SWORD_PATH_SLASH_DAMAGE_MULTIPLIER;
        ArcSlashCombatUtil.launchSwordPathSlashPoint(player, swordLocation, forward, particleDamage,
                this::playHitFeedback, SWORD_PATH_SLASH_RADIUS, SWORD_PATH_SLASH_TRAVEL_DISTANCE,
                SWORD_PATH_SLASH_TRAVEL_TICKS, hitTargets);
    }

    private int swingTicks(Player player, WieldStyleConfig activeConfig) {
        int baseTicks = activeConfig == null ? WieldStyleConfig.defaultConfig().swingTicks() : activeConfig.swingTicks();
        return attackSpeedScaledTicks(player, baseTicks, MIN_SWING_TICKS, MAX_SWING_TICKS, SWING_ATTACK_SPEED_WEIGHT);
    }

    private int returnToIdleTicks(Player player) {
        return attackSpeedScaledTicks(player, BASE_RETURN_TO_IDLE_TICKS, MIN_RETURN_TO_IDLE_TICKS,
                MAX_RETURN_TO_IDLE_TICKS, RETURN_ATTACK_SPEED_WEIGHT);
    }

    private int attackSpeedScaledTicks(Player player, int baseTicks, int minTicks, int maxTicks, double speedWeight) {
        if (player == null) {
            return Math.max(minTicks, Math.min(maxTicks, baseTicks));
        }
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double attackSpeed = Math.max(0.1D, stats.attackSpeed);
        double speedRatio = BASE_ANIMATION_ATTACK_SPEED / attackSpeed;
        double weightedRatio = Math.pow(speedRatio, Math.max(0.0D, speedWeight));
        int ticks = (int) Math.round(baseTicks * weightedRatio);
        return Math.max(minTicks, Math.min(maxTicks, ticks));
    }

    private void damageSwingTargets(Player player, Pose pose, Set<UUID> hitTargets) {
        Location location = pose.location();
        double baseDamage = playerAttackDamage(player);
        for (LivingEntity target : location.getNearbyLivingEntities(SWING_HIT_RADIUS)) {
            if (!isValidSwingTarget(player, target) || !hitTargets.add(target.getUniqueId())) {
                continue;
            }
            double damage = StatsEffectListener.computeBasicAttackDamage(player, target, baseDamage, ThreadLocalRandom.current());
            SpellEffectUtil.applyDirectSpellDamage(plugin, player, target, damage, true);
            playHitFeedback(target);
        }
    }

    private void playHitFeedback(LivingEntity target) {
        if (target == null || target.getWorld() == null) {
            return;
        }
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 1.05f);
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                target.getLocation().add(0.0, target.getHeight() * 0.6, 0.0),
                6, 0.2, 0.25, 0.2, 0.03);
    }

    private boolean isValidSwingTarget(Player player, LivingEntity target) {
        return target != null
                && !target.equals(player)
                && !(target instanceof Player)
                && !(target instanceof ArmorStand)
                && CombatTargetUtil.isSpellValidTarget(target);
    }

    private double playerAttackDamage(Player player) {
        Attribute attackDamageAttribute = AttributeUtil.resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
        AttributeInstance attribute = attackDamageAttribute == null ? null : player.getAttribute(attackDamageAttribute);
        return attribute == null ? SWING_DAMAGE_BASE_FALLBACK : Math.max(SWING_DAMAGE_BASE_FALLBACK, attribute.getValue());
    }

    private void applyTransformation(ItemDisplay display, Pose pose) {
        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f((float) Math.toRadians(pose.leftRotationDegrees()), 1f, 0f, 0f),
                new Vector3f(pose.scale(), pose.scale(), pose.scale()),
                new AxisAngle4f((float) Math.toRadians(pose.rightRotationDegrees()), 0f, 1f, 0f)
        ));
    }

    private void applyEquippedHandModel(WieldSession session, ItemStack weapon) {
        if (session == null || weapon == null || weapon.getType().isAir()) {
            return;
        }
        restoreEquippedHandModel(session);
        session.modeledHandItem = weapon;
        session.modeledHandState = ItemUtil.applyTemporaryNexoModel(weapon, AUTO_HAND_MODEL_NEXO_ID);
        Player player = plugin.getServer().getPlayer(session.playerId);
        if (player != null && player.isOnline()) {
            player.updateInventory();
        }
    }

    private void restoreEquippedHandModel(WieldSession session) {
        if (session == null || session.modeledHandState == null || session.modeledHandItem == null) {
            return;
        }
        ItemUtil.restoreVisualModelState(session.modeledHandItem, session.modeledHandState);
        Player player = plugin.getServer().getPlayer(session.playerId);
        if (player != null && player.isOnline()) {
            player.updateInventory();
        }
        session.modeledHandItem = null;
        session.modeledHandState = null;
    }

    private void refreshHandVisibility() {
        for (WieldSession session : sessions.values()) {
            Player player = plugin.getServer().getPlayer(session.playerId);
            if (player != null && player.isOnline()) {
                restoreHandVisibility(session);
                applyHandVisibility(player, session);
            }
        }
    }

    private void applyHandVisibility(Player player, WieldSession session) {
        if (handVisibilityMode != HandVisibilityMode.CLOAK_WITH_DEBUG_HANDLE || session.handCloaked) {
            return;
        }
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack current = player.getInventory().getItem(slot);
        session.cloakedSlot = slot;
        session.originalHandItem = current == null ? null : current.clone();
        session.handCloaked = true;
        player.getInventory().setItem(slot, createDebugHandle());
        player.updateInventory();
    }

    private void restoreHandVisibility(WieldSession session) {
        if (!session.handCloaked) {
            return;
        }
        Player player = plugin.getServer().getPlayer(session.playerId);
        if (player != null && player.isOnline()) {
            ItemStack current = player.getInventory().getItem(session.cloakedSlot);
            if (isDebugHandle(current)) {
                player.getInventory().setItem(session.cloakedSlot, session.originalHandItem);
            } else if (session.originalHandItem != null && !session.originalHandItem.getType().isAir()) {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(session.originalHandItem);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            player.updateInventory();
        }
        session.originalHandItem = null;
        session.handCloaked = false;
        session.cloakedSlot = -1;
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
        private boolean autoManaged;
        private ItemStack modeledHandItem;
        private ItemVisualModelState modeledHandState;
        private boolean handCloaked;
        private int cloakedSlot = -1;
        private ItemStack originalHandItem;
        private int comboIndex;
        private long nextAllowedSwingTick;
        private long lastInputTick = -1L;

        private WieldSession(UUID playerId, ItemDisplay display, ItemStack visualItem) {
            this.playerId = playerId;
            this.display = display;
            this.visualItem = visualItem;
        }
    }


    public enum HandVisibilityMode {
        NORMAL("normal", "Normal Hand"),
        CLOAK_WITH_DEBUG_HANDLE("cloak", "Cloak With Debug Handle");

        private final String id;
        private final String displayName;

        HandVisibilityMode(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }

        public static HandVisibilityMode fromString(String input) {
            if (input == null) {
                return null;
            }
            String normalized = input.toLowerCase(Locale.ROOT);
            for (HandVisibilityMode mode : values()) {
                if (mode.id.equals(normalized) || mode.name().equalsIgnoreCase(input)) {
                    return mode;
                }
            }
            return null;
        }
    }

    public enum WieldStylePreset {
        OVERHEAD_SLASH("overhead_slash", "Overhead Slash",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        DEFAULT_IDLE_DISTANCE, DEFAULT_IDLE_RIGHT_OFFSET, -0.28, -18.0, -12.0, -35.0, 60.0,
                        106.78813936115861, -186.6909558808558, 0.15980664918631562,
                        0.9877825861829582, 0.036862218691507104, 1.2711515689082424,
                        0.4006004088814077, -41.38846296298364, 48.91458472939911,
                        -53.61977811440525, 81.38337452682886, -66.83012338117723,
                        132.66759822330394, 99.1515139506437, -30.49929654416185)),
        BEYBLADE_SWIRL("beyblade_swirl", "Beyblade Swirl",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        DEFAULT_IDLE_DISTANCE, DEFAULT_IDLE_RIGHT_OFFSET, -0.28, -18.0, -12.0, -35.0, 60.0,
                        149.69386305872723, -171.96775775595756, 1.1393345553474903,
                        0.8506799627353459, -0.36832261979426884, 1.2973677239379766,
                        0.4424320994105332, 5.7231808755518045, 185.31289826600295,
                        -35.89715186429072, 190.9657973506081, -97.00310424138125,
                        -423.9848043587916, -1.5009839314438977, 372.082944972278)),
        COOL_SWEEP("cool_sweep", "Cool Sweep",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        DEFAULT_IDLE_DISTANCE, DEFAULT_IDLE_RIGHT_OFFSET, -0.28, -18.0, -12.0, -35.0, 60.0,
                        55.01033770127748, -298.06188822354045, 0.9024002861771805,
                        0.6039956910541824, 0.06503697955364385, 1.1567669547934205,
                        0.4147886779934885, 124.46899839538139, -83.92384491011171,
                        -22.865147709861816, 210.69150810349333, 125.24722177342,
                        -177.45824996471913, 60.07567107365088, 161.6478692857686)),
        COOL_SWIRL("cool_swirl", "Cool Swirl",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        DEFAULT_IDLE_DISTANCE, DEFAULT_IDLE_RIGHT_OFFSET, -0.28, -18.0, -12.0, -35.0, 60.0,
                        -123.2797945004859, -326.88824337384665, -1.2929481246029473,
                        1.0850175545906546, -0.07845679609481981, 1.3829925186475112,
                        0.2843594103411882, 100.41132146621277, -139.45950615941555,
                        -19.099799951032765, 202.27736608788462, 171.37924907810157,
                        -433.8053333171781, 134.99582825105335, -268.5415701430878)),
        PARRY_TYPE("parry_type", "Parry Type",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        DEFAULT_IDLE_DISTANCE, DEFAULT_IDLE_RIGHT_OFFSET, -0.28, -18.0, -12.0, -35.0, 60.0,
                        -157.70547416035907, 391.3088240022212, 0.8931485413847031,
                        -0.8684853539984333, 0.2650746358058361, 1.2321292243985533,
                        0.36050623086962186, 23.51300724555844, -155.64026598650247,
                        -97.81968552087531, -198.02284776870925, -36.72350683795469,
                        -497.9896943862577, -37.62978480829449, -447.6378094901502)),
        BASIC_ATTACK("basic_attack", "Basic Attack",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        DEFAULT_IDLE_DISTANCE, DEFAULT_IDLE_RIGHT_OFFSET, -0.28, -18.0, -12.0, -35.0, 60.0,
                        -7.597476350519571, -396.6678009578013, -1.002474704638901,
                        -0.10955255504269501, -0.49299159608471477, 1.1246103768763687,
                        0.43023693341752944, -34.03258465878355, 105.8781397276856,
                        -26.468320533805482, 111.74781919098103, -77.1466018303899,
                        258.9278522064494, 74.59162027705167, -280.6300726398954)),
        BASIC_ATTACK_TWO("basic_attack_2", "Basic Attack 2",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        DEFAULT_IDLE_DISTANCE, DEFAULT_IDLE_RIGHT_OFFSET, -0.28, -18.0, -12.0, -35.0, 60.0,
                        -85.68423014032777, -202.81888790329361, 1.2802284526314247,
                        -0.5111532543229289, -0.07972367059891083, 1.179850083916887,
                        0.5010660807049536, 4.879281831669914, 165.93664880282688,
                        71.18506108827745, 72.11240325338264, 155.02853975191795,
                        -406.01305010724536, 187.880697581937, 52.44780271370257)),
        HORIZONTAL_SLASH("horizontal_slash", "Horizontal Slash",
                new WieldStyleConfig(16, 17, 1, 0.82,
                        DEFAULT_IDLE_DISTANCE, DEFAULT_IDLE_RIGHT_OFFSET, -0.28, -18.0, -12.0, -35.0, 60.0,
                        99.10203154985236, 289.83607866228397, -1.5018903566121915,
                        0.1904062981563185, -0.5539701381859183, 1.3438135165695648,
                        0.43818404742518, -242.37929775792298, -92.41844422327281,
                        -184.60878826451847, 12.334831170690254, -16.718988101303378,
                        369.9209503977733, 98.2872356071391, -254.1760750611425));

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
