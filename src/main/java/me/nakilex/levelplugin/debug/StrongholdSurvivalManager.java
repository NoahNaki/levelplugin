package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.RewardBombUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

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
    private final org.bukkit.NamespacedKey strongholdDoorKeyTag;
    private final Map<UUID, Run> runsByPlayer = new HashMap<>();
    private final Map<UUID, Run> runsByWorld = new HashMap<>();
    private final Map<UUID, UUID> mobToOwner = new HashMap<>();

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
        }
        beginWave(run, false);
    }

    public void stopRun(UUID playerId, boolean silent) {
        Run run = runsByPlayer.get(playerId);
        if (run == null) {
            return;
        }
        for (UUID member : run.members) {
            runsByPlayer.remove(member);
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
        if (ownerId == null) {
            return;
        }
        Run run = runsByPlayer.get(ownerId);
        if (run == null || !run.active) {
            return;
        }
        run.mobIds.remove(mobId);
        run.mobsRemaining = Math.max(0, run.mobsRemaining - 1);
        if (ThreadLocalRandom.current().nextDouble() <= 0.05D) {
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
        run.damageTaken += Math.max(0.0, event.getFinalDamage());
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
        run.mobIds.clear();
        run.mobsRemaining = spawnWaveMobs(run, player);
        run.waveDeadlineMs = System.currentTimeMillis() + (BASE_WAVE_SECONDS * 1000L);
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
        if (!run.active) {
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
        if (run.wave >= FINAL_WAVE) {
            finishRun(run);
            return;
        }
        run.lastBuff = grantIntermissionBuff(run, run.wave);
        beginWave(run, true);
    }

    private void finishRun(Run run) {
        long elapsedMs = Math.max(1L, System.currentTimeMillis() - run.startedAtMs);
        int score = calculateScore(run, elapsedMs);
        String rank = rankForScore(score);
        Player anchor = resolveAnchor(run);
        if (anchor != null) {
            if (plugin.getLootChestManager() != null) {
                Supplier<ItemStack> rewardSupplier = () -> plugin.getLootChestManager()
                        .getRandomLootForTier(Math.max(4, run.averageGearScore / 120), "stronghold", null);
                RewardBombUtil.startRewardBomb(plugin, anchor.getLocation(), rewardSupplier, 100);
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
        double progress = run.mobsRemaining <= 0 ? 0.0 : Math.min(1.0, millisLeft / (BASE_WAVE_SECONDS * 1000.0));
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
        int timeComponent = Math.max(0, 4200 - (int) (elapsedMs / 1000L) * 9);
        int damageComponent = Math.max(0, 2600 - (int) Math.round(run.damageTaken * 2.5));
        int chestComponent = Math.min(1600, run.chestsOpened * 180);
        int doorComponent = Math.min(1600, run.doorsOpened * 220);
        return Math.max(0, timeComponent + damageComponent + chestComponent + doorComponent);
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

    private String formatElapsed(long elapsedMs) {
        long totalSeconds = Math.max(0L, elapsedMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
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
        private final Set<UUID> mobIds = new HashSet<>();
        private BossBar bossBar;
        private BukkitTask waveTask;
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
