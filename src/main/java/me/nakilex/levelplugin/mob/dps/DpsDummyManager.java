package me.nakilex.levelplugin.mob.dps;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages persistent DPS training dummies backed by MythicMobs' {@code training_dummy}
 * (with a vanilla fallback). Dummies constantly heal, cannot be moved, and expose
 * a hologram showing rolling DPS over the last few seconds.
 */
public class DpsDummyManager implements Listener {
    private static final String DUMMY_TAG = "dps_dummy";
    private static final double DEFAULT_MAX_HEALTH = 2048.0;
    private static final long DPS_WINDOW_MS = 5_000L;
    private static final DecimalFormat DPS_FORMAT = new DecimalFormat("#,##0.0");

    private final Main plugin;
    private final BukkitAPIHelper mythicHelper;
    private final Map<UUID, Dummy> dummies = new HashMap<>();
    private final Map<UUID, UUID> selections = new HashMap<>();
    private BukkitTask updater;

    public DpsDummyManager(Main plugin, BukkitAPIHelper mythicHelper) {
        this.plugin = plugin;
        this.mythicHelper = mythicHelper;
        startUpdater();
    }

    /** Spawn a dummy at the given player's location. */
    public void spawn(Player player) {
        Location loc = player.getLocation();
        LivingEntity entity = spawnMythic(loc);
        if (entity == null) {
            entity = spawnFallback(loc);
        }
        if (entity == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Unable to spawn training dummy.");
            return;
        }

        prepareEntity(entity);
        Dummy dummy = new Dummy(entity, createHologram(entity));
        dummies.put(entity.getUniqueId(), dummy);
        selections.put(player.getUniqueId(), entity.getUniqueId());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Spawned DPS dummy and selected it.");
    }

    /** Remove the dummy selected by the player, or the one matching the id string. */
    public void despawn(Player player, String idOrNull) {
        UUID target = resolveTarget(player, idOrNull);
        if (target == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No DPS dummy selected or found.");
            return;
        }
        Dummy dummy = dummies.remove(target);
        if (dummy != null) {
            dummy.despawn();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Despawned DPS dummy.");
        }
        selections.values().removeIf(uuid -> uuid.equals(target));
    }

    /** Attempt to select the dummy the player is currently looking at. */
    public void select(Player player) {
        org.bukkit.entity.Entity lookedAt = player.getTargetEntity(10);
        if (!(lookedAt instanceof LivingEntity target) || !dummies.containsKey(target.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Look at a DPS dummy to select it.");
            return;
        }
        selections.put(player.getUniqueId(), target.getUniqueId());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Selected DPS dummy.");
    }

    /** List all active dummies to the requester. */
    public void list(Player player) {
        if (dummies.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "No DPS dummies are currently spawned.");
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Active DPS dummies:");
        dummies.values().forEach(dummy -> dummy.sendListEntry(player));
    }

    /** IDs for tab completion. */
    public List<String> getDummyIds() {
        return dummies.keySet().stream().map(UUID::toString).toList();
    }

    /** Remove holograms and entities when shutting down. */
    public void shutdown() {
        if (updater != null) {
            updater.cancel();
        }
        dummies.values().forEach(Dummy::despawn);
        dummies.clear();
        selections.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        Dummy dummy = dummies.get(entity.getUniqueId());
        if (dummy == null) return;

        double damage = event.getFinalDamage();
        dummy.recordDamage(damage);

        event.setCancelled(true);
        entity.setFireTicks(0);
        entity.setVelocity(new Vector());
        entity.setHealth(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        dummy.refreshHologram();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selections.remove(event.getPlayer().getUniqueId());
    }

    private void startUpdater() {
        updater = Bukkit.getScheduler().runTaskTimer(plugin, () -> dummies.values().forEach(Dummy::refreshHologram), 20L, 20L);
    }

    private LivingEntity spawnMythic(Location loc) {
        if (mythicHelper == null) return null;

        org.bukkit.entity.Entity entity = mythicHelper.spawnMythicMob("training_dummy", loc);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private LivingEntity spawnFallback(Location loc) {
        LivingEntity entity = loc.getWorld().spawn(loc, org.bukkit.entity.Zombie.class, zombie -> zombie.setAdult());
        return entity;
    }

    private void prepareEntity(LivingEntity entity) {
        entity.addScoreboardTag(DUMMY_TAG);
        entity.setAI(false);
        entity.setGravity(false);
        entity.setCollidable(false);
        entity.setRemoveWhenFarAway(false);
        entity.setSilent(true);
        if (entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
            entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        }
        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(DEFAULT_MAX_HEALTH);
            entity.setHealth(DEFAULT_MAX_HEALTH);
        }
    }

    private MultiLineHologram createHologram(LivingEntity entity) {
        Location above = entity.getLocation().clone().add(0, entity.getHeight() + 0.5, 0);
        MultiLineHologram hologram = new MultiLineHologram(above, DUMMY_TAG);
        hologram.spawn(List.of(formatDpsLine(0)));
        return hologram;
    }

    private String formatDpsLine(double dps) {
        return ChatColor.translateAlternateColorCodes('&', "&aDPS&7: &f" + DPS_FORMAT.format(dps));
    }

    private UUID resolveTarget(Player player, String idOrNull) {
        if (idOrNull != null) {
            try {
                UUID uuid = UUID.fromString(idOrNull);
                if (dummies.containsKey(uuid)) return uuid;
            } catch (IllegalArgumentException ignored) { }
        }
        return selections.get(player.getUniqueId());
    }

    /** Lightweight dummy state holder. */
    private class Dummy {
        private final LivingEntity entity;
        private final MultiLineHologram hologram;
        private final Deque<DamageSample> samples = new ArrayDeque<>();

        Dummy(LivingEntity entity, MultiLineHologram hologram) {
            this.entity = entity;
            this.hologram = hologram;
        }

        void recordDamage(double amount) {
            long now = System.currentTimeMillis();
            samples.addLast(new DamageSample(amount, now));
            trim(now);
        }

        void refreshHologram() {
            double dps = computeDps();
            hologram.setLines(List.of(formatDpsLine(dps)));
        }

        double computeDps() {
            long now = System.currentTimeMillis();
            trim(now);
            if (samples.isEmpty()) return 0d;
            double total = samples.stream().mapToDouble(sample -> sample.amount).sum();
            long window = Math.max(1L, Math.min(DPS_WINDOW_MS, now - samples.getFirst().time));
            return total * 1000d / window;
        }

        void trim(long now) {
            while (!samples.isEmpty() && now - samples.getFirst().time > DPS_WINDOW_MS) {
                samples.removeFirst();
            }
        }

        void sendListEntry(Player viewer) {
            Location l = entity.getLocation();
            String location = ChatColor.AQUA + l.getWorld().getName() + ChatColor.GRAY + " @ " +
                    ChatColor.WHITE + String.format("%.1f, %.1f, %.1f", l.getX(), l.getY(), l.getZ());
            net.md_5.bungee.api.chat.TextComponent line = new net.md_5.bungee.api.chat.TextComponent(ChatColor.GRAY + " - " + location + " ");
            net.md_5.bungee.api.chat.TextComponent delete = new net.md_5.bungee.api.chat.TextComponent(ChatColor.WHITE + "[" + ChatColor.RED + "-" + ChatColor.WHITE + "]");
            delete.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                    "/dpsdummy despawn " + entity.getUniqueId()));
            line.addExtra(delete);
            viewer.spigot().sendMessage(line);
        }

        void despawn() {
            hologram.despawn();
            entity.remove();
        }
    }

    private record DamageSample(double amount, long time) {}
}
