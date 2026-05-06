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
import me.nakilex.levelplugin.utils.CombatTargetUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
    private final Map<String, Map<UUID, DungeonProgress>> progressByDungeon = new HashMap<>();
    private File progressionFile;
    private YamlConfiguration progressionConfig;

    public StagedDungeonManager(Main plugin, ArenaInstanceManager instanceManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
        this.playerConfig = plugin.getPlayerConfig();
        registerDefaults();
        loadProgressionData();
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
                500.0D,
                500.0D,
                3,
                "gems",
                "<glyph:purple_orb_icon>",
                20,
                org.bukkit.boss.BarColor.PURPLE,
                StagedDungeonObjective.KILL_MOB,
                (player, amount) -> plugin.getGemsManager().addUnits(player, amount)
        ));
        registerDefinition(new StagedDungeonDefinition(
                "coin",
                "Coin Dungeon",
                ChatColor.GOLD,
                org.bukkit.Material.GOLD_NUGGET,
                "coin_dungeon",
                org.bukkit.entity.EntityType.SLIME,
                "Golden Slime",
                500.0D,
                500.0D,
                3,
                "coins",
                "<glyph:coins_icon>",
                20,
                org.bukkit.boss.BarColor.YELLOW,
                StagedDungeonObjective.DAMAGE_METER,
                (player, amount) -> plugin.getEconomyManager().addCoins(player, amount)
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
        int legacyProfileStage = slot == null ? 0 : playerConfig.getStagedDungeonBestStage(playerId, slot, definition.id());
        int progressionStage = getProgressionStage(playerId, definition);
        int resolved = Math.max(legacyProfileStage, progressionStage);
        if (resolved > progressionStage) {
            setProgression(playerId, definition, resolved, getProgression(playerId, definition).bestDamage());
            saveProgressionData();
        }
        return resolved;
    }

    public int getSweepsUsed(Player player, StagedDungeonDefinition definition) {
        if (!definition.supportsSweeps()) return 0;
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
        if (!definition.supportsSweeps()) return 0;
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
        ArenaInstance instance = instanceManager.createInstance(definition.worldPrefix());
        if (instance == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Failed to create a dungeon instance.");
            return;
        }
        StagedDungeonRun run = new StagedDungeonRun(player.getUniqueId(), definition, stage,
                definition.runMobHealth(stage), player.getLocation(), instance);
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
        double bestDamage = getBestDamage(player, definition);
        int reward = definition.rewardForSweep(stage, bestDamage);
        definition.rewardGrant().grant(player, reward);
        int used = getSweepsUsed(player, definition) + 1;
        playerConfig.setStagedDungeonSweeps(player.getUniqueId(), slot, definition.id(), used, currentSweepResetKey());
        playerConfig.savePlayer(player.getUniqueId());
        ChatMessageUtil.send(player, MessageType.REWARD,
                "You received " + definition.themeColor() + NumberUtil.formatCommas(reward) + " "
                        + definition.rewardGlyph() + " " + ChatColor.GOLD + definition.rewardName()
                        + ChatColor.GOLD + " from sweeping " + definition.displayName() + " Stage "
                        + ChatColor.WHITE + stage + ChatColor.GOLD
                        + (definition.isDamageMeter()
                        ? ChatColor.GRAY + " (Best Damage: " + ChatColor.WHITE + NumberUtil.formatCommas(Math.round(bestDamage)) + ChatColor.GRAY + ")"
                        : "") + ChatColor.GOLD + ".");
    }

    public void stopAll() {
        for (StagedDungeonRun run : new java.util.ArrayList<>(activeRuns.values())) {
            finishRun(run, false, false);
        }
        activeRuns.clear();
    }

    private void spawnStageMob(StagedDungeonRun run) {
        Location spawn = run.instance.getSecondSpawn();
        LivingEntity entity = (LivingEntity) spawn.getWorld().spawnEntity(spawn, run.definition.mobType());
        if (entity instanceof Slime slime) {
            slime.setSize(1);
        }
        AttributeUtil.setMaxHealthAndHeal(entity, run.mobHealth);
        if (run.definition.isDamageMeter()) {
            entity.setAI(false);
            entity.setRemoveWhenFarAway(false);
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
                    if (run.definition.isDamageMeter()) {
                        completeRun(run);
                    } else {
                        failRun(run);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void updateHealthBar(StagedDungeonRun run) {
        if (run == null || run.healthBar == null) return;
        if (run.definition.isDamageMeter()) {
            int reward = run.definition.rewardFromDamage(run.damageDealt);
            run.healthBar.setTitle(run.definition.themeColor() + "§l" + run.definition.mobDisplayName()
                    + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Reward: "
                    + run.definition.themeColor() + NumberUtil.formatCommas(reward) + " "
                    + run.definition.rewardGlyph() + " " + run.definition.rewardName());
            double secondsLeft = Math.max(0.0D, (run.deadlineMs - System.currentTimeMillis()) / 1000.0D);
            run.healthBar.setProgress(Math.max(0.0D, Math.min(1.0D,
                    secondsLeft / Math.max(1.0D, run.definition.stageTimeSeconds()))));
            return;
        }
        LivingEntity mob = run.getMob();
        double current = mob == null || mob.isDead() ? 0.0D : Math.max(0.0D, mob.getHealth());
        double max = Math.max(1.0D, run.mobHealth);
        run.healthBar.setTitle(run.definition.themeColor() + "§l" + run.definition.mobDisplayName()
                + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + NumberUtil.formatCommas(Math.round(current))
                + ChatColor.GRAY + "/" + ChatColor.WHITE + NumberUtil.formatCommas(Math.round(max)) + " HP");
        run.healthBar.setProgress(Math.max(0.0D, Math.min(1.0D, current / max)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        StagedDungeonRun run = findRunByMob(living.getUniqueId());
        if (run == null || run.finishing || !run.definition.isDamageMeter()) return;
        if (!CombatTargetUtil.isPlayerSourced(event.getDamager())) return;
        run.damageDealt += Math.max(0.0D, event.getFinalDamage());
        event.setDamage(0.0D);
        LivingEntity mob = run.getMob();
        if (mob != null && !mob.isDead()) {
            mob.setHealth(Math.min(run.mobHealth, getMaxHealth(mob)));
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            StagedDungeonRun current = activeRuns.get(run.playerId);
            healDamageMeterMob(current);
            updateHealthBar(current);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        StagedDungeonRun run = findRunByMob(living.getUniqueId());
        if (run == null || run.finishing) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            StagedDungeonRun current = activeRuns.get(run.playerId);
            if (current == null || current.finishing) return;
            if (current.definition.isDamageMeter()) {
                event.setDamage(0.0D);
                healDamageMeterMob(current);
                updateHealthBar(current);
                return;
            }
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
        if (run.definition.isDamageMeter()) {
            return;
        }
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
        int reward = run.definition.isDamageMeter()
                ? run.definition.rewardFromDamage(run.damageDealt)
                : run.definition.rewardForStage(run.stage);
        Player player = run.getPlayer();
        if (player != null) {
            run.definition.rewardGrant().grant(player, reward);
            persistRunProgress(player, run.definition, run.stage, run.definition.isDamageMeter() ? run.damageDealt : 0.0D);
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

    private void healDamageMeterMob(StagedDungeonRun run) {
        if (run == null || !run.definition.isDamageMeter()) return;
        LivingEntity mob = run.getMob();
        if (mob == null || mob.isDead()) return;
        mob.setHealth(Math.min(run.mobHealth, getMaxHealth(mob)));
    }

    private double getMaxHealth(LivingEntity mob) {
        if (mob == null) return 1.0D;
        org.bukkit.attribute.Attribute maxHealthAttribute = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        org.bukkit.attribute.AttributeInstance attribute = maxHealthAttribute == null ? null : mob.getAttribute(maxHealthAttribute);
        return attribute == null ? Math.max(1.0D, mob.getHealth()) : Math.max(1.0D, attribute.getValue());
    }

    private StagedDungeonRun findRunByMob(UUID mobId) {
        if (mobId == null) return null;
        return activeRuns.values().stream()
                .filter(candidate -> mobId.equals(candidate.mobId))
                .findFirst()
                .orElse(null);
    }

    private void persistRunProgress(Player player, StagedDungeonDefinition definition, int stage, double damageDealt) {
        UUID playerId = player.getUniqueId();
        DungeonProgress previousProgression = getProgression(playerId, definition);
        int updated = Math.max(previousProgression.highestCompletedStage(), stage);
        double updatedBestDamage = Math.max(previousProgression.bestDamage(), damageDealt);
        setProgression(playerId, definition, updated, updatedBestDamage);
        saveProgressionData();

        Integer slot = resolveProgressSlot(playerId);
        int previousProfile = 0;
        int verifiedProfile = 0;
        if (slot == null) {
            plugin.getLogger().warning("No profile slot resolved while mirroring " + definition.displayName()
                    + " Stage " + stage + " for " + player.getName()
                    + "; manager progression was still saved.");
        } else {
            previousProfile = playerConfig.getStagedDungeonBestStage(playerId, slot, definition.id());
            playerConfig.setStagedDungeonBestStage(playerId, slot, definition.id(), Math.max(previousProfile, updated));
            playerConfig.savePlayer(playerId);
            verifiedProfile = playerConfig.getStagedDungeonBestStage(playerId, slot, definition.id());
        }
        plugin.getLogger().fine("Updated " + definition.displayName() + " progression for " + player.getName()
                + ": cleared=" + stage + ", previousProgression=" + previousProgression.highestCompletedStage()
                + ", updated=" + updated + ", bestDamage=" + updatedBestDamage + ", previousProfile=" + previousProfile
                + ", verifiedProfile=" + verifiedProfile + ", next=" + definition.nextStage(updated));
    }

    private void sendCompletionMessage(Player player, StagedDungeonRun run, int reward) {
        ChatFormatter.constructDivider(player, run.definition.themeColor() + "§l-", 45);
        ChatFormatter.sendCenteredMessage(player, run.definition.themeColor() + "§l" + run.definition.displayName().toUpperCase() + " CLEARED");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "Stage " + ChatColor.WHITE + run.stage + ChatColor.GRAY
                        + (run.definition.isDamageMeter() ? " completed." : " defeated."));
        if (run.definition.isDamageMeter()) {
            ChatFormatter.sendCenteredMessage(player,
                    ChatColor.GRAY + "Damage Dealt: " + ChatColor.WHITE
                            + NumberUtil.formatCommas(Math.round(run.damageDealt)));
        }
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

    private void loadProgressionData() {
        progressionFile = new File(plugin.getDataFolder(), "staged_dungeon_progression.yml");
        progressionConfig = YamlConfiguration.loadConfiguration(progressionFile);
        progressByDungeon.clear();
        if (!progressionConfig.isConfigurationSection("dungeons")) {
            return;
        }
        for (String dungeonId : progressionConfig.getConfigurationSection("dungeons").getKeys(false)) {
            String playersPath = "dungeons." + dungeonId + ".players";
            if (!progressionConfig.isConfigurationSection(playersPath)) {
                continue;
            }
            Map<UUID, DungeonProgress> progress = progressByDungeon.computeIfAbsent(
                    dungeonId.toLowerCase(java.util.Locale.ROOT), ignored -> new HashMap<>());
            for (String key : progressionConfig.getConfigurationSection(playersPath).getKeys(false)) {
                try {
                    String playerPath = playersPath + "." + key;
                    int highestStage = Math.max(0, progressionConfig.getInt(playerPath + ".highest-completed-stage", 0));
                    double bestDamage = Math.max(0.0D, progressionConfig.getDouble(playerPath + ".best-damage", 0.0D));
                    progress.put(UUID.fromString(key), new DungeonProgress(highestStage, bestDamage));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring invalid staged dungeon progression UUID: " + key);
                }
            }
        }
    }

    private void saveProgressionData() {
        if (progressionConfig == null || progressionFile == null) return;
        progressionConfig.set("dungeons", null);
        for (Map.Entry<String, Map<UUID, DungeonProgress>> dungeonEntry : progressByDungeon.entrySet()) {
            String dungeonId = dungeonEntry.getKey();
            for (Map.Entry<UUID, DungeonProgress> playerEntry : dungeonEntry.getValue().entrySet()) {
                String playerPath = "dungeons." + dungeonId + ".players." + playerEntry.getKey();
                DungeonProgress progress = playerEntry.getValue();
                progressionConfig.set(playerPath + ".highest-completed-stage", Math.max(0, progress.highestCompletedStage()));
                progressionConfig.set(playerPath + ".best-damage", progress.bestDamage() > 0.0D ? progress.bestDamage() : null);
            }
        }
        try {
            progressionConfig.save(progressionFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save staged_dungeon_progression.yml: " + ex.getMessage());
        }
    }

    private int getProgressionStage(UUID playerId, StagedDungeonDefinition definition) {
        return getProgression(playerId, definition).highestCompletedStage();
    }

    public double getBestDamage(Player player, StagedDungeonDefinition definition) {
        if (player == null || definition == null) return 0.0D;
        return getProgression(player.getUniqueId(), definition).bestDamage();
    }

    public int getSweepReward(Player player, StagedDungeonDefinition definition) {
        if (player == null || definition == null) return 0;
        int highest = getHighestCleared(player, definition);
        return definition.rewardForSweep(definition.sweepStage(highest), getBestDamage(player, definition));
    }

    private DungeonProgress getProgression(UUID playerId, StagedDungeonDefinition definition) {
        return progressByDungeon
                .getOrDefault(progressKey(definition), java.util.Collections.emptyMap())
                .getOrDefault(playerId, DungeonProgress.EMPTY);
    }

    private void setProgression(UUID playerId, StagedDungeonDefinition definition, int stage, double bestDamage) {
        progressByDungeon
                .computeIfAbsent(progressKey(definition), ignored -> new HashMap<>())
                .merge(playerId, new DungeonProgress(Math.max(0, stage), Math.max(0.0D, bestDamage)), DungeonProgress::bestOf);
    }

    private record DungeonProgress(int highestCompletedStage, double bestDamage) {
        private static final DungeonProgress EMPTY = new DungeonProgress(0, 0.0D);

        private static DungeonProgress bestOf(DungeonProgress first, DungeonProgress second) {
            return new DungeonProgress(
                    Math.max(first.highestCompletedStage, second.highestCompletedStage),
                    Math.max(first.bestDamage, second.bestDamage));
        }
    }

    private String progressKey(StagedDungeonDefinition definition) {
        return definition.id().toLowerCase(java.util.Locale.ROOT);
    }

    public SweepAdjustment adjustSweeps(Player player, StagedDungeonDefinition definition, int deltaAvailableSweeps) {
        if (player == null || definition == null || deltaAvailableSweeps == 0) {
            return null;
        }
        Integer slot = resolveProgressSlot(player.getUniqueId());
        if (slot == null) {
            return null;
        }
        String today = currentSweepResetKey();
        String stored = playerConfig.getStagedDungeonSweepResetKey(player.getUniqueId(), slot, definition.id());
        int used = today.equals(stored)
                ? playerConfig.getStagedDungeonSweepsUsed(player.getUniqueId(), slot, definition.id())
                : 0;
        int beforeLeft = Math.max(0, definition.sweepAttempts() - used);
        int afterLeft = Math.max(0, Math.min(definition.sweepAttempts(), beforeLeft + deltaAvailableSweeps));
        int newUsed = Math.max(0, definition.sweepAttempts() - afterLeft);
        playerConfig.setStagedDungeonSweeps(player.getUniqueId(), slot, definition.id(), newUsed, today);
        playerConfig.savePlayer(player.getUniqueId());
        return new SweepAdjustment(beforeLeft, afterLeft, definition.sweepAttempts());
    }

    public record SweepAdjustment(int beforeLeft, int afterLeft, int total) {}

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
