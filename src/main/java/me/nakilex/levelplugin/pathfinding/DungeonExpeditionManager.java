package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.verified.CrimsonReliquaryDungeon;
import me.nakilex.levelplugin.pathfinding.npc.ArcherMercenary;
import me.nakilex.levelplugin.pathfinding.npc.MageMercenary;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.pathfinding.npc.RogueMercenary;
import me.nakilex.levelplugin.pathfinding.npc.WarriorMercenary;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DungeonExpeditionManager implements Listener {
    private static final String RELIQUARY_KEY = "crimson reliquary";
    private final Plugin plugin;
    private final DungeonManager dungeonManager;
    private final Map<UUID, ExpeditionRun> activeRuns = new HashMap<>();

    public DungeonExpeditionManager(Plugin plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void startCrimsonReliquaryExpedition(Player player) {
        if (player == null) {
            return;
        }
        if (activeRuns.containsKey(player.getUniqueId())) {
            ChatMessageUtil.send(player, MessageType.WARNING, "A dungeon expedition is already running.");
            return;
        }

        CrimsonReliquaryDungeon reliquary = resolveReliquary();
        if (reliquary == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Crimson Reliquary is not registered.");
            return;
        }

        dungeonManager.startInstance(player, reliquary.getKey());
        ChatMessageUtil.send(player, MessageType.INFO, "Starting Crimson Reliquary expedition...");

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                attempts++;
                World world = player.getWorld();
                if (!dungeonManager.isInstanceWorld(world)) {
                    return;
                }
                String layout = dungeonManager.getInstanceLayout(world);
                if (layout == null || !layout.equalsIgnoreCase(reliquary.getKey())) {
                    return;
                }
                var routeOpt = reliquary.getExpeditionRoute(world);
                if (routeOpt.isEmpty()) {
                    if (attempts % 20 == 0) {
                        plugin.getLogger().info("[DungeonExpedition] Waiting for instance to finish loading...");
                    }
                    return;
                }
                spawnMercenaries(player, routeOpt.get());
                cancel();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void spawnMercenaries(Player player, CrimsonReliquaryDungeon.ExpeditionRoute route) {
        if (route.path().size() < 2) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Expedition route is missing path markers.");
            return;
        }

        List<PathNpc> squad = List.of(
                new WarriorMercenary(),
                new RogueMercenary(),
                new MageMercenary(),
                new ArcherMercenary()
        );

        List<PathFollower> followers = new ArrayList<>();
        double spacing = 0.8;
        for (int i = 0; i < squad.size(); i++) {
            PathNpc profile = squad.get(i);
            double offsetX = (i % 2 == 0 ? -spacing : spacing);
            double offsetZ = (i / 2 == 0 ? -spacing : spacing);
            List<Location> path = offsetPath(route.path(), offsetX, offsetZ);
            NPC npc = CitizensAPI.getNPCRegistry().createNPC(profile.type(), profile.name());
            PathFollower follower = new PathFollower(plugin, npc, path, profile, false, null);
            follower.start();
            followers.add(follower);
        }

        activeRuns.put(player.getUniqueId(), new ExpeditionRun(route.spawn().getWorld(), followers));
        ChatMessageUtil.send(player, MessageType.SUCCESS, "Mercenaries deployed. They will follow the wool markers.");
    }

    private List<Location> offsetPath(List<Location> path, double offsetX, double offsetZ) {
        List<Location> offset = new ArrayList<>(path.size());
        for (Location point : path) {
            offset.add(point.clone().add(offsetX, 0, offsetZ));
        }
        return offset;
    }

    private CrimsonReliquaryDungeon resolveReliquary() {
        return dungeonManager.getVerifiedDefinition(RELIQUARY_KEY) instanceof CrimsonReliquaryDungeon reliquary
                ? reliquary
                : null;
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (!event.getEntity().getScoreboardTags().contains("dungeon_boss")) {
            return;
        }
        World world = event.getEntity().getWorld();
        activeRuns.entrySet().removeIf(entry -> {
            ExpeditionRun run = entry.getValue();
            if (!run.world.equals(world)) {
                return false;
            }
            run.cleanup();
            return true;
        });
    }

    private static final class ExpeditionRun {
        private final World world;
        private final List<PathFollower> followers;

        private ExpeditionRun(World world, List<PathFollower> followers) {
            this.world = world;
            this.followers = followers;
        }

        private void cleanup() {
            for (PathFollower follower : followers) {
                follower.stop();
            }
        }
    }
}
