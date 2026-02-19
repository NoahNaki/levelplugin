package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MageFireballBasicAttackSpell implements SpellHandler {
    private static final List<String> MODEL_CANDIDATES = List.of("fireball", "fireball.bbmodel", "fireball_bbmodel");
    public static final double DEFAULT_FORWARD_OFFSET = 1.1;
    public static final double DEFAULT_VERTICAL_OFFSET = -0.15;

    private static final double SPEED_PER_TICK = 1.15;
    private static final double MAX_RANGE = 30.0;
    private static final double HIT_RADIUS = 0.45;
    private static final double BASE_DAMAGE = 3.0;
    private static final double INTELLIGENCE_SCALE = 0.45;
    private static final double TECHNIQUE_SCALE = 0.001;

    private static final Set<UUID> DEBUG_PLAYERS = ConcurrentHashMap.newKeySet();

    private final Main plugin;

    public MageFireballBasicAttackSpell(Main plugin) {
        this.plugin = plugin;
    }

    public record FireballSpawnResult(ArmorStand anchor,
                                      ModelEngineUtil.ModelApplyResult modelResult,
                                      Location spawnLocation,
                                      Vector direction) {
    }

    public static Location resolveSpawnLocation(Location eyeLocation, Vector rawDirection) {
        if (eyeLocation == null || rawDirection == null || rawDirection.lengthSquared() <= 0.000001) {
            return null;
        }
        Vector direction = rawDirection.clone().normalize();
        Location spawn = eyeLocation.clone().add(direction.clone().multiply(DEFAULT_FORWARD_OFFSET));
        spawn.add(0.0, DEFAULT_VERTICAL_OFFSET, 0.0);
        return spawn;
    }

    public static FireballSpawnResult spawnProjectileAnchor(Main plugin,
                                                            Location eyeLocation,
                                                            Vector rawDirection) {
        if (plugin == null || eyeLocation == null || rawDirection == null || rawDirection.lengthSquared() <= 0.000001) {
            return null;
        }
        Vector direction = rawDirection.clone().normalize();
        Location spawn = resolveSpawnLocation(eyeLocation, direction);
        if (spawn == null || spawn.getWorld() == null) {
            return null;
        }
        ArmorStand projectile = spawn.getWorld().spawn(spawn, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setInvulnerable(true);
        });
        ModelEngineUtil.ModelApplyResult modelResult =
                ModelEngineUtil.applyFirstAvailableModel(projectile, MODEL_CANDIDATES, plugin);
        ModelEngineUtil.orientEntityToVector(projectile, direction);
        return new FireballSpawnResult(projectile, modelResult, spawn, direction);
    }

    public static List<String> modelCandidates() {
        return MODEL_CANDIDATES;
    }

    public static void setDebugEnabled(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return;
        }
        if (enabled) {
            DEBUG_PLAYERS.add(playerId);
        } else {
            DEBUG_PLAYERS.remove(playerId);
        }
    }

    public static boolean isDebugEnabled(UUID playerId) {
        return playerId != null && DEBUG_PLAYERS.contains(playerId);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        boolean debug = isDebugEnabled(caster.getUniqueId());
        Location eye = caster.getEyeLocation().clone();
        Vector direction = eye.getDirection().clone();
        FireballSpawnResult spawnResult = spawnProjectileAnchor(plugin, eye, direction);
        if (spawnResult == null) {
            return;
        }
        ArmorStand projectile = spawnResult.anchor();
        direction = spawnResult.direction();
        ModelEngineUtil.ModelApplyResult modelResult = spawnResult.modelResult();
        if (modelResult.applied().isEmpty()) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Fireball model was not found in ModelEngine. Showing particles only.");
        }
        if (debug) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                    "[FireballDebug] Spawned projectile anchor id=" + projectile.getEntityId()
                            + " marker=" + projectile.isMarker() + " small=" + projectile.isSmall());
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                    "[FireballDebug] Model result applied=" + modelResult.applied()
                            + " failed=" + modelResult.failed() + " blueprintOnly=" + modelResult.blueprintOnly());
        }
        launchProjectile(caster, projectile, direction, debug);
    }

    private void launchProjectile(Player caster, ArmorStand projectile, Vector direction, boolean debug) {
        Vector step = direction.clone().normalize().multiply(SPEED_PER_TICK);
        double maxDistanceSq = MAX_RANGE * MAX_RANGE;
        Location origin = projectile.getLocation().clone();
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!projectile.isValid() || !caster.isOnline()) {
                    if (debug) {
                        ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                                "[FireballDebug] Projectile removed early: valid=" + projectile.isValid()
                                        + " online=" + caster.isOnline());
                    }
                    removeProjectile();
                    cancel();
                    return;
                }

                Location current = projectile.getLocation();
                if (current.distanceSquared(origin) >= maxDistanceSq) {
                    if (debug) {
                        ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                                "[FireballDebug] Projectile expired at max range.");
                    }
                    removeProjectile();
                    cancel();
                    return;
                }

                Location next = current.clone().add(step);
                projectile.teleport(next);
                ModelEngineUtil.orientEntityToVector(projectile, step);

                LivingEntity target = findTargetAt(next, caster, projectile);
                if (target != null) {
                    if (debug) {
                        ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                                "[FireballDebug] Entity hit at " + format(next)
                                        + " target=" + target.getType().name() + " tick=" + ticks);
                    }
                    onImpact(caster, next, target);
                    removeProjectile();
                    cancel();
                    return;
                }

                ticks++;
                if (debug && ticks % 10 == 0) {
                    ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                            "[FireballDebug] Traveling tick=" + ticks + " loc=" + format(next));
                }
            }

            private void removeProjectile() {
                if (projectile.isValid()) {
                    projectile.remove();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private LivingEntity findTargetAt(Location center, Player caster, ArmorStand projectile) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        double radius = HIT_RADIUS;
        for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.isDead() || living.equals(caster) || living.equals(projectile)) {
                continue;
            }
            if (living instanceof ArmorStand) {
                continue;
            }
            return living;
        }
        return null;
    }

    private void onImpact(Player caster, Location impact, LivingEntity target) {
        World world = impact.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(impact, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.25f);

        if (target != null) {
            double damage = SpellEffectUtil.computeIntTecScaledDamage(caster, BASE_DAMAGE, INTELLIGENCE_SCALE, TECHNIQUE_SCALE);
            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage);
            if (isDebugEnabled(caster.getUniqueId())) {
                ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.INFO,
                        "[FireballDebug] Applied INT/TEC damage=" + String.format("%.2f", damage));
            }
        }
    }

    private String format(Location location) {
        return String.format("%.2f %.2f %.2f", location.getX(), location.getY(), location.getZ());
    }
}
