package me.nakilex.levelplugin.stronghold;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.stronghold.utils.StrongholdMobSpawnUtil;
import me.nakilex.levelplugin.utils.CombatTargetUtil;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.utils.RewardBombUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Lightweight shrine defense encounter for Stronghold iteration 1.
 */
public class StrongholdShrineManager implements Listener {
    private static final String SHRINE_ID_META = "stronghold_shrine_id";
    private static final String SHRINE_HOLOGRAM_TAG = "stronghold_shrine_hologram";
    private static final String SHRINE_INTERACTION_TAG = "stronghold_shrine_interaction";

    private static final double DEFAULT_ZONE_RADIUS = 6.0;
    private static final int DEFAULT_DURATION_SECONDS = 10;
    private static final double DEFAULT_MAX_HEALTH = 250.0;
    private static final int SHRINE_BONUS_XP_MIN = 55;
    private static final int SHRINE_BONUS_XP_MAX = 95;
    private static final int SHRINE_BONUS_COINS_MIN = 35;
    private static final int SHRINE_BONUS_COINS_MAX = 85;
    private static final double SHRINE_REWARD_GEAR_COMBAT_POWER = 45.0;
    private static final String SHRINE_FURNITURE_ID = "medievalpack_baner";

    private final Main plugin;
    private final Map<UUID, ShrineAnchor> anchorsById = new HashMap<>();
    private final Map<UUID, ActiveShrineEvent> activeByAnchor = new HashMap<>();
    private final List<String> shrineMobPool = List.of("goblin_warrior", "goblin_archer", "goblin_assassin");

    public StrongholdShrineManager(Main plugin) {
        this.plugin = plugin;
    }

    public Optional<ShrineAnchor> spawnShrine(Location location, double hp) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        World world = location.getWorld();
        double maxHp = Math.max(20.0, hp);

        Location shrineBase = location.clone().add(0.0, 1.0, 0.0);
        FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(SHRINE_FURNITURE_ID);
        if (mechanic == null) {
            plugin.getLogger().warning("[StrongholdShrineManager] Missing Nexo furniture id '" + SHRINE_FURNITURE_ID + "'.");
            return Optional.empty();
        }
        org.bukkit.entity.ItemDisplay shrineDisplay = NexoFurniture.place(SHRINE_FURNITURE_ID, shrineBase.clone(), 0f, org.bukkit.block.BlockFace.NORTH);
        if (shrineDisplay == null) {
            return Optional.empty();
        }
        LivingEntity living = world.spawn(shrineBase.clone(), org.bukkit.entity.Slime.class, slime -> {
            slime.setSize(1);
            slime.setAI(false);
            slime.setCollidable(false);
            slime.setGravity(false);
            slime.setInvisible(true);
            slime.setSilent(true);
            slime.setRemoveWhenFarAway(false);
        });
        living.setAI(false);
        living.setCollidable(false);
        living.setMetadata(SHRINE_ID_META, new FixedMetadataValue(plugin, "pending"));
        if (living instanceof Mob mob) {
            mob.setRemoveWhenFarAway(false);
        }
        if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
            living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHp);
        }
        living.setHealth(Math.min(maxHp, living.getAttribute(Attribute.MAX_HEALTH) != null
                ? living.getAttribute(Attribute.MAX_HEALTH).getValue()
                : maxHp));
        CombatTargetUtil.markDamageImmune(living, plugin);

        org.bukkit.entity.TextDisplay title = world.spawn(shrineBase.clone().add(0.0, 2.35, 0.0), org.bukkit.entity.TextDisplay.class, td -> {
            td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            td.setText(ChatColor.LIGHT_PURPLE + "<glyph:star> " + ChatColor.WHITE + "Shrine");
            td.addScoreboardTag(SHRINE_HOLOGRAM_TAG);
            td.setMetadata(SHRINE_ID_META, new FixedMetadataValue(plugin, "pending"));
        });
        org.bukkit.entity.TextDisplay subtitle = world.spawn(shrineBase.clone().add(0.0, 2.1, 0.0), org.bukkit.entity.TextDisplay.class, td -> {
            td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            td.setText(ChatColor.GRAY + "Right-click to begin defense");
            td.addScoreboardTag(SHRINE_HOLOGRAM_TAG);
            td.setMetadata(SHRINE_ID_META, new FixedMetadataValue(plugin, "pending"));
        });
        Interaction interaction = world.spawn(shrineBase.clone().add(0.0, 1.2, 0.0), Interaction.class, i -> {
            i.setInteractionHeight(2.0f);
            i.setInteractionWidth(1.3f);
            i.addScoreboardTag(SHRINE_INTERACTION_TAG);
            i.setMetadata(SHRINE_ID_META, new FixedMetadataValue(plugin, "pending"));
        });

        UUID shrineId = UUID.randomUUID();
        applyShrineMetadata(living, shrineId);
        applyShrineMetadata(shrineDisplay, shrineId);
        applyShrineMetadata(title, shrineId);
        applyShrineMetadata(subtitle, shrineId);
        applyShrineMetadata(interaction, shrineId);

        ShrineAnchor anchor = new ShrineAnchor(shrineId, shrineDisplay, living, title, subtitle, interaction, shrineBase.clone(), maxHp, DEFAULT_ZONE_RADIUS);
        anchorsById.put(shrineId, anchor);
        return Optional.of(anchor);
    }

    public int spawnRandomShrines(Location origin, int count, int searchRadius, double hp) {
        if (origin == null || origin.getWorld() == null || count <= 0) {
            return 0;
        }
        List<Location> candidates = new ArrayList<>();
        World world = origin.getWorld();
        int radius = Math.max(8, searchRadius);
        for (int x = -radius; x <= radius; x += 3) {
            for (int z = -radius; z <= radius; z += 3) {
                Location sample = origin.clone().add(x, 0.0, z);
                int surfaceY = world.getHighestBlockYAt(sample);
                Block ground = world.getBlockAt(sample.getBlockX(), surfaceY - 1, sample.getBlockZ());
                if (ground.getType() != Material.GRASS_BLOCK) {
                    continue;
                }
                Location spawn = ground.getLocation().add(0.5, 0.0, 0.5);
                if (spawn.distanceSquared(origin) < 10 * 10) {
                    continue;
                }
                candidates.add(spawn);
            }
        }
        if (candidates.isEmpty()) {
            return 0;
        }
        java.util.Collections.shuffle(candidates);
        int spawned = 0;
        for (Location candidate : candidates) {
            boolean tooClose = anchorsById.values().stream()
                    .anyMatch(anchor -> anchor.origin.getWorld().equals(candidate.getWorld())
                            && anchor.origin.distanceSquared(candidate) < 12 * 12);
            if (tooClose) {
                continue;
            }
            if (spawnShrine(candidate, hp).isPresent()) {
                spawned++;
            }
            if (spawned >= count) {
                break;
            }
        }
        return spawned;
    }

    public int spawnFallbackShrines(Location origin, int count, int searchRadius, double hp) {
        if (origin == null || origin.getWorld() == null || count <= 0) {
            return 0;
        }
        World world = origin.getWorld();
        int radius = Math.max(16, searchRadius);
        List<Location> candidates = new ArrayList<>();
        for (int x = -radius; x <= radius; x += 4) {
            for (int z = -radius; z <= radius; z += 4) {
                Location sample = origin.clone().add(x, 0.0, z);
                int surfaceY = world.getHighestBlockYAt(sample);
                Block ground = world.getBlockAt(sample.getBlockX(), surfaceY - 1, sample.getBlockZ());
                if (ground.getType().isAir() || !ground.getType().isSolid() || ground.isLiquid()) {
                    continue;
                }
                Location spawn = ground.getLocation().add(0.5, 0.0, 0.5);
                if (spawn.distanceSquared(origin) < 8 * 8) {
                    continue;
                }
                candidates.add(spawn);
            }
        }
        if (candidates.isEmpty()) {
            return 0;
        }
        java.util.Collections.shuffle(candidates);
        int spawned = 0;
        for (Location candidate : candidates) {
            boolean tooClose = anchorsById.values().stream()
                    .anyMatch(anchor -> anchor.origin.getWorld().equals(candidate.getWorld())
                            && anchor.origin.distanceSquared(candidate) < 10 * 10);
            if (tooClose) {
                continue;
            }
            if (spawnShrine(candidate, hp).isPresent()) {
                spawned++;
            }
            if (spawned >= count) {
                break;
            }
        }
        return spawned;
    }

    public void cleanup() {
        for (ActiveShrineEvent event : new ArrayList<>(activeByAnchor.values())) {
            event.stop();
        }
        activeByAnchor.clear();
        for (ShrineAnchor anchor : new ArrayList<>(anchorsById.values())) {
            despawnAnchor(anchor);
        }
        anchorsById.clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (handleInteract(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (handleInteract(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    private boolean handleInteract(Player player, Entity clicked) {
        UUID shrineId = readShrineId(clicked);
        if (shrineId == null) {
            return false;
        }
        ShrineAnchor anchor = anchorsById.get(shrineId);
        if (anchor == null || !anchor.isValid()) {
            return false;
        }
        if (activeByAnchor.containsKey(shrineId)) {
            send(player, MessageType.WARNING, "This shrine is already active.");
            return true;
        }
        startShrineEvent(anchor, player);
        return true;
    }

    private void startShrineEvent(ShrineAnchor anchor, Player activator) {
        ActiveShrineEvent event = new ActiveShrineEvent(anchor, activator.getUniqueId(), DEFAULT_DURATION_SECONDS);
        activeByAnchor.put(anchor.id(), event);
        send(activator, MessageType.INFO,
                "Shrine defense started. Keep mobs off the shrine for " + DEFAULT_DURATION_SECONDS + " seconds.");
        anchor.interaction().remove();
        anchor.subtitle().setText(ChatColor.RED + "Defend the shrine!");
        anchor.title().setText(ChatColor.LIGHT_PURPLE + "<glyph:star> " + ChatColor.WHITE + "Shrine " + ChatColor.GRAY + "[Active]");
        event.start();
    }

    @EventHandler
    public void onShrineDamaged(EntityDamageByEntityEvent event) {
        UUID shrineId = readShrineId(event.getEntity());
        if (shrineId == null) {
            return;
        }
        ShrineAnchor anchor = anchorsById.get(shrineId);
        if (anchor == null) {
            return;
        }

        ActiveShrineEvent active = activeByAnchor.get(shrineId);
        if (active == null) {
            event.setCancelled(true);
            return;
        }
        if (CombatTargetUtil.isPlayerSourced(event.getDamager())) {
            event.setCancelled(true);
            return;
        }

        double current = anchor.entity().getHealth();
        double after = Math.max(0.0, current - event.getFinalDamage());
        if (after <= 0.0) {
            event.setCancelled(true);
            failShrine(active, "The shrine was destroyed.");
            return;
        }
        anchor.subtitle().setText(ChatColor.GRAY + "HP: " + ChatColor.RED + ((int) Math.ceil(after))
                + ChatColor.GRAY + "/" + ChatColor.WHITE + ((int) Math.ceil(anchor.maxHealth())));
    }

    @EventHandler
    public void onShrineDeath(EntityDeathEvent event) {
        UUID shrineId = readShrineId(event.getEntity());
        if (shrineId == null) {
            return;
        }
        ActiveShrineEvent active = activeByAnchor.get(shrineId);
        if (active != null) {
            failShrine(active, "The shrine was destroyed.");
        }
    }

    private void failShrine(ActiveShrineEvent active, String reason) {
        active.stop();
        activeByAnchor.remove(active.anchor.id());
        notifyNearby(active.anchor.origin, MessageType.ERROR, reason + " Shrine failed.");
        active.anchor.title().setText(ChatColor.DARK_RED + "Shrine [Failed]");
        active.anchor.subtitle().setText(ChatColor.GRAY + "Defeat all mobs and retry with a new shrine.");
        despawnAnchor(active.anchor);
        anchorsById.remove(active.anchor.id());
    }

    private void completeShrine(ActiveShrineEvent active) {
        active.stop();
        activeByAnchor.remove(active.anchor.id());
        notifyNearby(active.anchor.origin, MessageType.SUCCESS,
                "Shrine defended successfully! You earned a boon and a reward bomb.");

        Player activator = plugin.getServer().getPlayer(active.activator);
        if (activator != null && activator.isOnline()) {
            grantSimpleBoon(activator);
            grantShrineRunRewards(activator);
            RewardBombUtil.startRewardBomb(plugin, active.anchor.origin.clone().add(0.0, 0.3, 0.0),
                    () -> rollShrineReward(activator), 100, activator);
        } else {
            RewardBombUtil.startRewardBomb(plugin, active.anchor.origin.clone().add(0.0, 0.3, 0.0),
                    () -> rollShrineReward(null), 100);
        }

        active.anchor.title().setText(ChatColor.GREEN + "Shrine [Secured]");
        active.anchor.subtitle().setText(ChatColor.GRAY + "Rewards granted.");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            despawnAnchor(active.anchor);
            anchorsById.remove(active.anchor.id());
        }, 40L);
    }

    private void grantShrineRunRewards(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        int xp = ThreadLocalRandom.current().nextInt(SHRINE_BONUS_XP_MIN, SHRINE_BONUS_XP_MAX + 1);
        int coins = ThreadLocalRandom.current().nextInt(SHRINE_BONUS_COINS_MIN, SHRINE_BONUS_COINS_MAX + 1);
        plugin.getLevelManager().addXP(player, xp);
        plugin.getEconomyManager().addCoins(player, coins);
        send(player, MessageType.REWARD,
                "Shrine bonus: " + ChatColor.GREEN + "+" + xp + " <glyph:experience_orb_icon> XP");
        CurrencyMessageUtil.sendReceive(player, CurrencyMessageUtil.Currency.COINS, coins);
    }

    private ItemStack rollShrineReward(Player owner) {
        ItemStack gear = rollShrineGearReward(owner);
        if (gear != null) {
            return gear;
        }
        int roll = ThreadLocalRandom.current().nextInt(5);
        return switch (roll) {
            case 0 -> new ItemStack(Material.EMERALD, ThreadLocalRandom.current().nextInt(3, 7));
            case 1 -> new ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(2, 6));
            case 2 -> new ItemStack(Material.EXPERIENCE_BOTTLE, ThreadLocalRandom.current().nextInt(4, 9));
            case 3 -> new ItemStack(Material.LAPIS_LAZULI, ThreadLocalRandom.current().nextInt(4, 10));
            default -> new ItemStack(Material.GOLDEN_APPLE, 1);
        };
    }

    private ItemStack rollShrineGearReward(Player owner) {
        if (plugin.getLootChestManager() == null || ThreadLocalRandom.current().nextDouble() > 0.35) {
            return null;
        }
        int levelRequirement = owner == null ? 1 : Math.max(1, owner.getLevel());
        return plugin.getLootChestManager().getRandomLootForCombatPower(
                SHRINE_REWARD_GEAR_COMBAT_POWER,
                levelRequirement,
                null,
                null,
                false);
    }

    private void grantSimpleBoon(Player player) {
        int roll = ThreadLocalRandom.current().nextInt(3);
        switch (roll) {
            case 0 -> {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 20 * 60, 0, true, true));
                send(player, MessageType.REWARD, "Boon: " + ChatColor.WHITE + "Battle Focus" + ChatColor.GRAY + " (+Strength for 60s)");
            }
            case 1 -> {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 20 * 60, 0, true, true));
                send(player, MessageType.REWARD, "Boon: " + ChatColor.WHITE + "Stone Ward" + ChatColor.GRAY + " (+Resistance for 60s)");
            }
            default -> {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 20 * 60, 0, true, true));
                send(player, MessageType.REWARD, "Boon: " + ChatColor.WHITE + "Windstep" + ChatColor.GRAY + " (+Speed for 60s)");
            }
        }
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
    }

    private void notifyNearby(Location origin, MessageType type, String message) {
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        for (Player player : origin.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(origin) <= 30 * 30) {
                send(player, type, message);
            }
        }
    }

    private void applyShrineMetadata(Entity entity, UUID shrineId) {
        if (entity == null) {
            return;
        }
        entity.setMetadata(SHRINE_ID_META, new FixedMetadataValue(plugin, shrineId.toString()));
    }

    private UUID readShrineId(Entity entity) {
        if (entity == null || !entity.hasMetadata(SHRINE_ID_META)) {
            return null;
        }
        String raw = entity.getMetadata(SHRINE_ID_META).stream()
                .filter(v -> v.getOwningPlugin() == plugin)
                .map(v -> v.asString())
                .findFirst()
                .orElse(null);
        if (raw == null || raw.isBlank() || raw.equals("pending")) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void despawnAnchor(ShrineAnchor anchor) {
        if (anchor == null) {
            return;
        }
        if (anchor.interaction() != null && anchor.interaction().isValid()) anchor.interaction().remove();
        if (anchor.title() != null && anchor.title().isValid()) anchor.title().remove();
        if (anchor.subtitle() != null && anchor.subtitle().isValid()) anchor.subtitle().remove();
        if (anchor.shrineDisplay() != null && anchor.shrineDisplay().isValid()) {
            NexoFurniture.remove(anchor.shrineDisplay());
        }
        if (anchor.entity() != null && anchor.entity().isValid() && !anchor.entity().isDead()) {
            anchor.entity().remove();
        }
    }

    public record ShrineAnchor(UUID id,
                               org.bukkit.entity.ItemDisplay shrineDisplay,
                               LivingEntity entity,
                               org.bukkit.entity.TextDisplay title,
                               org.bukkit.entity.TextDisplay subtitle,
                               Interaction interaction,
                               Location origin,
                               double maxHealth,
                               double zoneRadius) {
        boolean isValid() {
            return shrineDisplay != null && shrineDisplay.isValid() && entity != null && entity.isValid() && !entity.isDead();
        }
    }

    private final class ActiveShrineEvent {
        private final ShrineAnchor anchor;
        private final UUID activator;
        private final int durationSeconds;
        private final List<UUID> spawnedMobs = new ArrayList<>();

        private BukkitTask task;
        private int elapsedTicks;
        private int spawnTicks;

        private ActiveShrineEvent(ShrineAnchor anchor, UUID activator, int durationSeconds) {
            this.anchor = anchor;
            this.activator = activator;
            this.durationSeconds = Math.max(5, durationSeconds);
        }

        private void start() {
            this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                if (!anchor.isValid()) {
                    failShrine(this, "The shrine vanished.");
                    return;
                }
                elapsedTicks++;
                spawnTicks++;

                renderZoneParticles(anchor.origin, anchor.zoneRadius);
                retargetMobsToShrine(anchor.entity);

                if (spawnTicks >= 20) {
                    spawnTicks = 0;
                    spawnPulse(anchor.origin, anchor.zoneRadius, anchor.entity);
                }

                int remaining = Math.max(0, durationSeconds - (elapsedTicks / 20));
                if (remaining % 2 == 0) {
                    anchor.subtitle().setText(ChatColor.GRAY + "Hold for " + ChatColor.WHITE + remaining + "s"
                            + ChatColor.GRAY + " | HP " + ChatColor.RED + (int) Math.ceil(anchor.entity.getHealth()));
                }

                if (elapsedTicks >= durationSeconds * 20) {
                    completeShrine(this);
                }
            }, 1L, 1L);
        }

        private void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            for (UUID mobId : spawnedMobs) {
                Entity entity = plugin.getServer().getEntity(mobId);
                if (entity instanceof LivingEntity living && living.isValid() && !living.isDead()) {
                    living.remove();
                }
            }
            spawnedMobs.clear();
        }

        private void renderZoneParticles(Location center, double radius) {
            SpellEffectUtil.spawnRingParticles(center, radius, Particle.HAPPY_VILLAGER, 28, 0.1);
        }

        private void spawnPulse(Location center, double radius, LivingEntity target) {
            World world = center.getWorld();
            if (world == null) {
                return;
            }
            int spawns = ThreadLocalRandom.current().nextInt(2, 4);
            for (int i = 0; i < spawns; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                Vector offset = new Vector(Math.cos(angle) * (radius + 1.25), 0.0, Math.sin(angle) * (radius + 1.25));
                Location spawn = center.clone().add(offset);
                spawn.setY(Math.max(center.getY(), world.getHighestBlockYAt(spawn) + 1));

                LivingEntity mob = spawnShrineMob(spawn);
                if (mob == null) {
                    continue;
                }
                spawnedMobs.add(mob.getUniqueId());
                if (mob instanceof Mob hostile) {
                    hostile.setTarget(target);
                }
                world.spawnParticle(Particle.SMOKE, spawn, 8, 0.2, 0.2, 0.2, 0.01);
            }
        }

        private LivingEntity spawnShrineMob(Location at) {
            return StrongholdMobSpawnUtil.spawnStrongholdHostile(plugin.getCustomMobManager(), shrineMobPool, at);
        }

        private void retargetMobsToShrine(LivingEntity target) {
            if (target == null || target.isDead()) {
                return;
            }
            for (UUID mobId : spawnedMobs) {
                Entity entity = plugin.getServer().getEntity(mobId);
                if (!(entity instanceof Mob mob) || mob.isDead()) {
                    continue;
                }
                if (mob.getTarget() == null || !mob.getTarget().getUniqueId().equals(target.getUniqueId())) {
                    mob.setTarget(target);
                }
            }
        }
    }
}
