package me.nakilex.levelplugin.stageddungeon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.arena.instance.ArenaInstance;
import me.nakilex.levelplugin.arena.instance.ArenaInstanceManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.spells.SpellProgression;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TeleportUtils;
import me.nakilex.levelplugin.utils.CombatTargetUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/** Coordinates reusable stage-based currency dungeon runs and progression. */
public class StagedDungeonManager implements Listener {
    private static final String RUN_MOB_TAG = "staged_dungeon_mob";
    private static final String SPELL_UPGRADE_TITLE = "Dungeon Spell Upgrades";
    private static final int[] SPELL_UPGRADE_SLOTS = {11, 13, 15};
    private static final int DUNGEON_ENTRY_UPGRADES = 5;

    public record StageStatus(String displayName, ChatColor color, int stage, int secondsLeft) {}

    private final Main plugin;
    private final ArenaInstanceManager instanceManager;
    private final PlayerConfig playerConfig;
    private final ProfileManager profileManager = ProfileManager.getInstance();
    private final Map<String, StagedDungeonDefinition> definitions = new HashMap<>();
    private final Map<UUID, StagedDungeonRun> activeRuns = new HashMap<>();
    private final Map<String, Map<UUID, DungeonProgress>> progressByDungeon = new HashMap<>();
    private final Map<UUID, SpellUpgradeSession> pendingSpellUpgrades = new HashMap<>();
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
        return Math.max(0, getTotalSweepAttempts(player, definition) - getSweepsUsed(player, definition));
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
        beginDungeonSpellUpgrades(player, definition);
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
        int reward = applyDungeonYieldPetBonus(player, definition, definition.rewardForSweep(stage, bestDamage));
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        StagedDungeonRun run = findRunByMob(living.getUniqueId());
        if (run == null || run.finishing || !run.definition.isDamageMeter()) return;
        if (!CombatTargetUtil.isPlayerSourced(event.getDamager())) return;
        // Run after stat-scaling listeners so the meter records the real damage the hit would deal.
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
        clearDungeonSpellUpgrades(run.playerId);
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
            reward = applyDungeonYieldPetBonus(player, run.definition, reward);
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
        clearDungeonSpellUpgrades(run.playerId);
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
        int totalAttempts = getTotalSweepAttempts(player, definition);
        int beforeLeft = Math.max(0, totalAttempts - used);
        int afterLeft = Math.max(0, Math.min(totalAttempts, beforeLeft + deltaAvailableSweeps));
        int newUsed = Math.max(0, totalAttempts - afterLeft);
        playerConfig.setStagedDungeonSweeps(player.getUniqueId(), slot, definition.id(), newUsed, today);
        playerConfig.savePlayer(player.getUniqueId());
        return new SweepAdjustment(beforeLeft, afterLeft, getTotalSweepAttempts(player, definition));
    }

    public record SweepAdjustment(int beforeLeft, int afterLeft, int total) {}


    @EventHandler
    public void onSpellUpgradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView() == null || !GuiUtil.titleMatches(event.getView().getTitle(), SPELL_UPGRADE_TITLE)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        SpellUpgradeSession session = pendingSpellUpgrades.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }
        int choiceIndex = switch (event.getRawSlot()) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            default -> -1;
        };
        if (choiceIndex < 0 || choiceIndex >= session.choices().size()) return;
        SpellUpgradeChoice choice = session.choices().get(choiceIndex);
        SpellProgressionManager progression = SpellProgressionManager.getInstance();
        if (progression.addTemporarySpellLevel(player.getUniqueId(), choice.baseSpellId(), 1)) {
            ChatMessageUtil.send(player, MessageType.SUCCESS,
                    "Dungeon spell upgrade: " + ChatColor.WHITE + choice.displayName() + ChatColor.GRAY + ".");
        }
        int left = Math.max(0, session.remainingSelections() - 1);
        if (left <= 0) {
            pendingSpellUpgrades.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        List<SpellUpgradeChoice> choices = rollSpellUpgradeChoices(player, 3);
        if (choices.isEmpty()) {
            pendingSpellUpgrades.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        SpellUpgradeSession next = new SpellUpgradeSession(left, choices);
        pendingSpellUpgrades.put(player.getUniqueId(), next);
        Bukkit.getScheduler().runTask(plugin, () -> openSpellUpgradeGui(player, next));
    }

    @EventHandler
    public void onSpellUpgradeClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!GuiUtil.titleMatches(event.getView().getTitle(), SPELL_UPGRADE_TITLE)) return;
        SpellUpgradeSession session = pendingSpellUpgrades.get(player.getUniqueId());
        if (session == null || session.remainingSelections() <= 0) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(player.getUniqueId());
            SpellUpgradeSession current = pendingSpellUpgrades.get(player.getUniqueId());
            if (online != null && online.isOnline() && current != null && activeRuns.containsKey(player.getUniqueId())) {
                openSpellUpgradeGui(online, current);
            }
        });
    }

    private void beginDungeonSpellUpgrades(Player player, StagedDungeonDefinition definition) {
        SpellProgressionManager.getInstance().clearTemporarySpellLevels(player.getUniqueId());
        List<SpellUpgradeChoice> choices = rollSpellUpgradeChoices(player, 3);
        if (choices.isEmpty()) {
            return;
        }
        SpellUpgradeSession session = new SpellUpgradeSession(DUNGEON_ENTRY_UPGRADES, choices);
        pendingSpellUpgrades.put(player.getUniqueId(), session);
        ChatMessageUtil.send(player, MessageType.INFO,
                definition.displayName() + " grants " + ChatColor.AQUA + DUNGEON_ENTRY_UPGRADES
                        + ChatColor.GRAY + " temporary spell upgrades for this run.");
        Bukkit.getScheduler().runTask(plugin, () -> openSpellUpgradeGui(player, session));
    }

    private void openSpellUpgradeGui(Player player, SpellUpgradeSession session) {
        if (player == null || session == null) return;
        Inventory inv = Bukkit.createInventory(player, 27, SPELL_UPGRADE_TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inv.getSize(); slot++) {
            inv.setItem(slot, filler);
        }
        for (int i = 0; i < SPELL_UPGRADE_SLOTS.length && i < session.choices().size(); i++) {
            inv.setItem(SPELL_UPGRADE_SLOTS[i], createSpellUpgradeItem(player, session.choices().get(i), session.remainingSelections()));
        }
        player.openInventory(inv);
    }

    private ItemStack createSpellUpgradeItem(Player player, SpellUpgradeChoice choice, int remainingSelections) {
        Material icon = choice.unlock() ? Material.BOOK : Material.ENCHANTED_BOOK;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((choice.unlock() ? ChatColor.GREEN : ChatColor.AQUA) + choice.displayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + (choice.unlock() ? "Unlock this spell for the run." : "Upgrade this spell for the run."));
            lore.add(" ");
            lore.add(TooltipUtil.sectionHeader("Run Upgrade"));
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Remaining Choices: " + ChatColor.WHITE + remainingSelections));
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Current Run Rank: " + ChatColor.WHITE
                    + SpellProgressionManager.getInstance().getSpellLevel(player.getUniqueId(), choice.baseSpellId())));
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Next Spell: " + ChatColor.WHITE + choice.resultDisplayName()));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to choose this temporary upgrade", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<SpellUpgradeChoice> rollSpellUpgradeChoices(Player player, int count) {
        SpellProgressionManager progression = SpellProgressionManager.getInstance();
        List<SpellUpgradeChoice> candidates = new ArrayList<>();
        for (String baseId : progression.getUpgradeableBaseSpellsForPlayer(player, true)) {
            SpellUpgradeChoice choice = spellUpgradeChoiceFor(player, baseId);
            if (choice != null) {
                candidates.add(choice);
            }
        }
        List<SpellUpgradeChoice> rolled = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        while (!candidates.isEmpty() && rolled.size() < count) {
            rolled.add(candidates.remove(rng.nextInt(candidates.size())));
        }
        return rolled;
    }

    private SpellUpgradeChoice spellUpgradeChoiceFor(Player player, String baseSpellId) {
        if (player == null || baseSpellId == null) return null;
        SpellProgressionManager progressionManager = SpellProgressionManager.getInstance();
        SpellRegistry registry = SpellRegistry.getInstance();
        String base = baseSpellId.toLowerCase(Locale.ROOT);
        int current = progressionManager.getSpellLevel(player.getUniqueId(), base);
        int max = progressionManager.getMaxLevel(base);
        if (current >= max) return null;
        SpellProgression progression = registry.getProgression(base);
        if (progression == null || progression.upgradeSpellIds().isEmpty()) return null;
        String resultId;
        boolean unlock = current <= 0;
        if (unlock) {
            var baseEntry = registry.getSpell(base);
            if (baseEntry == null || baseEntry.definition() == null) return null;
            resultId = progression.upgradeSpellIds().get(0);
            var resultEntry = registry.getSpell(resultId);
            return new SpellUpgradeChoice(base, "Unlock " + baseEntry.definition().displayName(),
                    resultEntry == null || resultEntry.definition() == null ? resultId : resultEntry.definition().displayName(), true);
        }
        resultId = progression.upgradeSpellIds().get(current);
        var upgraded = registry.getSpell(resultId);
        if (upgraded == null || upgraded.definition() == null) return null;
        return new SpellUpgradeChoice(base, "Upgrade: " + upgraded.definition().displayName(), upgraded.definition().displayName(), false);
    }

    private void clearDungeonSpellUpgrades(UUID playerId) {
        pendingSpellUpgrades.remove(playerId);
        SpellProgressionManager.getInstance().clearTemporarySpellLevels(playerId);
    }

    private int applyDungeonYieldPetBonus(Player player, StagedDungeonDefinition definition, int reward) {
        if (player == null || definition == null || reward <= 0 || plugin.getPetManager() == null) return reward;
        PetEffectType type = switch (definition.id().toLowerCase(Locale.ROOT)) {
            case "gem" -> PetEffectType.GEM_DUNGEON_YIELD;
            case "coin" -> PetEffectType.COIN_DUNGEON_YIELD;
            default -> null;
        };
        return type == null ? reward : plugin.getPetManager().applyActiveEffectMultiplier(player.getUniqueId(), type, reward);
    }

    public int getTotalSweepAttempts(Player player, StagedDungeonDefinition definition) {
        if (player == null || definition == null) return 0;
        int base = Math.max(0, definition.sweepAttempts());
        if (plugin.getPetManager() == null) return base;
        int bonus = (int) Math.floor(plugin.getPetManager()
                .getActiveEffectValue(player.getUniqueId(), PetEffectType.STAGED_DUNGEON_SWEEP_ATTEMPTS));
        return Math.max(0, base + Math.max(0, bonus));
    }

    private record SpellUpgradeSession(int remainingSelections, List<SpellUpgradeChoice> choices) {}
    private record SpellUpgradeChoice(String baseSpellId, String displayName, String resultDisplayName, boolean unlock) {}

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
