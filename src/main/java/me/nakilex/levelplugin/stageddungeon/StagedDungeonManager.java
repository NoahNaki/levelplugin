package me.nakilex.levelplugin.stageddungeon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.arena.instance.ArenaInstance;
import me.nakilex.levelplugin.arena.instance.ArenaInstanceManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/** Coordinates reusable stage-based currency dungeon runs and progression. */
public class StagedDungeonManager implements Listener {
    private static final String RUN_MOB_TAG = "staged_dungeon_mob";

    public record StageStatus(String displayName, ChatColor color, int stage, int secondsLeft) {}

    private final Main plugin;
    private final ArenaInstanceManager instanceManager;
    private final PlayerConfig playerConfig;
    private final ProfileManager profileManager = ProfileManager.getInstance();
    private final Map<String, StagedDungeonDefinition> definitions = new HashMap<>();
    private final Map<UUID, StagedDungeonRun> activeRuns = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> highestClearedCache = new HashMap<>();

    public StagedDungeonManager(Main plugin, ArenaInstanceManager instanceManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
        this.playerConfig = plugin.getPlayerConfig();
        registerDefaults();
    }

    private void registerDefaults() {
        registerDefinition(new StagedDungeonDefinition(
                "gem",
                "Gem Dungeon",
                ChatColor.LIGHT_PURPLE,
                org.bukkit.Material.AMETHYST_CLUSTER,
                "gem_dungeon",
                org.bukkit.entity.EntityType.SLIME,
                "Common Slime",
                100.0D,
                50.0D,
                3,
                "gems",
                "<glyph:purple_orb_icon>",
                20,
                org.bukkit.boss.BarColor.PURPLE,
                (player, amount) -> plugin.getGemsManager().addUnits(player, amount)
        ));
    }

    public void registerDefinition(StagedDungeonDefinition definition) {
        if (definition == null || definition.id() == null || definition.id().isBlank()) return;
        definitions.put(definition.id().toLowerCase(), definition);
    }

    public Optional<StagedDungeonDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id == null ? "" : id.toLowerCase()));
    }

    public Collection<StagedDungeonDefinition> getDefinitions() {
        return java.util.Collections.unmodifiableCollection(definitions.values());
    }

    public boolean isInRun(UUID playerId) {
        return activeRuns.containsKey(playerId);
    }

    public StageStatus getStageStatus(UUID playerId) {
        StagedDungeonRun run = activeRuns.get(playerId);
        if (run == null) return null;
        return new StageStatus(run.definition.displayName(), run.definition.themeColor(), run.stage, secondsLeft(run));
    }

    public int getHighestCleared(Player player, StagedDungeonDefinition definition) {
        UUID playerId = player.getUniqueId();
        Integer slot = resolveProgressSlot(playerId);
        int stored = slot == null ? 0 : playerConfig.getStagedDungeonBestStage(playerId, slot, definition.id());
        int cached = getCachedHighestCleared(playerId, definition);
        int resolved = Math.max(stored, cached);
        if (resolved > cached) {
            cacheHighestCleared(playerId, definition, resolved);
        }
        return resolved;
    }

    public int getSweepsUsed(Player player, StagedDungeonDefinition definition) {
        Integer slot = resolveProgressSlot(player.getUniqueId());
        if (slot == null) return 0;
        String today = currentSweepResetKey();
        String stored = playerConfig.getStagedDungeonSweepResetKey(player.getUniqueId(), slot, definition.id());
        if (!today.equals(stored)) {
            playerConfig.setStagedDungeonSweeps(player.getUniqueId(), slot, definition.id(), 0, today);
            playerConfig.savePlayer(player.getUniqueId());
            return 0;
        }
        return playerConfig.getStagedDungeonSweepsUsed(player.getUniqueId(), slot, definition.id());
    }

    public int getSweepsLeft(Player player, StagedDungeonDefinition definition) {
        return Math.max(0, definition.sweepAttempts() - getSweepsUsed(player, definition));
    }

    public void startStage(Player player, StagedDungeonDefinition definition) {
        if (activeRuns.containsKey(player.getUniqueId())) {
            ChatMessageUtil.send(player, MessageType.WARNING, "You are already inside a dungeon.");
            return;
        }
        if (instanceManager == null || !instanceManager.isTemplateLoaded()) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Dungeon arenas are unavailable right now.");
            return;
        }
        int highestCleared = getHighestCleared(player, definition);
        int stage = definition.nextStage(highestCleared);
        debugProgress(player, definition, "start", "highest=" + highestCleared + ", attempting=" + stage);
        ArenaInstance instance = instanceManager.createInstance(definition.worldPrefix());
        if (instance == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Failed to create a dungeon instance.");
            return;
        }
        StagedDungeonRun run = new StagedDungeonRun(player.getUniqueId(), definition, stage,
                definition.mobHealth(stage), player.getLocation(), instance);
        activeRuns.put(player.getUniqueId(), run);
        TeleportUtils.safeTeleport(player, instance.getFirstSpawn());
        spawnStageMob(run);
        startTimer(run);
        updateScoreboard(player);
        ChatMessageUtil.send(player, MessageType.SUCCESS,
                "Entering " + definition.themeColor() + definition.displayName() + ChatColor.GREEN
                        + " Stage " + ChatColor.WHITE + stage + ChatColor.GREEN + ".");
    }

    public void sweep(Player player, StagedDungeonDefinition definition) {
        if (activeRuns.containsKey(player.getUniqueId())) {
            ChatMessageUtil.send(player, MessageType.WARNING, "Finish your active dungeon before sweeping.");
            return;
        }
        int highest = getHighestCleared(player, definition);
        if (highest <= 0) {
            ChatMessageUtil.send(player, MessageType.WARNING, "Clear Stage 1 before sweeping this dungeon.");
            return;
        }
        int left = getSweepsLeft(player, definition);
        if (left <= 0) {
            ChatMessageUtil.send(player, MessageType.WARNING, "You have no sweeps left for " + definition.displayName() + ".");
            return;
        }
        Integer slot = resolveProgressSlot(player.getUniqueId());
        if (slot == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "No active profile found.");
            return;
        }
        int stage = definition.sweepStage(highest);
        int reward = definition.rewardForStage(stage);
        definition.rewardGrant().grant(player, reward);
        int used = getSweepsUsed(player, definition) + 1;
        playerConfig.setStagedDungeonSweeps(player.getUniqueId(), slot, definition.id(), used, currentSweepResetKey());
        playerConfig.savePlayer(player.getUniqueId());
        ChatMessageUtil.send(player, MessageType.REWARD,
                "You received " + definition.themeColor() + NumberUtil.formatCommas(reward) + " "
                        + definition.rewardGlyph() + " " + ChatColor.GOLD + definition.rewardName()
                        + ChatColor.GOLD + " from sweeping " + definition.displayName() + " Stage "
                        + ChatColor.WHITE + stage + ChatColor.GOLD + ".");
    }

    public void stopAll() {
        for (StagedDungeonRun run : new java.util.ArrayList<>(activeRuns.values())) {
            finishRun(run, false, false);
        }
        activeRuns.clear();
    }

    private void spawnStageMob(StagedDungeonRun run) {
        Location spawn = run.instance.getSecondSpawn();
        Attribute maxHealthAttr = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        LivingEntity entity = (LivingEntity) spawn.getWorld().spawnEntity(spawn, run.definition.mobType());
        if (maxHealthAttr != null && entity.getAttribute(maxHealthAttr) != null) {
            entity.getAttribute(maxHealthAttr).setBaseValue(run.mobHealth);
            entity.setHealth(run.mobHealth);
        }
        if (entity instanceof Slime slime) {
            slime.setSize(1);
        }
        entity.setCustomName(run.definition.themeColor() + run.definition.mobDisplayName()
                + ChatColor.GRAY + " [Stage " + run.stage + "]");
        entity.setCustomNameVisible(true);
        entity.addScoreboardTag(RUN_MOB_TAG);
        entity.addScoreboardTag("staged_dungeon_" + run.definition.id());
        run.mobId = entity.getUniqueId();
        createHealthBar(run);
    }

    private void createHealthBar(StagedDungeonRun run) {
        Player player = run.getPlayer();
        if (player == null) return;
        run.healthBar = Bukkit.createBossBar("", run.definition.bossBarColor(), BarStyle.SOLID);
        run.healthBar.addPlayer(player);
        run.healthBar.setVisible(true);
        updateHealthBar(run);
    }

    private void startTimer(StagedDungeonRun run) {
        run.deadlineMs = System.currentTimeMillis() + Math.max(1, run.definition.stageTimeSeconds()) * 1000L;
        run.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeRuns.containsKey(run.playerId) || run.finishing) {
                    cancel();
                    return;
                }
                updateHealthBar(run);
                Player player = run.getPlayer();
                if (player != null) {
                    updateScoreboard(player);
                }
                if (System.currentTimeMillis() >= run.deadlineMs) {
                    failRun(run);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void updateHealthBar(StagedDungeonRun run) {
        if (run == null || run.healthBar == null) return;
        LivingEntity mob = run.getMob();
        double current = mob == null || mob.isDead() ? 0.0D : Math.max(0.0D, mob.getHealth());
        double max = Math.max(1.0D, run.mobHealth);
        run.healthBar.setTitle(run.definition.themeColor() + "§l" + run.definition.mobDisplayName()
                + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + NumberUtil.formatCommas(Math.round(current))
                + ChatColor.GRAY + "/" + ChatColor.WHITE + NumberUtil.formatCommas(Math.round(max)) + " HP");
        run.healthBar.setProgress(Math.max(0.0D, Math.min(1.0D, current / max)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        StagedDungeonRun run = findRunByMob(living.getUniqueId());
        if (run == null || run.finishing) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            StagedDungeonRun current = activeRuns.get(run.playerId);
            if (current == null || current.finishing) return;
            updateHealthBar(current);
            LivingEntity mob = current.getMob();
            if (mob == null || mob.isDead() || mob.getHealth() <= 0.0D) {
                completeRun(current);
            }
        });
    }

    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent event) {
        if (findRunByMob(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!event.getEntity().getScoreboardTags().contains(RUN_MOB_TAG)) return;
        StagedDungeonRun run = findRunByMob(event.getEntity().getUniqueId());
        if (run == null) {
            run = activeRuns.values().stream()
                    .filter(candidate -> candidate.instance.getWorld().equals(event.getEntity().getWorld()))
                    .findFirst()
                    .orElse(null);
        }
        if (run == null || run.finishing) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        completeRun(run);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        StagedDungeonRun run = activeRuns.remove(event.getPlayer().getUniqueId());
        if (run == null) return;
        run.cleanupUi();
        run.removeMob();
        instanceManager.destroyInstance(run.instance);
        updateProfileLocation(run.playerId, run.returnLocation);
    }

    private void completeRun(StagedDungeonRun run) {
        if (run == null || run.finishing) return;
        run.finishing = true;
        int reward = run.definition.rewardForStage(run.stage);
        Player player = run.getPlayer();
        if (player != null) {
            run.definition.rewardGrant().grant(player, reward);
            persistHighestCleared(player, run.definition, run.stage);
            sendCompletionMessage(player, run, reward);
        }
        finishRun(run, true, true);
    }

    private void failRun(StagedDungeonRun run) {
        if (run == null || run.finishing) return;
        run.finishing = true;
        Player player = run.getPlayer();
        if (player != null) {
            ChatMessageUtil.send(player, MessageType.ERROR,
                    "Time ran out! You failed " + run.definition.displayName() + " Stage " + ChatColor.WHITE + run.stage + ChatColor.RED + ".");
        }
        finishRun(run, true, true);
    }

    private void finishRun(StagedDungeonRun run, boolean teleportBack, boolean removeFromActive) {
        if (removeFromActive) activeRuns.remove(run.playerId);
        run.cleanupUi();
        run.removeMob();
        Player player = run.getPlayer();
        if (teleportBack && player != null) {
            TeleportUtils.safeTeleport(player, run.returnLocation);
            updateProfileLocation(player.getUniqueId(), run.returnLocation);
            Bukkit.getScheduler().runTask(plugin, () -> updateScoreboard(player));
        }
        instanceManager.destroyInstance(run.instance);
    }

    private StagedDungeonRun findRunByMob(UUID mobId) {
        if (mobId == null) return null;
        return activeRuns.values().stream()
                .filter(candidate -> mobId.equals(candidate.mobId))
                .findFirst()
                .orElse(null);
    }

    private void persistHighestCleared(Player player, StagedDungeonDefinition definition, int stage) {
        Integer slot = resolveProgressSlot(player.getUniqueId());
        if (slot == null) {
            plugin.getLogger().warning("Unable to persist " + definition.displayName() + " Stage " + stage
                    + " for " + player.getName() + " because no profile slot could be resolved.");
            return;
        }
        int current = playerConfig.getStagedDungeonBestStage(player.getUniqueId(), slot, definition.id());
        int updated = Math.max(current, stage);
        cacheHighestCleared(player.getUniqueId(), definition, updated);
        playerConfig.setStagedDungeonBestStage(player.getUniqueId(), slot, definition.id(), updated);
        playerConfig.savePlayer(player.getUniqueId());
        int verified = playerConfig.getStagedDungeonBestStage(player.getUniqueId(), slot, definition.id());
        debugProgress(player, definition, "clear", "cleared=" + stage + ", previousStored=" + current
                + ", updated=" + updated + ", verifiedInMemory=" + verified + ", next=" + definition.nextStage(updated));
    }

    private void sendCompletionMessage(Player player, StagedDungeonRun run, int reward) {
        ChatFormatter.constructDivider(player, run.definition.themeColor() + "§l-", 45);
        ChatFormatter.sendCenteredMessage(player, run.definition.themeColor() + "§l" + run.definition.displayName().toUpperCase() + " CLEARED");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "Stage " + ChatColor.WHITE + run.stage + ChatColor.GRAY + " defeated.");
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "Reward: " + run.definition.themeColor() + NumberUtil.formatCommas(reward)
                        + " " + run.definition.rewardGlyph() + " " + run.definition.rewardName());
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "Next Stage: " + ChatColor.WHITE + run.definition.nextStage(getHighestCleared(player, run.definition)));
        ChatFormatter.constructDivider(player, run.definition.themeColor() + "§l-", 45);
    }

    private void updateProfileLocation(UUID id, Location back) {
        if (back == null) return;
        Integer slot = resolveProgressSlot(id);
        if (slot != null) {
            playerConfig.setProfileLocation(id, slot, back);
            playerConfig.savePlayer(id);
        }
    }

    private Integer resolveProgressSlot(UUID playerId) {
        Integer activeSlot = profileManager.getActiveSlot(playerId);
        if (activeSlot != null) {
            return activeSlot;
        }
        boolean profilesEnabled = plugin.getCustomConfig() == null
                || plugin.getCustomConfig().getBoolean("features.profiles", true);
        if (!profilesEnabled) {
            return 0;
        }
        return profileManager.getProfile(playerId, 0) == null ? null : 0;
    }

    private int getCachedHighestCleared(UUID playerId, StagedDungeonDefinition definition) {
        return highestClearedCache
                .getOrDefault(playerId, java.util.Collections.emptyMap())
                .getOrDefault(progressKey(definition), 0);
    }

    private void cacheHighestCleared(UUID playerId, StagedDungeonDefinition definition, int stage) {
        highestClearedCache
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .merge(progressKey(definition), Math.max(0, stage), Math::max);
    }

    private String progressKey(StagedDungeonDefinition definition) {
        return definition.id().toLowerCase(java.util.Locale.ROOT);
    }

    public void sendDebug(Player player, StagedDungeonDefinition definition) {
        if (player == null || definition == null) return;
        Integer activeSlot = profileManager.getActiveSlot(player.getUniqueId());
        Integer resolvedSlot = resolveProgressSlot(player.getUniqueId());
        int stored = resolvedSlot == null ? 0 : playerConfig.getStagedDungeonBestStage(player.getUniqueId(), resolvedSlot, definition.id());
        int cached = getCachedHighestCleared(player.getUniqueId(), definition);
        int highest = getHighestCleared(player, definition);
        ChatMessageUtil.send(player, MessageType.INFO, "[GemDungeonDebug] activeSlot=" + activeSlot
                + ", resolvedSlot=" + resolvedSlot + ", stored=" + stored + ", cached=" + cached
                + ", highest=" + highest + ", next=" + definition.nextStage(highest)
                + ", inRun=" + activeRuns.containsKey(player.getUniqueId()));
    }

    private void debugProgress(Player player, StagedDungeonDefinition definition, String action, String detail) {
        UUID playerId = player.getUniqueId();
        Integer activeSlot = profileManager.getActiveSlot(playerId);
        Integer resolvedSlot = resolveProgressSlot(playerId);
        int stored = resolvedSlot == null ? 0 : playerConfig.getStagedDungeonBestStage(playerId, resolvedSlot, definition.id());
        int cached = getCachedHighestCleared(playerId, definition);
        String message = "[StagedDungeonDebug] player=" + player.getName() + ", dungeon=" + definition.id()
                + ", action=" + action + ", activeSlot=" + activeSlot + ", resolvedSlot=" + resolvedSlot
                + ", stored=" + stored + ", cached=" + cached + ", " + detail;
        plugin.getLogger().info(message);
        ChatMessageUtil.send(player, MessageType.INFO, message);
    }

    public boolean isInstanceWorld(World world) {
        if (world == null) return false;
        return activeRuns.values().stream().anyMatch(run -> run.instance.getWorld().equals(world));
    }

    private int secondsLeft(StagedDungeonRun run) {
        return (int) Math.max(0L, Math.ceil((run.deadlineMs - System.currentTimeMillis()) / 1000.0D));
    }

    private void updateScoreboard(Player player) {
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateBoard(player);
        }
    }

    private String currentSweepResetKey() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }
}
