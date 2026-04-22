package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lightweight wave survival runtime for stronghold debug worlds.
 * <p>
 * The manager intentionally reuses existing systems:
 * <ul>
 *     <li>CustomMobManager for encounter spawning.</li>
 *     <li>StatsManager for mana recovery rewards.</li>
 *     <li>ChatMessageUtil for consistent UX messaging.</li>
 * </ul>
 */
public final class StrongholdSurvivalManager implements Listener {
    private static final String WAVE_TAG = "stronghold_wave_mob";
    private static final int FINAL_WAVE = 30;
    private static final int BASE_WAVE_SECONDS = 50;
    private static final double BORDER_INITIAL_SIZE = 220.0;
    private static final double BORDER_MIN_SIZE = 42.0;
    private static final double BORDER_SHRINK_PER_WAVE = 5.5;
    private static final int BORDER_WARNING_DISTANCE = 12;

    private static final List<String> EARLY_POOL = List.of(
            "rpg_rat", "wild_rooster", "forest_slime", "moss_zombie", "goblin_warrior", "goblin_archer"
    );
    private static final List<String> MID_POOL = List.of(
            "cave_stalker", "crypt_skeleton", "goblin_assassin", "goblin_shaman", "desert_skirmisher", "sand_hexer"
    );
    private static final List<String> LATE_POOL = List.of(
            "cursed_archer", "cursed_mage", "cursed_knight", "frost_rager", "ice_channeler", "burrow_warden"
    );
    private static final List<String> ELITE_POOL = List.of(
            "reliquary_giant", "vp1_golem_damaged_1", "glacier_tyrant", "rift_warden"
    );
    private static final List<String> BOSS_POOL = List.of(
            "astral_devourer", "void_reaver", "slime_king"
    );

    public record StageStatus(int wave, int mobsRemaining, int secondsLeft) {
    }

    private final Main plugin;
    private final Map<UUID, Run> runsByPlayer = new HashMap<>();
    private final Map<UUID, Run> runsByWorld = new HashMap<>();
    private final Map<UUID, UUID> mobToOwner = new HashMap<>();

    public StrongholdSurvivalManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public StageStatus getStage(UUID playerId) {
        Run run = runsByPlayer.get(playerId);
        if (run == null || !run.active) {
            return null;
        }
        int seconds = (int) Math.max(0L, (run.waveDeadlineMs - System.currentTimeMillis()) / 1000L);
        return new StageStatus(run.wave, run.mobsRemaining, seconds);
    }

    public boolean isActive(UUID playerId) {
        return playerId != null && runsByPlayer.containsKey(playerId);
    }

    public void startRun(Player player) {
        if (player == null) {
            return;
        }
        World world = player.getWorld();
        if (!isStrongholdWorld(world)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Stronghold survival can only start inside a stronghold world.");
            return;
        }
        stopRun(player.getUniqueId(), true);
        Run run = new Run(player.getUniqueId(), world.getUID());
        runsByPlayer.put(player.getUniqueId(), run);
        runsByWorld.put(world.getUID(), run);
        initializeRunBorder(run, player);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.GRAY + "Objective: survive all " + ChatColor.GOLD + FINAL_WAVE
                        + ChatColor.GRAY + " waves and defeat the final boss.");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.GRAY + "The stronghold perimeter will " + ChatColor.RED + "collapse"
                        + ChatColor.GRAY + " as waves progress.");
        beginWave(run, false);
    }

    public void stopRun(UUID playerId, boolean silent) {
        Run run = runsByPlayer.remove(playerId);
        if (run == null) {
            return;
        }
        runsByWorld.remove(run.worldId);
        run.active = false;
        if (run.waveTask != null) {
            run.waveTask.cancel();
        }
        if (run.bossBar != null) {
            run.bossBar.removeAll();
        }
        restoreRunBorder(run);
        for (UUID mobId : new HashSet<>(run.mobIds)) {
            mobToOwner.remove(mobId);
            var entity = Bukkit.getEntity(mobId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        run.mobIds.clear();
        Player player = Bukkit.getPlayer(playerId);
        if (!silent && player != null && player.isOnline()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Stronghold survival ended.");
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        UUID mobId = event.getEntity().getUniqueId();
        UUID ownerId = mobToOwner.remove(mobId);
        if (ownerId == null) {
            return;
        }
        Run run = runsByPlayer.get(ownerId);
        if (run == null || !run.active) {
            return;
        }
        run.mobIds.remove(mobId);
        run.mobsRemaining = Math.max(0, run.mobsRemaining - 1);
        updateBossBar(run);
        if (run.mobsRemaining <= 0) {
            completeWave(run);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopRun(event.getPlayer().getUniqueId(), true);
    }

    private void beginWave(Run run, boolean announceBuff) {
        Player player = Bukkit.getPlayer(run.playerId);
        if (player == null || !player.isOnline()) {
            stopRun(run.playerId, true);
            return;
        }
        if (!isStrongholdWorld(player.getWorld())) {
            stopRun(run.playerId, true);
            return;
        }
        run.wave++;
        if (run.wave > FINAL_WAVE) {
            finishRun(run);
            return;
        }
        run.mobIds.clear();
        run.mobsRemaining = spawnWaveMobs(run, player);
        run.waveDeadlineMs = System.currentTimeMillis() + (BASE_WAVE_SECONDS * 1000L);
        applyWaveBorder(run);
        if (run.bossBar == null) {
            run.bossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        }
        run.bossBar.addPlayer(player);
        if (run.wave >= FINAL_WAVE) {
            run.bossBar.setColor(BarColor.RED);
        } else if (isEliteWave(run.wave)) {
            run.bossBar.setColor(BarColor.PURPLE);
        } else {
            run.bossBar.setColor(BarColor.BLUE);
        }
        updateBossBar(run);
        player.sendTitle(
                ChatColor.GOLD + "" + ChatColor.BOLD + "Wave " + run.wave,
                ChatColor.GRAY + (isEliteWave(run.wave) ? "Elite encounter" : "Defeat all enemies"),
                8, 30, 10
        );
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
        if (announceBuff && run.lastBuff != null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, run.lastBuff.message);
        }
        if (run.waveTask != null) {
            run.waveTask.cancel();
        }
        run.waveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickWave(run), 20L, 20L);
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateBoard(player);
        }
    }

    private void tickWave(Run run) {
        Player player = Bukkit.getPlayer(run.playerId);
        if (player == null || !player.isOnline()) {
            stopRun(run.playerId, true);
            return;
        }
        if (!run.active) {
            stopRun(run.playerId, true);
            return;
        }
        if (System.currentTimeMillis() >= run.waveDeadlineMs) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Wave timer expired. The stronghold overwhelms you.");
            player.sendTitle(ChatColor.RED + "Run Failed", ChatColor.GRAY + "Wave " + run.wave, 8, 40, 10);
            stopRun(run.playerId, true);
            return;
        }
        if (run.mobsRemaining <= 0) {
            completeWave(run);
            return;
        }
        updateBossBar(run);
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateBoard(player);
        }
    }

    private int spawnWaveMobs(Run run, Player player) {
        CustomMobManager mobManager = plugin.getCustomMobManager();
        if (mobManager == null) {
            return 0;
        }
        int wave = run.wave;
        boolean eliteWave = isEliteWave(wave);
        boolean bossWave = wave >= FINAL_WAVE;
        int count = bossWave ? 1 : eliteWave ? Math.max(2, wave / 5) : Math.min(20, 4 + wave);
        int level = Math.max(1, 4 + (wave * 2));
        String forcedMob = bossWave ? pickAvailableMob(mobManager, BOSS_POOL)
                : eliteWave ? pickAvailableMob(mobManager, ELITE_POOL)
                : pickAvailableMob(mobManager, poolForWave(wave));
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            String mobId = forcedMob != null ? forcedMob : pickAvailableMob(mobManager, poolForWave(wave));
            if (mobId == null) {
                continue;
            }
            Location spawn = randomSpawnAround(player.getLocation(), 8.0, 17.0);
            List<LivingEntity> entities = mobManager.spawn(mobId, spawn, 1, level);
            if (entities.isEmpty()) {
                continue;
            }
            LivingEntity entity = entities.getFirst();
            entity.addScoreboardTag(WAVE_TAG);
            entity.addScoreboardTag(WAVE_TAG + ":" + player.getUniqueId());
            mobToOwner.put(entity.getUniqueId(), player.getUniqueId());
            run.mobIds.add(entity.getUniqueId());
            spawned++;
        }
        return spawned;
    }

    private void completeWave(Run run) {
        Player player = Bukkit.getPlayer(run.playerId);
        if (player == null || !player.isOnline()) {
            stopRun(run.playerId, true);
            return;
        }
        int xpReward = 60 + (run.wave * 18);
        plugin.getLevelManager().addXP(player, xpReward);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                ChatColor.GOLD + "Wave " + run.wave + " cleared "
                        + ChatColor.GRAY + "• +" + xpReward + " <glyph:experience_orb_icon> XP");
        if (run.wave >= FINAL_WAVE) {
            finishRun(run);
            return;
        }
        run.lastBuff = grantIntermissionBuff(player, run.wave);
        beginWave(run, true);
    }

    private void finishRun(Run run) {
        Player player = Bukkit.getPlayer(run.playerId);
        if (player != null && player.isOnline()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "STRONGHOLD CLEARED"
                            + ChatColor.GRAY + " • You survived all " + ChatColor.GOLD + FINAL_WAVE + ChatColor.GRAY + " waves.");
            player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "Stronghold Cleared",
                    ChatColor.GRAY + "All waves defeated", 10, 60, 15);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
        }
        stopRun(run.playerId, true);
    }

    private void updateBossBar(Run run) {
        if (run.bossBar == null) {
            return;
        }
        long millisLeft = Math.max(0L, run.waveDeadlineMs - System.currentTimeMillis());
        double progress = run.mobsRemaining <= 0 ? 0.0 : Math.min(1.0, millisLeft / (BASE_WAVE_SECONDS * 1000.0));
        run.bossBar.setProgress(Math.max(0.0, progress));
        run.bossBar.setTitle(ChatColor.GOLD + "Wave " + run.wave
                + ChatColor.DARK_GRAY + " • "
                + ChatColor.WHITE + run.mobsRemaining + " mobs"
                + ChatColor.DARK_GRAY + " • "
                + ChatColor.YELLOW + (millisLeft / 1000L) + "s");
    }

    private BuffResult grantIntermissionBuff(Player player, int wave) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int roll = random.nextInt(4);
        return switch (roll) {
            case 0 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 30, 0, true, false, true));
                yield new BuffResult("Battle Boon", ChatColor.GRAY + "Boon: " + ChatColor.RED + "Fury "
                        + ChatColor.GRAY + "for 30s.");
            }
            case 1 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, 0, true, false, true));
                yield new BuffResult("Guard Boon", ChatColor.GRAY + "Boon: " + ChatColor.BLUE + "Bulwark "
                        + ChatColor.GRAY + "for 30s.");
            }
            case 2 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 0, true, false, true));
                yield new BuffResult("Mobility Boon", ChatColor.GRAY + "Boon: " + ChatColor.AQUA + "Haste "
                        + ChatColor.GRAY + "for 30s.");
            }
            default -> {
                StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
                int restored = Math.max(20, 30 + (wave * 2));
                stats.setCurrentMana(Math.min(stats.getMaxMana(), stats.getCurrentMana() + restored));
                double healed = Math.min(player.getMaxHealth(), player.getHealth() + (player.getMaxHealth() * 0.20));
                player.setHealth(healed);
                yield new BuffResult("Recovery Boon", ChatColor.GRAY + "Boon: " + ChatColor.GREEN + "Recovered "
                        + ChatColor.WHITE + restored + ChatColor.GRAY + " mana and health.");
            }
        };
    }

    private boolean isEliteWave(int wave) {
        return wave == 10 || wave == 15 || wave == 20 || wave == 25;
    }

    private List<String> poolForWave(int wave) {
        if (wave <= 9) {
            return EARLY_POOL;
        }
        if (wave <= 19) {
            return MID_POOL;
        }
        return LATE_POOL;
    }

    private String pickAvailableMob(CustomMobManager manager, List<String> candidates) {
        if (manager == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<String> available = new ArrayList<>();
        for (String id : candidates) {
            if (manager.getDefinition(id).isPresent()) {
                available.add(id);
            }
        }
        if (available.isEmpty()) {
            return null;
        }
        return available.get(ThreadLocalRandom.current().nextInt(available.size()));
    }

    private Location randomSpawnAround(Location center, double minRadius, double maxRadius) {
        Location base = center == null ? null : center.clone();
        if (base == null || base.getWorld() == null) {
            return center;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0.0, Math.PI * 2.0);
        double radius = random.nextDouble(minRadius, maxRadius);
        double x = base.getX() + (Math.cos(angle) * radius);
        double z = base.getZ() + (Math.sin(angle) * radius);
        int y = base.getWorld().getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        return new Location(base.getWorld(), x, y + 1.0, z);
    }

    private boolean isStrongholdWorld(World world) {
        if (world == null || world.getName() == null) {
            return false;
        }
        String name = world.getName().toLowerCase(java.util.Locale.ROOT);
        return name.startsWith("stronghold_debug_") || name.contains("stronghold");
    }

    private void initializeRunBorder(Run run, Player player) {
        if (run == null || player == null || player.getWorld() == null) {
            return;
        }
        World world = player.getWorld();
        var border = world.getWorldBorder();
        run.borderState = new BorderState(
                border.getCenter().clone(),
                border.getSize(),
                border.getWarningDistance(),
                border.getWarningTime()
        );
        border.setCenter(player.getLocation().getX(), player.getLocation().getZ());
        border.setWarningDistance(BORDER_WARNING_DISTANCE);
        border.setWarningTime(8);
        border.setSize(BORDER_INITIAL_SIZE);
    }

    private void applyWaveBorder(Run run) {
        if (run == null) {
            return;
        }
        World world = Bukkit.getWorld(run.worldId);
        if (world == null) {
            return;
        }
        double nextSize = Math.max(BORDER_MIN_SIZE, BORDER_INITIAL_SIZE - ((run.wave - 1) * BORDER_SHRINK_PER_WAVE));
        world.getWorldBorder().setSize(nextSize, BASE_WAVE_SECONDS);
    }

    private void restoreRunBorder(Run run) {
        if (run == null || run.borderState == null) {
            return;
        }
        World world = Bukkit.getWorld(run.worldId);
        if (world == null) {
            return;
        }
        var border = world.getWorldBorder();
        border.setCenter(run.borderState.center());
        border.setSize(run.borderState.size());
        border.setWarningDistance(run.borderState.warningDistance());
        border.setWarningTime(run.borderState.warningTime());
    }

    private static final class Run {
        private final UUID playerId;
        private final UUID worldId;
        private boolean active = true;
        private int wave = 0;
        private int mobsRemaining = 0;
        private long waveDeadlineMs = 0L;
        private final Set<UUID> mobIds = new HashSet<>();
        private BossBar bossBar;
        private BukkitTask waveTask;
        private BuffResult lastBuff;
        private BorderState borderState;

        private Run(UUID playerId, UUID worldId) {
            this.playerId = playerId;
            this.worldId = worldId;
        }
    }

    private record BuffResult(String id, String message) {
    }

    private record BorderState(Location center, double size, int warningDistance, int warningTime) {
    }
}
