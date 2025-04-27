package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.utils.MetadataTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class RogueSpell implements Listener {

    public boolean castRogueSpell(Player player, String effectKey) {
        switch (effectKey.toUpperCase()) {
            case "ENDLESS_ASSAULT":
                return castExecute(player);
            case "BLADE_FURY":
                castBladeFury(player);
                return true;
            case "VANISH":
                castVanish(player);
                return true;
            case "SHADOW_CLONE":
                castShadowClone(player);
                return true;
            default:
                player.sendMessage("§eUnknown Rogue Spell: " + effectKey);
                return false;
        }
    }


    private void castShurikenThrow(Player player) {
        // 1) compute damage once
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.3;

        // 2) setup
        Location center = player.getLocation().clone();
        Vector forwardVelocity = center.getDirection().multiply(1.3);
        List<ArmorStand> stands = new ArrayList<>();
        double radius = 0.2;
        Vector[] offsets = {
            new Vector(radius, 0, 0), new Vector(-radius, 0, 0),
            new Vector(0, 0, radius), new Vector(0, 0, -radius)
        };

        // 3) spawn the rotating “shuriken” armor stands
        for (Vector offset : offsets) {
            Location loc = center.clone().add(offset);
            ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setBasePlate(false);
            stand.setGravity(false);
            stand.setArms(true);
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            stand.setRightArmPose(new EulerAngle(Math.toRadians(270), 0, Math.toRadians(90)));
            stand.setLeftArmPose(new EulerAngle(Math.toRadians(270), 0, Math.toRadians(-90)));
            stands.add(stand);
        }

        // 4) animate & detect impact
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;

            @Override
            public void run() {
                if (ticks++ >= 60) {
                    player.sendMessage("§eYour shuriken dissipated!");
                    removeAll(stands);
                    cancel();
                    return;
                }

                angle += Math.toRadians(45);
                center.add(forwardVelocity);

                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand stand = stands.get(i);
                    Vector off = offsets[i].clone();
                    double cos = Math.cos(angle), sin = Math.sin(angle);
                    double x = off.getX() * cos - off.getZ() * sin;
                    double z = off.getX() * sin + off.getZ() * cos;
                    Location loc = center.clone().add(x, 0, z);

                    // 4a) block collision → teleport player there
                    Block block = loc.getBlock();
                    if (block.getType() != Material.AIR && !block.isPassable()) {
                        player.teleport(loc);
                        player.sendMessage("§aYou teleported to the shuriken’s location!");
                        removeAll(stands);
                        cancel();
                        return;
                    }

                    // 4b) entity collision → deal damage + chat, then explode
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.3, 0.3, 0.3)) {
                        if (e.equals(player) || stands.contains(e)) continue;
                        if (e instanceof LivingEntity le) {
                            // —— damage + chat here ——
                            SpellUtils.dealWithChat(player, le, damage, "Shuriken Throw");

                            // then explosion & cleanup
                            loc.getWorld().createExplosion(loc, 2f, false, false);
                            player.sendMessage("§cYour shuriken exploded on impact!");
                            removeAll(stands);
                            cancel();
                            return;
                        }
                    }

                    // 4c) move the stand
                    loc.setYaw((float) Math.toDegrees(Math.atan2(-x, z)));
                    stand.teleport(loc);
                }
            }

            private void removeAll(List<ArmorStand> list) {
                list.forEach(s -> { if (!s.isDead()) s.remove(); });
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }


    private void castShadowClone(Player player) {
        UUID id = player.getUniqueId();

        // 1) Swap with existing clone if there is one
        NPC existing = null;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(MetadataTrait.class)
                && npc.getTrait(MetadataTrait.class).getOwner().equals(id)) {
                existing = npc;
                break;
            }
        }
        if (existing != null) {
            Location cloneLoc = existing.getEntity().getLocation();
            existing.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.teleport(cloneLoc);
            player.sendMessage("§aYou swapped places with your shadow clone!");
            player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT,
                1f, 1f
            );
            return;
        }

        // 2) Spawn a brand-new clone
        NPC clone = CitizensAPI.getNPCRegistry()
            .createNPC(EntityType.PLAYER, "Shadow Clone");
        clone.spawn(player.getLocation());
        clone.getOrAddTrait(MetadataTrait.class).setOwner(id);
        clone.data().setPersistent("player-skin-name", player.getName());
        if (clone.getEntity() instanceof Player p) {
            p.getInventory().setArmorContents(player.getInventory().getArmorContents());
            p.getInventory().setItemInMainHand(player.getInventory().getItemInMainHand());
            p.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());
        }
        player.sendMessage("§aYou created a shadow clone!");

        // 3) Schedule its “explosion” in 5s
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!clone.isSpawned()) return;

                // a) Grab location, then kill the NPC
                Location loc = clone.getEntity().getLocation();
                clone.despawn();
                clone.destroy();

                // b) Visual + sound effect ONLY
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
                loc.getWorld().playSound(
                    loc,
                    Sound.ENTITY_GENERIC_EXPLODE,
                    1f, 1f
                );
                player.sendMessage("§cYour shadow clone exploded!");

                // c) Now *manually* damage every nearby target and log it
                double damage = player
                    .getAttribute(Attribute.ATTACK_DAMAGE)
                    .getValue() * 1.5;

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                    if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                    SpellUtils.dealWithChat(player, le, damage, "Shadow Clone");
                }
            }
        }.runTaskLater(
            Bukkit.getPluginManager().getPlugin("LevelPlugin"),
            100L
        );
    }



    private void castShadowStep(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 30, 0.5, 1, 0.5);

        new BukkitRunnable() {
            int casts = 0;

            @Override
            public void run() {
                if (casts++ >= 5) { cancel(); return; }

                LivingEntity target = player.getWorld().getNearbyEntities(player.getLocation(), 15, 15, 15).stream()
                    .filter(e -> e instanceof LivingEntity && e != player)
                    .map(e -> (LivingEntity) e)
                    .filter(e -> !(e instanceof Player) || DuelManager.getInstance().areInDuel(player.getUniqueId(), ((Player) e).getUniqueId()))
                    .min(Comparator.comparingDouble(e -> e.getLocation().distance(player.getLocation())))
                    .orElse(null);

                if (target != null) {
                    Location behind = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1).normalize());
                    behind.setYaw(target.getLocation().getYaw());
                    behind.setPitch(target.getLocation().getPitch());
                    player.teleport(behind);

                    double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;
                    SpellUtils.dealWithChat(player, target, damage, "Shadow Step");

                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 30, 0.5, 1, 0.5);
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation(), 10, 0.5, 0.5, 0.5);
                    target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 20, 0.5, 1, 0.5);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.2f);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

                    if (target instanceof Player) {
                        ((Player) target).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
                    }
                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 10, 0.3, 0.3, 0.3);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0));
                }

                player.getWorld().spawnParticle(Particle.ASH, player.getLocation(), 10, 0.3, 0.3, 0.3);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0, 10);
    }

    private void castBladeFury(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 30, radius, 1, radius);

        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (e instanceof LivingEntity le && le != player) {
                SpellUtils.dealWithChat(player, le, damage, "Blade Fury");
            }
        }
    }

    private static final Set<UUID> vanishedPlayers = new HashSet<>();
    private static final Map<UUID, BukkitRunnable> vanishTasks = new HashMap<>();


    private void castVanish(Player player) {
        Location origin = player.getLocation();
        World world = origin.getWorld();

        final int BASE_DURATION   = 100;  // 5s
        final int EXTEND_DURATION = 20;   // +2s per cast while already invisible

        // 1) compute new duration (extend if already invisible)
        int newDuration = BASE_DURATION;
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            PotionEffect old = player.getPotionEffect(PotionEffectType.INVISIBILITY);
            newDuration = old.getDuration() + EXTEND_DURATION;
        }

        // 2) determine agility‐based buffs
        int totalAgility = StatsManager
            .getInstance()
            .getStatValue(player, StatsManager.StatType.AGI);

        boolean applySpeed = false, applyJump = false;
        int speedAmp = 0, jumpAmp = 0;

        if (totalAgility > 500) {
            applySpeed = true; speedAmp = 1;   // Speed II
            applyJump  = true; jumpAmp  = 1;   // Jump Boost II
        } else if (totalAgility > 250) {
            applySpeed = true; speedAmp = 0;   // Speed I
            applyJump  = true; jumpAmp  = 0;   // Jump Boost I
        } else if (totalAgility > 100) {
            applySpeed = true; speedAmp = 0;   // Speed I
            // no jump yet
        }
        // (<100: invis only)

        // 3) schedule end‐of‐vanish cleanup
        BukkitRunnable vanishEnd = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                vanishedPlayers.remove(player.getUniqueId());
                for (Player other : Bukkit.getOnlinePlayers())
                    if (!other.equals(player))
                        other.showPlayer(Main.getInstance(), player);
            }
        };
        vanishEnd.runTaskLater(Main.getInstance(), newDuration);
        vanishTasks.put(player.getUniqueId(), vanishEnd);

        // 4) apply potion effects
        player.addPotionEffect(
            new PotionEffect(PotionEffectType.INVISIBILITY, newDuration, 0, false, false)
        );
        if (applySpeed) {
            player.addPotionEffect(
                new PotionEffect(PotionEffectType.SPEED, newDuration, speedAmp, false, false)
            );
        }
        if (applyJump) {
            player.addPotionEffect(
                new PotionEffect(PotionEffectType.JUMP_BOOST, newDuration, jumpAmp, false, false)
            );
        }

        // 5) hide player from others
        vanishedPlayers.add(player.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers())
            if (!other.equals(player))
                other.hidePlayer(Main.getInstance(), player);

        // 6) visual/audio feedback
        world.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        world.playSound(origin, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.2f);
        world.playSound(origin, Sound.ITEM_TOTEM_USE, 0.8f, 1f);
        world.spawnParticle(Particle.FIREWORK, origin, 30, 1, 1, 1, 0.05);
        world.spawnParticle(Particle.SMOKE,    origin, 20, 0.5, 1, 0.5, 0.02);
    }



    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (vanishedPlayers.contains(damager.getUniqueId())) {
                cancelVanish(damager);
            }
        }

        if (event.getEntity() instanceof Player damaged) {
            if (vanishedPlayers.contains(damaged.getUniqueId())) {
                cancelVanish(damaged);
            }
        }
    }

    private void cancelVanish(Player player) {
        vanishedPlayers.remove(player.getUniqueId());

        // Cancel the scheduled vanish end task
        BukkitRunnable task = vanishTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.showPlayer(Main.getInstance(), player);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 10, 0.5, 0.5, 0.5);

        player.sendMessage("§cYour vanish was broken!");
    }


    private boolean castExecute(Player player) {
        // 1) find the first valid target
        LivingEntity found = null;
        for (Entity e : player.getWorld()
            .getNearbyEntities(player.getEyeLocation(), 10, 10, 10)) {
            if (!(e instanceof LivingEntity le) || le.equals(player)) continue;

            // only allow hitting another Player if you're in a duel with them
            if (le instanceof Player tgt
                && !DuelManager.getInstance()
                .areInDuel(player.getUniqueId(), tgt.getUniqueId())) {
                continue;
            }

            found = le;
            break;
        }

        if (found == null) {
            player.sendMessage("§cNo valid target in range!");
            return false;  // no target → no mana consumed
        }

        final LivingEntity target = found;
        final World world = target.getWorld();
        final double damage = player.getAttribute(Attribute.ATTACK_DAMAGE)
            .getValue() * 1.5;

        // 2) prepare strike directions (8 around + above + below)
        final Vector[] directions = new Vector[10];
        int idx = 0;
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            directions[idx++] = new Vector(Math.cos(angle), 0.2, Math.sin(angle));
        }
        directions[idx++] = new Vector(0, -1, 0);  // above
        directions[idx++] = new Vector(0, 1, 0);   // below

        // 3) schedule the stacked strikes
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= directions.length) {
                    SpellUtils.dealWithChat(player, target, damage, "Execute");
                    cancel();
                    return;
                }
                target.setVelocity(directions[step].clone().multiply(1.2));
                world.spawnParticle(Particle.CRIT, target.getLocation(), 15, 0.5, 1, 0.5);
                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                step++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L);

        player.sendMessage("§aYou unleash a thousand cuts upon your foe!");
        return true;  // success → mana *should* be consumed
    }
}
