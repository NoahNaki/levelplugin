package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.pathfinding.npc.AssassinMercenary;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.utils.MobUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
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
    private final Map<UUID, MercenaryFollower> bindings = new HashMap<>();
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
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null) return false;
        if (!npc.isSpawned()) {
            npc.spawn(player.getLocation());
        }
        MercenaryFollower follower = new MercenaryFollower(npc, player, profile);
        bindings.put(player.getUniqueId(), follower);
        follower.start();
        return true;
    }

    /** Change a bound mercenary's mode. */
    public boolean setMode(Player player, Mode mode) {
        MercenaryFollower f = bindings.get(player.getUniqueId());
        if (f == null) return false;
        f.mode = mode;
        f.target = null;
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
        Mode mode = Mode.HOSTILE;
        LivingEntity target;
        BukkitTask task;

        MercenaryFollower(NPC npc, Player owner, PathNpc profile) {
            this.npc = npc;
            this.owner = owner;
            this.profile = profile;
        }

        void start() {
            var params = npc.getNavigator().getDefaultParameters();
            params.baseSpeed(params.baseSpeed() * profile.speedMultiplier());
            profile.equip(npc);
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
        }

        void tick() {
            if (!owner.isOnline() || !npc.isSpawned()) {
                unbind();
                return;
            }
            if (target != null) {
                if (target.isDead() || !target.isValid()) {
                    plugin.getLogger().info("[MercenaryDebug] Killed all targets in vicinity for " + owner.getName());
                    target = null;
                    npc.getNavigator().setTarget(owner, true);
                    return;
                }
                profile.handleCombat(npc, target, cd);
                return;
            }

            if (mode == Mode.TARGET) {
                LivingEntity t = playerTargets.remove(owner.getUniqueId());
                if (t != null && t.isValid() && !t.isDead() && t != owner && !CitizensAPI.getNPCRegistry().isNPC(t)) {
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

            double distSq = npc.getEntity().getLocation().distanceSquared(owner.getLocation());
            if (distSq > 400) {
                npc.teleport(owner.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                plugin.getLogger().info("[MercenaryDebug] Teleported mercenary for " + owner.getName() + " due to distance");
            } else if (!npc.getNavigator().isNavigating() || distSq > 9) {
                npc.getNavigator().setTarget(owner, true);
                plugin.getLogger().info("[MercenaryDebug] Moving to owner for " + owner.getName());
            }
        }

        void unbind() {
            if (task != null) task.cancel();
            bindings.remove(owner.getUniqueId());
        }
    }
}
