package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RogueSmokeBombSpell implements SpellHandler, Listener {
    private static boolean listenerRegistered;
    private static final Map<UUID, Item> activeBombs = new HashMap<>();
    private static final Map<UUID, BukkitTask> bombTasks = new HashMap<>();
    private static final Map<UUID, Set<UUID>> bombsByOwner = new HashMap<>();

    private final Main plugin;
    private final int durationTicks;
    private final double stunRadius;
    private final int stunTicks;
    private final int bombCount;
    private final double coneDegrees;
    private final double dotDamagePerTick;
    private final int dotPeriodTicks;

    public RogueSmokeBombSpell(Main plugin, int durationTicks, double stunRadius, int stunTicks) {
        this(plugin, durationTicks, stunRadius, stunTicks, 1, 0.0, 0.0, 20);
    }

    public RogueSmokeBombSpell(Main plugin,
                               int durationTicks,
                               double stunRadius,
                               int stunTicks,
                               int bombCount,
                               double coneDegrees,
                               double dotDamagePerTick,
                               int dotPeriodTicks) {
        this.plugin = plugin;
        this.durationTicks = Math.max(20, durationTicks);
        this.stunRadius = Math.max(1.0, stunRadius);
        this.stunTicks = Math.max(1, stunTicks);
        this.bombCount = Math.max(1, bombCount);
        this.coneDegrees = Math.max(0.0, coneDegrees);
        this.dotDamagePerTick = Math.max(0.0, dotDamagePerTick);
        this.dotPeriodTicks = Math.max(1, dotPeriodTicks);
        if (!listenerRegistered) {
            this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
            listenerRegistered = true;
        }
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        UUID casterId = caster.getUniqueId();
        cleanupOwnerBombs(casterId);

        Vector forward = caster.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            forward = new Vector(0.0, 0.0, 1.0);
        }
        forward.normalize();

        for (int i = 0; i < bombCount; i++) {
            double yawOffset = computeYawOffset(i);
            Vector launchDirection = rotateAroundY(forward.clone(), yawOffset).normalize();
            Location spawnLocation = caster.getLocation().clone()
                    .add(launchDirection.clone().multiply(0.65))
                    .add(0.0, 0.2, 0.0);
            spawnBombForCaster(caster, casterId, launchDirection, spawnLocation);
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.6f, 0.75f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.8f, 0.9f);
    }

    private void spawnBombForCaster(Player caster, UUID casterId, Vector launchDirection, Location spawnLocation) {
        Item bomb = caster.getWorld().dropItem(spawnLocation, new ItemStack(Material.WITHER_SKELETON_SKULL));
        bomb.setPickupDelay(Integer.MAX_VALUE);
        bomb.setCanMobPickup(false);
        bomb.setUnlimitedLifetime(false);
        bomb.setVelocity(launchDirection.clone().multiply(0.34).setY(0.18));

        UUID bombId = bomb.getUniqueId();
        activeBombs.put(bombId, bomb);
        bombsByOwner.computeIfAbsent(casterId, key -> new HashSet<>()).add(bombId);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            private int elapsed;
            private int dotElapsed;

            @Override
            public void run() {
                if (!bomb.isValid() || bomb.isDead() || elapsed >= durationTicks) {
                    cleanupBomb(casterId, bombId, true);
                    return;
                }

                Location center = bomb.getLocation().clone().add(0.0, 0.18, 0.0);
                center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 22, 0.82, 0.36, 0.82, 0.002);
                center.getWorld().spawnParticle(Particle.SMOKE, center, 12, 0.72, 0.28, 0.72, 0.002);
                center.getWorld().spawnParticle(Particle.DUST, center, 8, 0.66, 0.18, 0.66,
                        new Particle.DustOptions(Color.fromRGB(20, 20, 20), 1.3f));

                for (LivingEntity living : SpellEffectUtil.getLivingTargets(center, stunRadius,
                        target -> !target.equals(caster) && !(target instanceof Player))) {
                    SpellEffectUtil.applyStun(living, stunTicks);
                    if (dotDamagePerTick > 0.0 && dotElapsed <= 0) {
                        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, living, dotDamagePerTick, true);
                    }
                }
                elapsed += 2;
                dotElapsed += 2;
                if (dotElapsed >= dotPeriodTicks) {
                    dotElapsed = 0;
                }
            }
        }, 0L, 2L);
        bombTasks.put(bombId, task);
    }

    private double computeYawOffset(int index) {
        if (bombCount <= 1 || coneDegrees <= 0.0) {
            return 0.0;
        }
        double step = coneDegrees / Math.max(1, bombCount - 1);
        return (-coneDegrees / 2.0) + (step * index);
    }

    private Vector rotateAroundY(Vector vector, double degrees) {
        return vector.rotateAroundY(Math.toRadians(degrees));
    }

    @EventHandler
    public void onBombPickup(EntityPickupItemEvent event) {
        if (activeBombs.containsKey(event.getItem().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onOwnerQuit(PlayerQuitEvent event) {
        cleanupOwnerBombs(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            cleanupAllBombs();
        }
    }

    private void cleanupOwnerBombs(UUID ownerId) {
        Set<UUID> ownerBombs = bombsByOwner.remove(ownerId);
        if (ownerBombs == null || ownerBombs.isEmpty()) {
            return;
        }
        for (UUID bombId : new HashSet<>(ownerBombs)) {
            cleanupBomb(ownerId, bombId, true);
        }
    }

    private void cleanupAllBombs() {
        for (UUID ownerId : new HashSet<>(bombsByOwner.keySet())) {
            cleanupOwnerBombs(ownerId);
        }
        activeBombs.clear();
        bombTasks.clear();
        bombsByOwner.clear();
    }

    private void cleanupBomb(UUID ownerId, UUID bombId, boolean removeEntity) {
        BukkitTask task = bombTasks.remove(bombId);
        if (task != null) {
            task.cancel();
        }

        Item bomb = activeBombs.remove(bombId);
        if (removeEntity && bomb != null && bomb.isValid()) {
            bomb.remove();
        }

        Set<UUID> ownerBombs = bombsByOwner.get(ownerId);
        if (ownerBombs == null) {
            return;
        }
        ownerBombs.remove(bombId);
        if (ownerBombs.isEmpty()) {
            bombsByOwner.remove(ownerId);
        }
    }
}
