package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.pathfinding.npc.AssassinMercenary;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.utils.MobUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CurrentLocation;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Binds mercenary NPCs to players and controls their behaviour.
 */
public class MercenaryManager implements Listener {
    private final Plugin plugin;
    private final CooldownManager cd = CooldownManager.getInstance();
    /** Player -> (templateId -> follower) */
    private final Map<UUID, Map<Integer, MercenaryFollower>> bindings = new HashMap<>();
    private final Map<UUID, LivingEntity> playerTargets = new HashMap<>();

    public MercenaryManager(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Modes a mercenary can operate in. */
    public enum Mode { HOSTILE, TARGET }

    /** Bind an NPC by id to follow and fight for the player using default assassin profile. */
    public boolean bind(int npcId, Player player) {
        return bind(npcId, player, new AssassinMercenary());
    }

    /** Bind an NPC by id to follow and fight for the player using a custom profile. */
    public boolean bind(int npcId, Player player, PathNpc profile) {
        NPC template = CitizensAPI.getNPCRegistry().getById(npcId);
        if (template == null) return false;

        // Clone the template NPC so the original remains untouched
        NPC clone = template.copy();
        var loc = player.getLocation();
        clone.getOrAddTrait(CurrentLocation.class).setLocation(loc);
        clone.spawn(loc);

        MercenaryFollower follower = new MercenaryFollower(clone, npcId, player, profile);
        bindings.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(npcId, follower);
        follower.start();
        plugin.getLogger().info("[MercenaryDebug] Bound " + profile.name()
                + " using skill '" + profile.primarySkill() + "' for " + player.getName());
        return true;
    }

    /** Unbind a mercenary from a player and remove the spawned copy. */
    public boolean unbind(int npcId, Player player) {
        Map<Integer, MercenaryFollower> map = bindings.get(player.getUniqueId());
        if (map == null) return false;
        MercenaryFollower follower = map.get(npcId);
        if (follower == null) return false;
        follower.unbind();
        return true;
    }

    /** Change a bound mercenary's mode. */
    public boolean setMode(Player player, Mode mode) {
        Map<Integer, MercenaryFollower> map = bindings.get(player.getUniqueId());
        if (map == null) return false;
        for (MercenaryFollower f : map.values()) {
            f.mode = mode;
            f.target = null;
        }
        playerTargets.remove(player.getUniqueId());
        return true;
    }

    public boolean hasMercenary(Player player) {
        return bindings.containsKey(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && event.getEntity() instanceof LivingEntity le) {
            if (CitizensAPI.getNPCRegistry().isNPC(p) || CitizensAPI.getNPCRegistry().isNPC(le) || le instanceof Player || !(le instanceof Monster)) {
                return;
            }
            playerTargets.put(p.getUniqueId(), le);
        }
    }

    private class MercenaryFollower {
        final NPC npc;
        final Player owner;
        final PathNpc profile;
        final int templateId;
        Mode mode = Mode.HOSTILE;
        LivingEntity target;
        BukkitTask task;

        MercenaryFollower(NPC npc, int templateId, Player owner, PathNpc profile) {
            this.npc = npc;
            this.templateId = templateId;
            this.owner = owner;
            this.profile = profile;
        }

        void start() {
            var params = npc.getNavigator().getDefaultParameters();
            params.baseSpeed(params.baseSpeed() * profile.speedMultiplier());
            profile.equip(npc);
            LookClose lc = npc.getOrAddTrait(LookClose.class);
            lc.lookClose(true);
            lc.setRange(10);
            lc.setRandomLook(false);
            lc.setRealisticLooking(true);
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
        }

        void tick() {
            if (!owner.isOnline() || !npc.isSpawned()) {
                unbind();
                return;
            }
            if (target != null) {
                // allow switching to a newly damaged target in TARGET mode
                if (mode == Mode.TARGET) {
                    LivingEntity swap = playerTargets.remove(owner.getUniqueId());
                    if (swap != null && swap instanceof Monster && swap != owner && !CitizensAPI.getNPCRegistry().isNPC(swap)) {
                        target = swap;
                        plugin.getLogger().info("[MercenaryDebug] Switching target to " + swap.getName() + " for " + owner.getName());
                    }
                }

                if (target == owner || target instanceof Player || CitizensAPI.getNPCRegistry().isNPC(target)) {
                    plugin.getLogger().info("[MercenaryDebug] Ignoring player target for " + owner.getName());
                    target = null;
                    moveNearOwner();
                    return;
                }

                if (target.isDead() || !target.isValid()) {
                    plugin.getLogger().info("[MercenaryDebug] Killed all targets in vicinity for " + owner.getName());
                    target = null;
                    moveNearOwner();
                    return;
                }

                npc.getEntity().lookAt(target.getEyeLocation());
                profile.handleCombat(npc, target, cd);
                return;
            }

            if (mode == Mode.TARGET) {
                LivingEntity t = playerTargets.remove(owner.getUniqueId());
                if (t instanceof Monster && t.isValid() && !t.isDead() && t != owner && !CitizensAPI.getNPCRegistry().isNPC(t)) {
                    target = t;
                    plugin.getLogger().info("[MercenaryDebug] Targeting mob " + t.getName() + " for " + owner.getName());
                    profile.handleCombat(npc, target, cd);
                    return;
                }
            } else {
                LivingEntity hostile = MobUtil.findNearestHostile((LivingEntity) npc.getEntity(), 10);
                if (hostile != null) {
                    target = hostile;
                    plugin.getLogger().info("[MercenaryDebug] Targeting mob " + hostile.getName() + " for " + owner.getName());
                    profile.handleCombat(npc, target, cd);
                    return;
                }
            }

            var ownerLoc = owner.getLocation();
            double distSq = npc.getEntity().getLocation().distanceSquared(ownerLoc);
            if (distSq > 400) {
                npc.teleport(ownerLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                plugin.getLogger().info("[MercenaryDebug] Teleported mercenary for " + owner.getName() + " due to distance");
            } else if (distSq < FOLLOW_DIST * FOLLOW_DIST) {
                if (npc.getNavigator().isNavigating()) {
                    npc.getNavigator().cancelNavigation();
                }
            } else if (!npc.getNavigator().isNavigating() || distSq > (FOLLOW_DIST + 1) * (FOLLOW_DIST + 1)) {
                moveNearOwner();
            }
        }

        void unbind() {
            if (task != null) task.cancel();
            if (npc.isSpawned()) npc.despawn();
            npc.destroy();
            Map<Integer, MercenaryFollower> map = bindings.get(owner.getUniqueId());
            if (map != null) {
                map.remove(templateId);
                if (map.isEmpty()) bindings.remove(owner.getUniqueId());
            }
        }

        private static final double FOLLOW_DIST = 5.0;

        private void moveNearOwner() {
            var ownerLoc = owner.getLocation();
            var dir = ownerLoc.getDirection().setY(0).normalize().multiply(-FOLLOW_DIST);
            var dest = ownerLoc.clone().add(dir);
            npc.getNavigator().setTarget(dest);
            plugin.getLogger().info("[MercenaryDebug] Moving near owner for " + owner.getName());
        }
    }
}
