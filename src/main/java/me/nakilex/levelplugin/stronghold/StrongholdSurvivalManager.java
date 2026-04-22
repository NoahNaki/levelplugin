package me.nakilex.levelplugin.stronghold;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class StrongholdSurvivalManager implements Listener {
    public record StageStatus(int wave, int mobsRemaining, int secondsLeft, String objective) {}

    private final Main plugin;
    private final Map<UUID, RunState> activeRuns = new HashMap<>();

    public StrongholdSurvivalManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public StageStatus getStage(UUID playerId) {
        RunState run = activeRuns.get(playerId);
        if (run == null || !run.active) {
            return null;
        }
        int left = (int) Math.max(0, (run.waveDeadlineMs - System.currentTimeMillis()) / 1000L);
        return new StageStatus(run.wave, run.trackedMobs.size(), left, run.objective);
    }

    public boolean isInRun(UUID playerId) {
        return activeRuns.containsKey(playerId);
    }

    public void startRun(Collection<UUID> members, World world, Location center) {
        if (world == null || center == null || members == null || members.isEmpty()) {
            return;
        }
        RunState run = new RunState(world, center);
        run.active = true;
        run.objective = "Survive the assault";
        for (UUID id : members) {
            activeRuns.put(id, run);
        }

        configureBorder(world, center);
        startWave(run);
        for (UUID id : members) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        ChatColor.GOLD + "Stronghold run started. " + ChatColor.GRAY + run.objective + ".");
            }
        }
    }

    public void addKey(UUID playerId, int amount) {
        RunState run = activeRuns.get(playerId);
        if (run == null || amount <= 0) return;
        run.keysByPlayer.merge(playerId, amount, Integer::sum);
    }

    public int consumeKey(UUID playerId, int amount) {
        RunState run = activeRuns.get(playerId);
        if (run == null || amount <= 0) return 0;
        int current = run.keysByPlayer.getOrDefault(playerId, 0);
        int consumed = Math.min(current, amount);
        if (consumed > 0) {
            run.keysByPlayer.put(playerId, current - consumed);
        }
        return consumed;
    }

    public int getKeys(UUID playerId) {
        RunState run = activeRuns.get(playerId);
        return run == null ? 0 : run.keysByPlayer.getOrDefault(playerId, 0);
    }

    public void exit(UUID playerId) {
        RunState run = activeRuns.remove(playerId);
        if (run == null) return;
        if (activeRuns.values().stream().noneMatch(v -> v == run)) {
            run.shutdown();
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        for (RunState run : new HashSet<>(activeRuns.values())) {
            if (!run.active || !run.trackedMobs.remove(id)) {
                continue;
            }
            if (run.trackedMobs.isEmpty()) {
                if (run.wave >= 10) {
                    completeRun(run);
                } else {
                    startWave(run);
                }
            }
            break;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        exit(event.getPlayer().getUniqueId());
    }

    private void completeRun(RunState run) {
        run.shutdown();
        for (UUID id : new ArrayList<>(activeRuns.keySet())) {
            if (activeRuns.get(id) == run) {
                activeRuns.remove(id);
                Player player = Bukkit.getPlayer(id);
                if (player != null && player.isOnline()) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                            "Stronghold complete! You survived all waves.");
                }
            }
        }
    }

    private void failRun(RunState run) {
        run.shutdown();
        for (UUID id : new ArrayList<>(activeRuns.keySet())) {
            if (activeRuns.get(id) == run) {
                activeRuns.remove(id);
                Player player = Bukkit.getPlayer(id);
                if (player != null && player.isOnline()) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "Stronghold failed: the wave timer ran out.");
                }
            }
        }
    }

    private void startWave(RunState run) {
        run.wave++;
        run.trackedMobs.clear();
        run.waveDeadlineMs = System.currentTimeMillis() + 60_000L;
        int mobCount = 4 + (run.wave * 2);
        for (int i = 0; i < mobCount; i++) {
            Location spawn = run.center.clone().add((i % 4) - 1.5, 1, (i / 4) - 1.5);
            LivingEntity mob = run.world.spawn(spawn, Zombie.class, z -> z.setCustomName(ChatColor.DARK_RED + "Stronghold Foe"));
            mob.addScoreboardTag("stronghold_mob");
            run.trackedMobs.add(mob.getUniqueId());
        }
        if (run.timer != null) {
            run.timer.cancel();
        }
        run.timer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!run.active) {
                    cancel();
                    return;
                }
                if (System.currentTimeMillis() >= run.waveDeadlineMs) {
                    failRun(run);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void configureBorder(World world, Location center) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        border.setSize(320.0);
        border.setWarningDistance(12);
    }

    private static final class RunState {
        private final World world;
        private final Location center;
        private final Set<UUID> trackedMobs = new HashSet<>();
        private final Map<UUID, Integer> keysByPlayer = new HashMap<>();
        private BukkitTask timer;
        private boolean active;
        private int wave;
        private long waveDeadlineMs;
        private String objective;

        private RunState(World world, Location center) {
            this.world = world;
            this.center = center;
        }

        private void shutdown() {
            active = false;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }
}
