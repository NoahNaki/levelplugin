package me.nakilex.levelplugin.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
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
 * enabled, then left-clicking animates it through a slash arc. This gives us a
 * reusable place to tune the presentation before wiring it into real weapon
 * hit windows and damage rules.</p>
 */
public class WieldStyleDebugManager implements Listener {
    private static final int DEFAULT_COOLDOWN_TICKS = 12;
    private static final int DEFAULT_SWING_TICKS = 8;
    private static final double IDLE_DISTANCE = 1.05;
    private static final double IDLE_RIGHT_OFFSET = 0.42;
    private static final double IDLE_UP_OFFSET = -0.38;
    private static final float DISPLAY_SCALE = 0.75f;

    private final Main plugin;
    private final NamespacedKey debugHandleKey;
    private final Map<UUID, WieldSession> sessions = new ConcurrentHashMap<>();

    private Material defaultMaterial = Material.DIAMOND_SWORD;
    private String defaultNexoModelId;
    private int cooldownTicks = DEFAULT_COOLDOWN_TICKS;
    private int swingTicks = DEFAULT_SWING_TICKS;

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
                    moveDisplay(session, idlePose(online));
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
        this.cooldownTicks = Math.max(1, cooldownTicks);
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setSwingTicks(int swingTicks) {
        this.swingTicks = Math.max(2, swingTicks);
    }

    public int getSwingTicks() {
        return swingTicks;
    }

    public String describeSettings() {
        return "material=" + defaultMaterial
                + ", nexo=" + (defaultNexoModelId == null ? "none" : defaultNexoModelId)
                + ", cooldownTicks=" + cooldownTicks
                + ", swingTicks=" + swingTicks;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        WieldSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        startSwing(event.getPlayer(), session, false);
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
        Pose pose = idlePose(player);
        World world = player.getWorld();
        return world.spawn(pose.location(), ItemDisplay.class, display -> {
            display.setItemStack(item);
            display.setBillboard(Display.Billboard.FIXED);
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setPersistent(false);
            display.setSilent(true);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(1);
            applyTransformation(display, pose);
        });
    }

    private void startSwing(Player player, WieldSession session, boolean force) {
        long now = player.getWorld().getFullTime();
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
        session.nextAllowedSwingTick = now + cooldownTicks;
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
                int total = Math.max(2, swingTicks);
                double progress = Math.min(1.0, tick / (double) (total - 1));
                moveDisplay(session, swingPose(online, easeOut(progress)));
                tick++;
                if (tick >= total) {
                    session.swinging = false;
                    moveDisplay(session, idlePose(online));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Pose idlePose(Player player) {
        Basis basis = Basis.from(player);
        Location location = player.getEyeLocation().clone()
                .add(basis.forward().multiply(IDLE_DISTANCE))
                .add(basis.right().multiply(IDLE_RIGHT_OFFSET))
                .add(basis.up().multiply(IDLE_UP_OFFSET));
        location.setYaw(player.getLocation().getYaw() - 35.0f);
        location.setPitch(-8.0f);
        return new Pose(location, 0.0, -35.0, 35.0, DISPLAY_SCALE);
    }

    private Pose swingPose(Player player, double progress) {
        Basis basis = Basis.from(player);
        double angle = Math.toRadians(-85.0 + (190.0 * progress));
        double side = Math.cos(angle) * 0.72;
        double up = Math.sin(angle) * 0.62 - 0.16;
        double forwardDistance = 1.0 + Math.sin(progress * Math.PI) * 0.55;
        Location location = player.getEyeLocation().clone()
                .add(basis.forward().multiply(forwardDistance))
                .add(basis.right().multiply(side))
                .add(basis.up().multiply(up));
        location.setYaw(player.getLocation().getYaw() + (float) (-115.0 + (230.0 * progress)));
        location.setPitch((float) (-30.0 + (80.0 * progress)));
        return new Pose(location,
                -35.0 + (95.0 * progress),
                -80.0 + (160.0 * progress),
                95.0 - (190.0 * progress),
                DISPLAY_SCALE);
    }

    private void moveDisplay(WieldSession session, Pose pose) {
        if (session.display == null || session.display.isDead()) {
            return;
        }
        session.display.teleport(pose.location());
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

    private record Pose(Location location, double leftRotationDegrees, double yawOffsetDegrees,
                        double rightRotationDegrees, float scale) {
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

        private WieldSession(UUID playerId, ItemDisplay display, ItemStack visualItem) {
            this.playerId = playerId;
            this.display = display;
            this.visualItem = visualItem;
        }
    }
}
