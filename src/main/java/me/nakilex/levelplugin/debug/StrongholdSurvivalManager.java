package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.StrongholdWorldUtil;
import me.nakilex.levelplugin.utils.RewardBombUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;

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
    private static final String STRONGHOLD_DOOR_KEY_TAG = "stronghold_door_key";
    private static final int FINAL_WAVE = 30;
    private static final int BASE_WAVE_SECONDS = 50;
    private static final double DEFAULT_BORDER_INITIAL_SIZE = 750.0;
    private static final double DEFAULT_BORDER_SHRINK_PER_WAVE = 5.5;
    private static final int BORDER_WARNING_DISTANCE = 12;
    private static final int BASE_SCORE_MAX = 10000;
    private static final String CHOICE_GUI_TITLE = "Stronghold Path Node";
    private static final int CHOICE_RISK_SLOT = 11;
    private static final int CHOICE_BALANCE_SLOT = 15;
    private static final int OBJECTIVE_WINDOW_SECONDS = 25;
    private static final Set<Material> WAVE_SPAWN_GROUND = EnumSet.of(Material.GRASS_BLOCK, Material.COARSE_DIRT);

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

    private enum StrongholdMutator {
        NONE("Calm Frontier", 1.0, 0, 1.0),
        SHADOW_SIEGE("Shadow Siege", 1.2, -6, 1.15),
        IRON_GARRISON("Iron Garrison", 1.35, -10, 1.28),
        QUICKENED_RITUAL("Quickened Ritual", 1.1, -14, 1.2);

        private final String displayName;
        private final double mobScale;
        private final int waveTimeOffset;
        private final double scoreMultiplier;

        StrongholdMutator(String displayName, double mobScale, int waveTimeOffset, double scoreMultiplier) {
            this.displayName = displayName;
            this.mobScale = mobScale;
            this.waveTimeOffset = waveTimeOffset;
            this.scoreMultiplier = scoreMultiplier;
        }
    }

    private enum PathChoice {
        RISK("Blood Route", 1.20, 0.10, 0.0),
        BALANCED("Tactician Route", 1.0, 0.04, 0.07);

        private final String displayName;
        private final double nextWaveMobScale;
        private final double scoreBonus;
        private final double damageMitigation;

        PathChoice(String displayName, double nextWaveMobScale, double scoreBonus, double damageMitigation) {
            this.displayName = displayName;
            this.nextWaveMobScale = nextWaveMobScale;
            this.scoreBonus = scoreBonus;
            this.damageMitigation = damageMitigation;
        }
    }

    private enum WaveObjectiveType {
        SPEED_CLEAR("Clear this wave quickly", 320),
        CLEAN_OPEN("Take no damage for 20 seconds", 260),
        ELITE_RUSH("Slay 2 enemies within 25 seconds", 300);

        private final String displayName;
        private final int scoreBonus;

        WaveObjectiveType(String displayName, int scoreBonus) {
            this.displayName = displayName;
            this.scoreBonus = scoreBonus;
        }
    }

    public record StageStatus(int wave, int mobsRemaining, int secondsLeft) {
    }

    private final Main plugin;
    private final org.bukkit.NamespacedKey strongholdDoorKeyTag;
    private final Map<UUID, Run> runsByPlayer = new HashMap<>();
    private final Map<UUID, Run> runsByWorld = new HashMap<>();
    private final Map<UUID, UUID> mobToOwner = new HashMap<>();
    private final Map<UUID, Run> pendingChoiceByLeader = new HashMap<>();
    private final Map<UUID, Long> mobSpawnedAtMs = new HashMap<>();
    private static double borderInitialSize = DEFAULT_BORDER_INITIAL_SIZE;
    private static double borderShrinkPerWave = DEFAULT_BORDER_SHRINK_PER_WAVE;

    public StrongholdSurvivalManager(Main plugin) {
        this.plugin = plugin;
        this.strongholdDoorKeyTag = new org.bukkit.NamespacedKey(plugin, STRONGHOLD_DOOR_KEY_TAG);
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

    public static double getBorderInitialSize() {
        return borderInitialSize;
    }

    public static double getBorderShrinkPerWave() {
        return borderShrinkPerWave;
    }

    public static void setBorderInitialSize(double value) {
        borderInitialSize = Math.max(100.0, Math.min(2000.0, value));
    }

    public static void setBorderShrinkPerWave(double value) {
        borderShrinkPerWave = Math.max(0.5, Math.min(20.0, value));
    }

    public static void resetBorderSettings() {
        borderInitialSize = DEFAULT_BORDER_INITIAL_SIZE;
        borderShrinkPerWave = DEFAULT_BORDER_SHRINK_PER_WAVE;
    }

    public void recordDoorOpened(UUID playerId) {
        Run run = runsByPlayer.get(playerId);
        if (run != null) {
            run.doorsOpened++;
        }
    }

    public void recordChestOpened(UUID playerId) {
        Run run = runsByPlayer.get(playerId);
        if (run != null) {
            run.chestsOpened++;
        }
    }

    public boolean consumeDoorKey(Player player) {
        if (player == null) return false;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (!isStrongholdDoorKey(stack)) {
                continue;
            }
            int next = stack.getAmount() - 1;
            if (next <= 0) {
                player.getInventory().removeItem(stack);
            } else {
                stack.setAmount(next);
            }
            return true;
        }
        return false;
    }

    public ItemStack createStrongholdDoorKey() {
        ItemStack stack = new ItemStack(Material.TRIAL_KEY);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Stronghold Gate Key");
            meta.setLore(TooltipUtil.dungeonItemLore(
                    "Consumed to open sealed stronghold doors.", true));
            meta.getPersistentDataContainer().set(ItemUtil.DUNGEON_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(strongholdDoorKeyTag, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        ItemUtil.setSoulbound(stack, true);
        return stack;
    }

    public boolean isStrongholdDoorKey(ItemStack stack) {
        if (stack == null || stack.getType() != Material.TRIAL_KEY || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(strongholdDoorKeyTag, PersistentDataType.BYTE);
    }

    public void startRun(Player player) {
        if (player == null) {
            return;
        }
        startRun(List.of(player),  Math.max(50, me.nakilex.levelplugin.items.utils.ItemUtil.calculateTotalGearScore(player)));
    }

    public void startRun(List<Player> party, int averageGearScore) {
        if (party == null || party.isEmpty()) {
            return;
        }
        Player leader = party.getFirst();
        World world = leader.getWorld();
        if (!isStrongholdWorld(world)) {
            ChatMessageUtil.send(leader, ChatMessageUtil.MessageType.WARNING,
                    "Stronghold survival can only start inside a stronghold world.");
            return;
        }
        for (Player member : party) {
            stopRun(member.getUniqueId(), true);
        }
        List<UUID> memberIds = party.stream().map(Player::getUniqueId).toList();
        Run run = new Run(leader.getUniqueId(), world.getUID(), memberIds, Math.max(50, averageGearScore));
        run.mutator = rollMutator();
        run.teamplayScoreBonus = Math.max(0.0, (memberIds.size() - 1) * 0.03);
        for (UUID memberId : memberIds) {
            runsByPlayer.put(memberId, run);
        }
        runsByWorld.put(world.getUID(), run);
        initializeRunBorder(run, leader);
        for (Player member : party) {
            ChatMessageUtil.send(member, ChatMessageUtil.MessageType.INFO,
                    ChatColor.GRAY + "Objective: survive all " + ChatColor.GOLD + FINAL_WAVE
                            + ChatColor.GRAY + " waves and defeat the final boss.");
            ChatMessageUtil.send(member, ChatMessageUtil.MessageType.INFO,
                    ChatColor.GRAY + "Difficulty scales with party size (" + ChatColor.GOLD + run.members.size()
                            + ChatColor.GRAY + ") and average gear score (" + ChatColor.GOLD + run.averageGearScore + ChatColor.GRAY + ").");
            ChatMessageUtil.send(member, ChatMessageUtil.MessageType.INFO,
                    ChatColor.GRAY + "Mutator: " + ChatColor.LIGHT_PURPLE + run.mutator.displayName
                            + ChatColor.GRAY + " (score x" + String.format("%.2f", run.mutator.scoreMultiplier) + ").");
        }
        scheduleWaveStart(run, 10, false);
    }

    public void stopRun(UUID playerId, boolean silent) {
        Run run = runsByPlayer.get(playerId);
        if (!isRunTrackedAndActive(run)) {
            return;
        }
        run.active = false;
        for (UUID member : run.members) {
            runsByPlayer.remove(member);
        }
        pendingChoiceByLeader.remove(run.playerId);
        runsByWorld.remove(run.worldId);
        if (run.waveTask != null) {
            run.waveTask.cancel();
            run.waveTask = null;
        }
        if (run.countdownTask != null) {
            run.countdownTask.cancel();
            run.countdownTask = null;
        }
        if (run.bossBar != null) {
            run.bossBar.removeAll();
            run.bossBar = null;
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
        if (!silent) {
            for (UUID member : run.members) {
                Player player = Bukkit.getPlayer(member);
                if (player != null && player.isOnline()) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Stronghold survival ended.");
                }
            }
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        UUID mobId = event.getEntity().getUniqueId();
        UUID ownerId = mobToOwner.remove(mobId);
        long spawnedAtMs = mobSpawnedAtMs.getOrDefault(mobId, 0L);
        mobSpawnedAtMs.remove(mobId);
        if (ownerId == null) {
            return;
        }
        Run run = runsByPlayer.get(ownerId);
        if (run == null || !run.active) {
            return;
        }
        run.mobIds.remove(mobId);
        run.mobsRemaining = Math.max(0, run.mobsRemaining - 1);
        if (run.waveObjective == WaveObjectiveType.ELITE_RUSH
                && spawnedAtMs > 0L
                && System.currentTimeMillis() - spawnedAtMs <= OBJECTIVE_WINDOW_SECONDS * 1000L) {
            run.waveObjectiveCounter++;
        }
        double keyChance = 0.05D + partyEffectBonus(run, PetEffectType.STRONGHOLD_KEY_LUCK);
        if (ThreadLocalRandom.current().nextDouble() <= Math.min(0.30D, keyChance)) {
            event.getDrops().add(createStrongholdDoorKey());
        }
        updateBossBar(run);
        if (run.mobsRemaining <= 0) {
            completeWave(run);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopRun(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Run run = runsByPlayer.get(player.getUniqueId());
        if (run == null || !run.active) {
            return;
        }
        double mitigated = Math.max(0.0, event.getFinalDamage() * (1.0 - run.damageMitigationBonus));
        run.damageTaken += mitigated;
        if (run.waveObjective == WaveObjectiveType.CLEAN_OPEN
                && System.currentTimeMillis() - run.waveObjectiveStartedAtMs <= 20_000L) {
            run.waveObjectiveFailed = true;
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (isStrongholdWorld(player.getWorld())) {
            return;
        }
        int removed = stripStrongholdKeys(player);
        if (removed > 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    ChatColor.GRAY + "Your " + ChatColor.GOLD + "Stronghold Gate Key"
                            + ChatColor.GRAY + " dissolved outside the stronghold.");
        }
    }

    @EventHandler
    public void onChoiceClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), CHOICE_GUI_TITLE)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        Run run = pendingChoiceByLeader.get(player.getUniqueId());
        if (run == null || !run.active) {
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() == CHOICE_RISK_SLOT) {
            applyPathChoice(run, PathChoice.RISK, player);
        } else if (event.getRawSlot() == CHOICE_BALANCE_SLOT) {
            applyPathChoice(run, PathChoice.BALANCED, player);
        } else {
            return;
        }
        pendingChoiceByLeader.remove(player.getUniqueId());
        player.closeInventory();
        scheduleWaveStart(run, 5, true);
    }

    @EventHandler
    public void onChoiceClose(InventoryCloseEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), CHOICE_GUI_TITLE)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Run run = pendingChoiceByLeader.get(player.getUniqueId());
        if (run == null || !run.active) {
            pendingChoiceByLeader.remove(player.getUniqueId());
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!pendingChoiceByLeader.containsKey(player.getUniqueId()) || !isRunTrackedAndActive(run)) {
                return;
            }
            pendingChoiceByLeader.remove(player.getUniqueId());
            applyPathChoice(run, PathChoice.BALANCED, player.isOnline() ? player : null);
            scheduleWaveStart(run, 5, true);
        }, 2L);
    }

    private void scheduleWaveStart(Run run, int countdownSeconds, boolean announceBuff) {
        if (!isRunTrackedAndActive(run)) {
            return;
        }
        if (run.countdownTask != null) {
            run.countdownTask.cancel();
            run.countdownTask = null;
        }
        run.pendingWaveBuffAnnouncement = announceBuff;
        run.countdownSecondsRemaining = Math.max(1, countdownSeconds);
        sendWaveCountdown(run, run.countdownSecondsRemaining);
        run.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isRunTrackedAndActive(run)) {
                if (run.countdownTask != null) {
                    run.countdownTask.cancel();
                    run.countdownTask = null;
                }
                return;
            }
            run.countdownSecondsRemaining--;
            if (run.countdownSecondsRemaining <= 0) {
                if (run.countdownTask != null) {
                    run.countdownTask.cancel();
                    run.countdownTask = null;
                }
                beginWave(run, run.pendingWaveBuffAnnouncement);
                return;
            }
            sendWaveCountdown(run, run.countdownSecondsRemaining);
        }, 20L, 20L);
    }

    private void sendWaveCountdown(Run run, int secondsRemaining) {
        if (run == null || secondsRemaining <= 0) {
            return;
        }
        if (!(secondsRemaining <= 5 || secondsRemaining == 10)) {
            return;
        }
        for (UUID member : run.members) {
            Player player = Bukkit.getPlayer(member);
            if (player == null || !player.isOnline()) {
                continue;
            }
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    ChatColor.GRAY + "Next wave starts in " + ChatColor.GOLD + secondsRemaining + ChatColor.GRAY + "s.");
        }
    }

    private void beginWave(Run run, boolean announceBuff) {
        Player player = resolveAnchor(run);
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
        run.waveObjective = pickWaveObjective(run);
        run.waveObjectiveStartedAtMs = System.currentTimeMillis();
        run.waveObjectiveCounter = 0;
        run.waveObjectiveFailed = false;
        run.mobIds.clear();
        run.mobsRemaining = spawnWaveMobs(run, player);
        run.waveDeadlineMs = System.currentTimeMillis() + (runWaveSeconds(run) * 1000L);
        applyWaveBorder(run);
        if (run.bossBar == null) {
            run.bossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        }
        run.bossBar.removeAll();
        for (UUID member : run.members) {
            Player online = Bukkit.getPlayer(member);
            if (online != null && online.isOnline()) {
                run.bossBar.addPlayer(online);
            }
        }
        if (run.wave >= FINAL_WAVE) {
            run.bossBar.setColor(BarColor.RED);
        } else if (isEliteWave(run.wave)) {
            run.bossBar.setColor(BarColor.PURPLE);
        } else {
            run.bossBar.setColor(BarColor.BLUE);
        }
        updateBossBar(run);
        for (UUID member : run.members) {
            Player online = Bukkit.getPlayer(member);
            if (online == null || !online.isOnline()) continue;
            online.sendTitle(
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Wave " + run.wave,
                    ChatColor.GRAY + (isEliteWave(run.wave) ? "Elite encounter" : "Defeat all enemies"),
                    8, 30, 10
            );
            online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
            if (announceBuff && run.lastBuff != null) {
                ChatMessageUtil.send(online, ChatMessageUtil.MessageType.SUCCESS, run.lastBuff.message);
            }
            if (run.waveObjective != null) {
                ChatMessageUtil.send(online, ChatMessageUtil.MessageType.INFO,
                        ChatColor.DARK_GRAY + "Objective: " + ChatColor.WHITE + run.waveObjective.displayName
                                + ChatColor.DARK_GRAY + " (+" + run.waveObjective.scoreBonus + " score)");
            }
            boolean alertEnabled = plugin.getPetManager() != null
                    && plugin.getPetManager().getActiveEffectValue(online.getUniqueId(), PetEffectType.STRONGHOLD_ALERT) > 0.0;
            if (alertEnabled && (isEliteWave(run.wave) || run.wave >= FINAL_WAVE)) {
                ChatMessageUtil.send(online, ChatMessageUtil.MessageType.WARNING,
                        ChatColor.GRAY + "Warden Pup senses danger. " + ChatColor.RED + "Brace for a deadly wave.");
            }
        }
        if (run.waveTask != null) {
            run.waveTask.cancel();
        }
        run.waveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickWave(run), 20L, 20L);
        if (plugin.getScoreboardManager() != null) {
            for (UUID member : run.members) {
                Player online = Bukkit.getPlayer(member);
                if (online != null && online.isOnline()) {
                    plugin.getScoreboardManager().updateBoard(online);
                }
            }
        }
    }

    private void tickWave(Run run) {
        Player player = resolveAnchor(run);
        if (player == null || !player.isOnline()) {
            stopRun(run.playerId, true);
            return;
        }
        if (!isRunTrackedAndActive(run)) {
            stopRun(run.playerId, true);
            return;
        }
        if (System.currentTimeMillis() >= run.waveDeadlineMs) {
            for (UUID member : run.members) {
                Player online = Bukkit.getPlayer(member);
                if (online == null || !online.isOnline()) continue;
                ChatMessageUtil.send(online, ChatMessageUtil.MessageType.ERROR,
                        "Wave timer expired. The stronghold overwhelms you.");
                online.sendTitle(ChatColor.RED + "Run Failed", ChatColor.GRAY + "Wave " + run.wave, 8, 40, 10);
            }
            stopRun(run.playerId, true);
            return;
        }
        if (run.mobsRemaining <= 0) {
            completeWave(run);
            return;
        }
        updateBossBar(run);
        if (plugin.getScoreboardManager() != null) {
            for (UUID member : run.members) {
                Player online = Bukkit.getPlayer(member);
                if (online != null && online.isOnline()) {
                    plugin.getScoreboardManager().updateBoard(online);
                }
            }
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
        int partyScale = Math.max(1, run.members.size());
        double gearScale = Math.max(0.8, run.averageGearScore / 900.0);
        int count = bossWave ? Math.max(1, partyScale / 2) : eliteWave
                ? Math.max(2, (wave / 5) + partyScale - 1)
                : Math.min(36, 3 + wave + (partyScale * 2));
        count = Math.max(1, (int) Math.round(count * (run.mutator != null ? run.mutator.mobScale : 1.0)));
        count = Math.max(1, (int) Math.round(count * run.nextWaveMobScaleBonus));
        run.nextWaveMobScaleBonus = 1.0;
        int level = Math.max(1, (int) Math.round((4 + (wave * 2)) * gearScale));
        String forcedMob = bossWave ? pickAvailableMob(mobManager, BOSS_POOL)
                : eliteWave ? pickAvailableMob(mobManager, ELITE_POOL)
                : pickAvailableMob(mobManager, poolForWave(wave));
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            String mobId = forcedMob != null ? forcedMob : pickAvailableMob(mobManager, poolForWave(wave));
            if (mobId == null) {
                continue;
            }
            Location spawn = randomSpawnAround(pickSpawnAnchor(run, player), 8.0, 17.0);
            List<LivingEntity> entities = mobManager.spawn(mobId, spawn, 1, level);
            if (entities.isEmpty()) {
                continue;
            }
            LivingEntity entity = entities.getFirst();
            entity.addScoreboardTag(WAVE_TAG);
            entity.addScoreboardTag(WAVE_TAG + ":" + player.getUniqueId());
            mobToOwner.put(entity.getUniqueId(), player.getUniqueId());
            mobSpawnedAtMs.put(entity.getUniqueId(), System.currentTimeMillis());
            run.mobIds.add(entity.getUniqueId());
            spawned++;
        }
        return spawned;
    }

    private void completeWave(Run run) {
        Player player = resolveAnchor(run);
        if (player == null || !player.isOnline()) {
            stopRun(run.playerId, true);
            return;
        }
        int xpReward = (60 + (run.wave * 18)) * Math.max(1, run.members.size());
        int equalShare = Math.max(1, xpReward / Math.max(1, run.members.size()));
        for (UUID member : run.members) {
            Player online = Bukkit.getPlayer(member);
            if (online == null || !online.isOnline()) continue;
            plugin.getLevelManager().addXP(online, equalShare);
            ChatMessageUtil.send(online, ChatMessageUtil.MessageType.REWARD,
                    ChatColor.GOLD + "Wave " + run.wave + " cleared "
                            + ChatColor.GRAY + "• +" + equalShare + " <glyph:experience_orb_icon> XP");
        }
        applyObjectiveResult(run);
        if (run.wave >= FINAL_WAVE) {
            finishRun(run);
            return;
        }
        run.lastBuff = grantIntermissionBuff(run, run.wave);
        if (run.wave % 5 == 0) {
            openWaveChoice(run);
            return;
        }
        scheduleWaveStart(run, 5, true);
    }

    private void finishRun(Run run) {
        long elapsedMs = Math.max(1L, System.currentTimeMillis() - run.startedAtMs);
        ScoreBreakdown scoreBreakdown = calculateScoreBreakdown(run, elapsedMs);
        int score = scoreBreakdown.finalScore();
        String rank = rankForScore(score);
        String rankToken = rankToken(score);
        Player anchor = resolveAnchor(run);
        if (anchor != null) {
            if (plugin.getLootChestManager() != null) {
                int rankTierBonus = rankLootTierBonus(rankToken);
                Supplier<ItemStack> rewardSupplier = () -> plugin.getLootChestManager()
                        .getRandomLootForTier(Math.max(4, run.averageGearScore / 120) + rankTierBonus, "stronghold", null);
                int duration = switch (rankToken) {
                    case "S" -> 150;
                    case "A" -> 130;
                    case "B" -> 115;
                    case "C" -> 100;
                    default -> 85;
                };
                RewardBombUtil.startRewardBomb(plugin, anchor.getLocation(), rewardSupplier, duration);
            }
        }
        for (UUID member : run.members) {
            Player player = Bukkit.getPlayer(member);
            if (player == null || !player.isOnline()) continue;
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "STRONGHOLD CLEARED"
                            + ChatColor.GRAY + " • You survived all " + ChatColor.GOLD + FINAL_WAVE + ChatColor.GRAY + " waves.");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    ChatColor.GRAY + "Score " + ChatColor.GOLD + score + ChatColor.GRAY
                            + " • Rank " + rank);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, scoreBreakdown.summaryLine());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, scoreBreakdown.modifierLine());
            grantStrongholdBattlePassProgress(player, rankToken, elapsedMs, run);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    ChatColor.DARK_GRAY + "Time: " + ChatColor.WHITE + formatElapsed(elapsedMs)
                            + ChatColor.DARK_GRAY + " | Damage: " + ChatColor.WHITE + (int) Math.round(run.damageTaken)
                            + ChatColor.DARK_GRAY + " | Chests: " + ChatColor.WHITE + run.chestsOpened
                            + ChatColor.DARK_GRAY + " | Doors: " + ChatColor.WHITE + run.doorsOpened);
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
        double progress = run.mobsRemaining <= 0 ? 0.0 : Math.min(1.0, millisLeft / (runWaveSeconds(run) * 1000.0));
        run.bossBar.setProgress(Math.max(0.0, progress));
        run.bossBar.setTitle(ChatColor.GOLD + "Wave " + run.wave
                + ChatColor.DARK_GRAY + " • "
                + ChatColor.WHITE + run.mobsRemaining + " mobs"
                + ChatColor.DARK_GRAY + " • "
                + ChatColor.YELLOW + (millisLeft / 1000L) + "s");
    }

    private BuffResult grantIntermissionBuff(Run run, int wave) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int roll = random.nextInt(4);
        return switch (roll) {
            case 0 -> {
                for (Player player : onlineMembers(run)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 30, 0, true, false, true));
                }
                yield new BuffResult("Battle Boon", ChatColor.GRAY + "Boon: " + ChatColor.RED + "Fury "
                        + ChatColor.GRAY + "for 30s.");
            }
            case 1 -> {
                for (Player player : onlineMembers(run)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, 0, true, false, true));
                }
                yield new BuffResult("Guard Boon", ChatColor.GRAY + "Boon: " + ChatColor.BLUE + "Bulwark "
                        + ChatColor.GRAY + "for 30s.");
            }
            case 2 -> {
                for (Player player : onlineMembers(run)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 0, true, false, true));
                }
                yield new BuffResult("Mobility Boon", ChatColor.GRAY + "Boon: " + ChatColor.AQUA + "Haste "
                        + ChatColor.GRAY + "for 30s.");
            }
            default -> {
                int restored = Math.max(20, 30 + (wave * 2));
                for (Player player : onlineMembers(run)) {
                    StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
                    stats.setCurrentMana(Math.min(stats.getMaxMana(), stats.getCurrentMana() + restored));
                    double healed = Math.min(player.getMaxHealth(), player.getHealth() + (player.getMaxHealth() * 0.20));
                    player.setHealth(healed);
                }
                yield new BuffResult("Recovery Boon", ChatColor.GRAY + "Boon: " + ChatColor.GREEN + "Recovered "
                        + ChatColor.WHITE + restored + ChatColor.GRAY + " mana and health.");
            }
        };
    }

    private Player resolveAnchor(Run run) {
        for (UUID member : run.members) {
            Player online = Bukkit.getPlayer(member);
            if (online != null && online.isOnline()) {
                return online;
            }
        }
        return null;
    }

    private Location pickSpawnAnchor(Run run, Player fallback) {
        List<Player> online = onlineMembers(run);
        if (online.isEmpty()) {
            return fallback.getLocation();
        }
        return online.get(ThreadLocalRandom.current().nextInt(online.size())).getLocation();
    }

    private List<Player> onlineMembers(Run run) {
        List<Player> players = new ArrayList<>();
        for (UUID member : run.members) {
            Player online = Bukkit.getPlayer(member);
            if (online != null && online.isOnline()) {
                players.add(online);
            }
        }
        return players;
    }

    private WaveObjectiveType pickWaveObjective(Run run) {
        if (run == null) {
            return WaveObjectiveType.SPEED_CLEAR;
        }
        if (isEliteWave(run.wave)) {
            return WaveObjectiveType.ELITE_RUSH;
        }
        return ThreadLocalRandom.current().nextBoolean() ? WaveObjectiveType.SPEED_CLEAR : WaveObjectiveType.CLEAN_OPEN;
    }

    private void applyObjectiveResult(Run run) {
        if (run == null || run.waveObjective == null) {
            return;
        }
        boolean success = switch (run.waveObjective) {
            case SPEED_CLEAR -> (System.currentTimeMillis() - run.waveObjectiveStartedAtMs)
                    <= (runWaveSeconds(run) * 600L);
            case CLEAN_OPEN -> !run.waveObjectiveFailed;
            case ELITE_RUSH -> run.waveObjectiveCounter >= 2;
        };
        if (!success) {
            return;
        }
        run.bonusScoreFlat += run.waveObjective.scoreBonus;
        for (UUID member : run.members) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        ChatColor.GRAY + "Objective complete: " + ChatColor.GOLD + run.waveObjective.displayName
                                + ChatColor.GRAY + " (+" + run.waveObjective.scoreBonus + " score)");
            }
        }
    }

    private void openWaveChoice(Run run) {
        Player leader = Bukkit.getPlayer(run.playerId);
        if (leader == null || !leader.isOnline()) {
            applyPathChoice(run, PathChoice.BALANCED, null);
            scheduleWaveStart(run, 5, true);
            return;
        }
        Inventory inv = Bukkit.createInventory(leader, 27, CHOICE_GUI_TITLE);
        inv.setItem(CHOICE_RISK_SLOT, createPathChoiceItem(PathChoice.RISK));
        inv.setItem(CHOICE_BALANCE_SLOT, createPathChoiceItem(PathChoice.BALANCED));
        pendingChoiceByLeader.put(leader.getUniqueId(), run);
        leader.openInventory(inv);
        ChatMessageUtil.send(leader, ChatMessageUtil.MessageType.INFO,
                ChatColor.GRAY + "Choose the next stronghold route for your party.");
    }

    private ItemStack createPathChoiceItem(PathChoice choice) {
        Material material = choice == PathChoice.RISK ? Material.NETHER_STAR : Material.SHIELD;
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + choice.displayName);
            List<String> lore = new ArrayList<>();
            lore.addAll(TooltipUtil.bulletList(
                    "Next-wave mob scale: " + ChatColor.WHITE + String.format("x%.2f", choice.nextWaveMobScale),
                    "Score bonus: " + ChatColor.GREEN + "+" + (int) Math.round(choice.scoreBonus * 100) + "%"));
            if (choice.damageMitigation > 0) {
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Damage taken: " + ChatColor.AQUA + "-" + (int) Math.round(choice.damageMitigation * 100) + "%"));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to select this route", null));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void applyPathChoice(Run run, PathChoice choice, Player leader) {
        if (run == null || choice == null) {
            return;
        }
        run.nextWaveMobScaleBonus = choice.nextWaveMobScale;
        run.pathScoreBonus += choice.scoreBonus;
        run.damageMitigationBonus = Math.max(run.damageMitigationBonus, choice.damageMitigation);
        for (UUID member : run.members) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        ChatColor.GRAY + "Path chosen: " + ChatColor.GOLD + choice.displayName
                                + ChatColor.GRAY + (leader != null ? " by " + ChatColor.YELLOW + leader.getName() : ""));
            }
        }
    }

    private int stripStrongholdKeys(Player player) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!isStrongholdDoorKey(stack)) {
                continue;
            }
            removed += stack.getAmount();
            contents[i] = null;
        }
        player.getInventory().setContents(contents);
        return removed;
    }

    private int calculateScore(Run run, long elapsedMs) {
        return calculateScoreBreakdown(run, elapsedMs).finalScore();
    }

    private ScoreBreakdown calculateScoreBreakdown(Run run, long elapsedMs) {
        int timeComponent = Math.max(0, 4200 - (int) (elapsedMs / 1000L) * 9);
        int damageComponent = Math.max(0, 2600 - (int) Math.round(run.damageTaken * 2.5));
        int chestComponent = Math.min(1600, run.chestsOpened * 180);
        int doorComponent = Math.min(1600, run.doorsOpened * 220);
        int objectiveComponent = Math.max(0, run.bonusScoreFlat);
        int base = Math.max(0, timeComponent + damageComponent + chestComponent + doorComponent + objectiveComponent);
        double petBonus = partyEffectBonus(run, PetEffectType.STRONGHOLD_SCORE_BONUS);
        double mutatorMult = run.mutator != null ? run.mutator.scoreMultiplier : 1.0;
        double teamBonus = run.teamplayScoreBonus;
        double pathBonus = run.pathScoreBonus;
        double additiveBonus = 1.0 + petBonus + teamBonus + pathBonus;
        int adjusted = (int) Math.round(base * additiveBonus * mutatorMult);
        int finalScore = Math.max(0, Math.min(BASE_SCORE_MAX, adjusted));
        return new ScoreBreakdown(timeComponent, damageComponent, chestComponent, doorComponent, objectiveComponent,
                petBonus, teamBonus, pathBonus, mutatorMult, finalScore);
    }

    private double partyEffectBonus(Run run, PetEffectType effectType) {
        if (run == null || effectType == null || plugin.getPetManager() == null) {
            return 0.0;
        }
        double total = 0.0;
        for (UUID member : run.members) {
            total += plugin.getPetManager().getActiveEffectValue(member, effectType);
        }
        return total;
    }

    private String rankForScore(int score) {
        if (score >= 8500) return ChatColor.GOLD + "" + ChatColor.BOLD + "S";
        if (score >= 7200) return ChatColor.GREEN + "A";
        if (score >= 6000) return ChatColor.AQUA + "B";
        if (score >= 4800) return ChatColor.YELLOW + "C";
        if (score >= 3600) return ChatColor.GOLD + "D";
        if (score >= 2400) return ChatColor.RED + "E";
        return ChatColor.DARK_RED + "F";
    }

    private String rankToken(int score) {
        if (score >= 8500) return "S";
        if (score >= 7200) return "A";
        if (score >= 6000) return "B";
        if (score >= 4800) return "C";
        if (score >= 3600) return "D";
        if (score >= 2400) return "E";
        return "F";
    }

    private int rankLootTierBonus(String rankToken) {
        return switch (rankToken) {
            case "S" -> 3;
            case "A" -> 2;
            case "B" -> 1;
            default -> 0;
        };
    }

    private void grantStrongholdBattlePassProgress(Player player, String rankToken, long elapsedMs, Run run) {
        if (player == null || plugin.getBattlePassManager() == null) {
            return;
        }
        int xp = switch (rankToken) {
            case "S" -> 1200;
            case "A" -> 900;
            case "B" -> 700;
            case "C" -> 500;
            default -> 300;
        };
        if (elapsedMs <= 16 * 60 * 1000L) {
            xp += 120;
        }
        if (run.chestsOpened >= 4) {
            xp += 80;
        }
        if (run.doorsOpened >= 3) {
            xp += 80;
        }
        plugin.getBattlePassManager().addProgress(player, xp,
                "for clearing Stronghold (" + rankToken + " rank)");
    }

    private StrongholdMutator rollMutator() {
        StrongholdMutator[] values = StrongholdMutator.values();
        int week = Instant.now().atZone(ZoneOffset.UTC).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return values[Math.floorMod(week, values.length)];
    }

    private String formatElapsed(long elapsedMs) {
        long totalSeconds = Math.max(0L, elapsedMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private int runWaveSeconds(Run run) {
        int adjusted = BASE_WAVE_SECONDS + (run.mutator != null ? run.mutator.waveTimeOffset : 0);
        return Math.max(24, adjusted);
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
        World world = base.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble(0.0, Math.PI * 2.0);
            double radius = random.nextDouble(minRadius, maxRadius);
            double x = base.getX() + (Math.cos(angle) * radius);
            double z = base.getZ() + (Math.sin(angle) * radius);
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            int y = world.getHighestBlockYAt(blockX, blockZ);
            var ground = world.getBlockAt(blockX, y, blockZ);
            if (!WAVE_SPAWN_GROUND.contains(ground.getType())) {
                continue;
            }
            if (!world.getBlockAt(blockX, y + 1, blockZ).isPassable() || !world.getBlockAt(blockX, y + 2, blockZ).isPassable()) {
                continue;
            }
            return new Location(world, x, y + 1.0, z);
        }
        int fallbackY = world.getHighestBlockYAt(base.getBlockX(), base.getBlockZ());
        return new Location(world, base.getX(), fallbackY + 1.0, base.getZ());
    }

    private boolean isStrongholdWorld(World world) {
        return StrongholdWorldUtil.isStrongholdWorld(world);
    }

    private boolean isRunTrackedAndActive(Run run) {
        if (run == null || !run.active) {
            return false;
        }
        Run worldRun = runsByWorld.get(run.worldId);
        return worldRun == run;
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
        border.setSize(borderInitialSize);
    }

    private void applyWaveBorder(Run run) {
        if (run == null) {
            return;
        }
        World world = Bukkit.getWorld(run.worldId);
        if (world == null) {
            return;
        }
        double nextSize = Math.max(1.0, borderInitialSize - ((run.wave - 1) * borderShrinkPerWave));
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

    private record ScoreBreakdown(int timeComponent,
                                  int damageComponent,
                                  int chestComponent,
                                  int doorComponent,
                                  int objectiveComponent,
                                  double petBonus,
                                  double teamBonus,
                                  double pathBonus,
                                  double mutatorMultiplier,
                                  int finalScore) {
        private String summaryLine() {
            return ChatColor.DARK_GRAY + "Breakdown: "
                    + ChatColor.WHITE + "time " + timeComponent
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "damage " + damageComponent
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "chests " + chestComponent
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "doors " + doorComponent
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "objective " + objectiveComponent;
        }

        private String modifierLine() {
            return ChatColor.DARK_GRAY + "Modifiers: "
                    + ChatColor.WHITE + "pet +" + percent(petBonus)
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "team +" + percent(teamBonus)
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "path +" + percent(pathBonus)
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "mutator x" + String.format("%.2f", mutatorMultiplier);
        }

        private static String percent(double value) {
            return (int) Math.round(value * 100) + "%";
        }
    }

    private static final class Run {
        private final UUID playerId;
        private final UUID worldId;
        private final List<UUID> members;
        private final int averageGearScore;
        private boolean active = true;
        private int wave = 0;
        private int mobsRemaining = 0;
        private long waveDeadlineMs = 0L;
        private final long startedAtMs = System.currentTimeMillis();
        private double damageTaken = 0.0;
        private int chestsOpened = 0;
        private int doorsOpened = 0;
        private int bonusScoreFlat = 0;
        private double teamplayScoreBonus = 0.0;
        private double pathScoreBonus = 0.0;
        private double damageMitigationBonus = 0.0;
        private double nextWaveMobScaleBonus = 1.0;
        private WaveObjectiveType waveObjective;
        private long waveObjectiveStartedAtMs = 0L;
        private int waveObjectiveCounter = 0;
        private boolean waveObjectiveFailed = false;
        private StrongholdMutator mutator = StrongholdMutator.NONE;
        private final Set<UUID> mobIds = new HashSet<>();
        private BossBar bossBar;
        private BukkitTask waveTask;
        private BukkitTask countdownTask;
        private int countdownSecondsRemaining = 0;
        private boolean pendingWaveBuffAnnouncement = false;
        private BuffResult lastBuff;
        private BorderState borderState;

        private Run(UUID playerId, UUID worldId, List<UUID> members, int averageGearScore) {
            this.playerId = playerId;
            this.worldId = worldId;
            this.members = new ArrayList<>(members);
            this.averageGearScore = averageGearScore;
        }
    }

    private record BuffResult(String id, String message) {
    }

    private record BorderState(Location center, double size, int warningDistance, int warningTime) {
    }
}
