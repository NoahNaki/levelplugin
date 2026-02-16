package me.nakilex.levelplugin.horse.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.horse.data.HorseData;
import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.particles.presets.ElementalPresets;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class HorseManager implements Listener {

    private static final long COOLDOWN_MS = 5_000L; // 5 seconds
    private static final double MOVE_EPSILON_SQUARED = 0.0001;
    private static final double JUMP_DELTA_Y_THRESHOLD = 0.35;

    private final HorseConfigManager configManager;
    private final ParticleService particleService;
    private final Map<UUID, HorseData> horses = new HashMap<>();
    private final Map<UUID, Long> lastSpawnTimestamps = new HashMap<>();
    private final Map<UUID, UUID> activeHorseByPlayer = new HashMap<>();
    private final Map<UUID, BukkitTask> trailTasks = new HashMap<>();
    private final Map<UUID, Location> lastRideLocation = new HashMap<>();
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();

    // Constructor to accept HorseConfigManager
    public HorseManager(HorseConfigManager configManager) {
        this.configManager = configManager;
        this.particleService = new ParticleService(Main.getInstance());
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());

        // Load all previously saved horses into memory
        Set<String> keys = configManager.getHorseUUIDStrings();
        for (String uuidStr : keys) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                HorseData data = configManager.loadHorseData(uuid);
                if (data != null) {
                    horses.put(uuid, data);
                }
            } catch (IllegalArgumentException ignored) {
                // Skip invalid UUID entries
            }
        }
    }

    public HorseData getHorse(UUID uuid) {
        return horses.get(uuid);
    }

    public void rerollHorse(UUID uuid) {
        HorseData previous = horses.get(uuid);
        String trail = previous != null ? previous.getTrailPreset() : "OFF";
        HorseData newHorse = HorseData.randomHorse(uuid);
        newHorse.setTrailPreset(trail);
        horses.put(uuid, newHorse);
        configManager.saveHorseData(uuid, newHorse); // Persist data
    }

    public void setTrailPreset(UUID uuid, String presetName) {
        HorseData data = horses.get(uuid);
        if (data == null) {
            return;
        }
        data.setTrailPreset(presetName);
        configManager.saveHorseData(uuid, data);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            updateActiveHorseTrail(player);
        }
    }

    public void spawnHorse(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long last = lastSpawnTimestamps.get(uuid);
        if (last != null && now - last < COOLDOWN_MS) {
            return;
        }
        lastSpawnTimestamps.put(uuid, now);

        HorseData horseData = getHorse(uuid);
        if (horseData == null) {
            send(player, MessageType.ERROR, "You do not own a horse.");
            return;
        }

        dismountHorse(player);

        AbstractHorse horse;
        switch (horseData.getType().toUpperCase(Locale.ROOT)) {
            case "ZOMBIE":
                horse = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.ZombieHorse.class);
                break;
            case "SKELETON":
                horse = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.SkeletonHorse.class);
                break;
            default:
                Horse normalHorse = player.getWorld().spawn(player.getLocation(), Horse.class);
                normalHorse.setColor(Horse.Color.valueOf(horseData.getType().toUpperCase(Locale.ROOT)));
                horse = normalHorse;
                break;
        }

        horse.setOwner(player);
        horse.setTamed(true);
        horse.setCustomName(player.getName() + "'s Horse");
        horse.setCustomNameVisible(true);
        horse.setInvulnerable(true);

        int jumpStars = Math.min(horseData.getJumpHeight(), 5);
        int speedStars = Math.min(horseData.getSpeed(), 5);
        horse.setJumpStrength(0.3 + jumpStars * 0.1);
        var speedAttr = horse.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.12 + speedStars * 0.04);
        }

        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.addPassenger(player);

        activeHorseByPlayer.put(uuid, horse.getUniqueId());
        lastRideLocation.put(uuid, horse.getLocation().clone());
        wasOnGround.put(uuid, horse.isOnGround());
        updateActiveHorseTrail(player);
    }

    public void updateActiveHorseTrail(Player player) {
        UUID ownerId = player.getUniqueId();
        stopTrail(ownerId);

        HorseData data = horses.get(ownerId);
        if (data == null || !data.hasTrailPreset()) {
            return;
        }

        UUID activeHorseId = activeHorseByPlayer.get(ownerId);
        if (activeHorseId == null) {
            return;
        }

        Entity entity = Bukkit.getEntity(activeHorseId);
        if (!(entity instanceof AbstractHorse horse) || !horse.isValid()) {
            activeHorseByPlayer.remove(ownerId);
            return;
        }

        ParticlePreset preset = ElementalPresets.getPreset(data.getTrailPreset());
        if (preset == null) {
            return;
        }

        BukkitTask task = particleService.renderPresetWhile(
                player,
                preset,
                () -> horse.isValid() ? horse.getLocation().clone().add(0, 1.1, 0) : null,
                () -> player.isOnline()
                        && horse.isValid()
                        && player.isInsideVehicle()
                        && player.getVehicle() != null
                        && player.getVehicle().getUniqueId().equals(horse.getUniqueId())
        );
        trailTasks.put(ownerId, task);
    }

    public void dismountHorse(Player player) {
        if (player.isInsideVehicle() && player.getVehicle() instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) player.getVehicle();
            cleanupActiveHorse(player.getUniqueId(), horse, true);
            player.leaveVehicle();
        }
    }

    /** Remove all horse data for a player. */
    public void clearPlayerData(UUID uuid) {
        horses.remove(uuid);
        lastSpawnTimestamps.remove(uuid);
        stopTrail(uuid);
        activeHorseByPlayer.remove(uuid);
        lastRideLocation.remove(uuid);
        wasOnGround.remove(uuid);
        configManager.deleteHorseData(uuid);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof AbstractHorse) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.SADDLE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRideMovement(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof AbstractHorse horse)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID activeHorseId = activeHorseByPlayer.get(playerId);
        if (activeHorseId == null || !activeHorseId.equals(horse.getUniqueId())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != to.getWorld()) {
            return;
        }

        if (from.distanceSquared(to) <= MOVE_EPSILON_SQUARED) {
            return;
        }

        Location previous = lastRideLocation.put(playerId, horse.getLocation().clone());
        double deltaMeters = 0.0;
        if (previous != null && previous.getWorld() == horse.getWorld()) {
            deltaMeters = Math.sqrt(previous.distanceSquared(horse.getLocation()));
        }

        boolean previousGround = wasOnGround.getOrDefault(playerId, true);
        boolean currentGround = horse.isOnGround();
        wasOnGround.put(playerId, currentGround);
        int jumpCount = (!currentGround && previousGround && (to.getY() - from.getY()) > JUMP_DELTA_Y_THRESHOLD) ? 1 : 0;

        if (deltaMeters > 0 || jumpCount > 0) {
            BattlePassManager battlePassManager = Main.getInstance().getBattlePassManager();
            if (battlePassManager != null) {
                battlePassManager.recordHorseChallengeProgress(player, deltaMeters, jumpCount);
            }
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getDismounted() instanceof AbstractHorse horse)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID activeHorseId = activeHorseByPlayer.get(playerId);
        if (activeHorseId == null || !activeHorseId.equals(horse.getUniqueId())) {
            return;
        }

        cleanupActiveHorse(playerId, horse, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Entity active = getActiveHorseEntity(playerId);
        if (active instanceof AbstractHorse horse) {
            cleanupActiveHorse(playerId, horse, true);
        } else {
            stopTrail(playerId);
            activeHorseByPlayer.remove(playerId);
            lastRideLocation.remove(playerId);
            wasOnGround.remove(playerId);
        }
    }

    private Entity getActiveHorseEntity(UUID ownerId) {
        UUID entityId = activeHorseByPlayer.get(ownerId);
        if (entityId == null) {
            return null;
        }
        return Bukkit.getEntity(entityId);
    }

    private void cleanupActiveHorse(UUID ownerId, AbstractHorse horse, boolean removeEntity) {
        stopTrail(ownerId);
        activeHorseByPlayer.remove(ownerId);
        lastRideLocation.remove(ownerId);
        wasOnGround.remove(ownerId);
        if (removeEntity && horse != null && horse.isValid()) {
            horse.remove();
        }
    }

    private void stopTrail(UUID ownerId) {
        BukkitTask task = trailTasks.remove(ownerId);
        if (task != null) {
            task.cancel();
        }
    }
}
