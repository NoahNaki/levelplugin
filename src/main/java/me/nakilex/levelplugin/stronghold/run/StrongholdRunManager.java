package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.spells.SpellCastManager;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellDefinition;
import me.nakilex.levelplugin.spells.SpellProgression;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.stronghold.StrongholdShrineManager;
import me.nakilex.levelplugin.stronghold.utils.StrongholdMobSpawnUtil;
import me.nakilex.levelplugin.utils.StrongholdWorldUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class StrongholdRunManager implements Listener {
    private static final String UPGRADE_GUI_TITLE = ChatColor.DARK_PURPLE + "Stronghold Upgrades";
    private static final int SHRINES_PER_RUN = 1;
    private static final int FIRST_WAVE_DELAY_SECONDS = 3;
    private static final int WAVE_INTERVAL_SECONDS = 5;
    private static final int AUTOCAST_TICK_INTERVAL = 4;
    private static final int BASE_XP_REQUIRED = 100;
    private static final double MIN_ENEMY_SPAWN_RADIUS = 5.0;
    private static final long BASE_AUTOCAST_COOLDOWN_MS = 1_400L;
    private static final long STUCK_PULL_DELAY_MS = 4_000L;
    private static final double STUCK_MOVE_EPSILON_SQ = 0.20 * 0.20;
    private static final double STUCK_PULL_DISTANCE = 6.0;
    private static final long MOB_RELOCATE_COOLDOWN_MS = 2_500L;
    private static final double MOB_RELOCATE_TRIGGER_DISTANCE = 30.0;
    private static final double MOB_RELOCATE_MIN_RADIUS = 4.0;
    private static final double MOB_RELOCATE_MAX_RADIUS = 10.0;

    private final Main plugin;
    private final StrongholdShrineManager shrineManager;
    private final Map<UUID, ActiveRun> activeRuns = new HashMap<>();
    private final List<String> waveMobPool = List.of("goblin_warrior", "goblin_archer", "goblin_assassin");
    private final Set<String> autoCastBasePool = new HashSet<>();
    private final Set<String> excludedAutoCastSpellIds = Set.of(
            "mage_blink", "mage_blink_phase", "mage_blink_rift",
            "archer_skybound",
            "rogue_razor_dash", "rogue_razor_dash_rift", "rogue_razor_dash_shade",
            "warrior_titan_vault"
    );

    public StrongholdRunManager(Main plugin, StrongholdShrineManager shrineManager) {
        this.plugin = plugin;
        this.shrineManager = shrineManager;
    }

    public void startSoloRun(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        World world = player.getWorld();
        if (!StrongholdWorldUtil.isStrongholdWorld(world)) {
            return;
        }
        UUID worldId = world.getUID();
        stopRun(worldId);

        Location origin = player.getLocation().clone();
        int shrines = shrineManager.spawnRandomShrines(origin, SHRINES_PER_RUN, 72, 250.0);
        if (shrines > 0) {
            send(player, MessageType.INFO, "Placed " + ChatColor.WHITE + shrines + ChatColor.GRAY + " shrine(s) around the stronghold.");
        }

        ActiveRun run = new ActiveRun(worldId, origin);
        activeRuns.put(worldId, run);
        run.start();
        send(player, MessageType.SUCCESS, "Stronghold waves started.");
    }

    public void stopAll() {
        for (UUID worldId : new ArrayList<>(activeRuns.keySet())) {
            stopRun(worldId);
        }
    }

    public boolean addDebugXp(Player player, int amount) {
        if (player == null || amount <= 0) {
            return false;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            return false;
        }
        return run.grantDebugXp(player, amount);
    }

    public StageStatus getStageStatus(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return null;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            return null;
        }
        return run.getStageStatus(playerId);
    }

    private void stopRun(UUID worldId) {
        ActiveRun existing = activeRuns.remove(worldId);
        if (existing != null) {
            existing.stop();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event == null || event.getEntity() == null) {
            return;
        }
        LivingEntity entity = event.getEntity();
        World world = entity.getWorld();
        if (world == null) {
            return;
        }
        ActiveRun run = activeRuns.get(world.getUID());
        if (run == null) {
            return;
        }
        run.onEntityDeath(entity);
    }

    @EventHandler
    public void onUpgradeGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getView() == null || !UPGRADE_GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null) {
            player.closeInventory();
            return;
        }
        run.handleUpgradeClick(player, event.getRawSlot());
    }

    @EventHandler
    public void onUpgradeGuiClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getView() == null || !UPGRADE_GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run != null) {
            run.handleUpgradeClose(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run != null) {
            run.hideProgressBar(player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSelectionDamageImmunity(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null || !run.isUpgradePaused(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSelectionRegenPause(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null || !run.isUpgradePaused(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPausedPlayerTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        ActiveRun run = activeRuns.get(player.getWorld().getUID());
        if (run == null || !run.isUpgradePaused(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (event.getEntity() instanceof Mob mob) {
            mob.setTarget(null);
        }
    }

    private void initializeAutoCastPool() {
        autoCastBasePool.clear();
        SpellRegistry registry = SpellRegistry.getInstance();
        for (String spellId : baseSpellIds()) {
            SpellRegistry.SpellEntry entry = registry.getSpell(spellId);
            if (entry == null) {
                continue;
            }
            SpellDefinition definition = entry.definition();
            if (definition == null || definition.movementSpell()) {
                continue;
            }
            if (excludedAutoCastSpellIds.contains(definition.id().toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (!isAllowedAutoCastSpellId(definition.id())) {
                continue;
            }
            autoCastBasePool.add(definition.id().toLowerCase(Locale.ROOT));
        }
    }

    private boolean isAllowedAutoCastSpellId(String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return false;
        }
        String normalized = spellId.toLowerCase(Locale.ROOT);
        return normalized.startsWith("mage_")
                || normalized.startsWith("archer_")
                || normalized.equals("meteor")
                || normalized.equals("blackhole");
    }

    private Set<String> baseSpellIds() {
        Set<String> ids = new HashSet<>();
        for (SpellProgression progression : SpellRegistry.getInstance().getAllProgressions()) {
            if (progression != null && progression.baseSpellId() != null) {
                ids.add(progression.baseSpellId().toLowerCase(Locale.ROOT));
            }
        }
        return ids;
    }

    private int xpRequiredForLevel(int level) {
        int safeLevel = Math.max(1, level);
        return BASE_XP_REQUIRED + ((safeLevel - 1) * 45);
    }

    private final class ActiveRun {
        private final UUID worldId;
        private final Location origin;
        private final List<UUID> spawned = new ArrayList<>();
        private final List<UUID> currentWaveSpawned = new ArrayList<>();
        private final Map<UUID, MobMotionState> mobMotionStates = new HashMap<>();
        private final Map<UUID, SurvivorState> playerStates = new HashMap<>();
        private final Set<UUID> pausedPlayers = new HashSet<>();

        private BukkitTask task;
        private BukkitTask autoCastTask;
        private int wave = 0;
        private int secondsUntilNextWave = FIRST_WAVE_DELAY_SECONDS;

        private ActiveRun(UUID worldId, Location origin) {
            this.worldId = worldId;
            this.origin = origin;
        }

        private void start() {
            World runWorld = plugin.getServer().getWorld(worldId);
            if (runWorld != null) {
                initializePlayers(runWorld);
            }
            this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                World world = plugin.getServer().getWorld(worldId);
                if (world == null || !StrongholdWorldUtil.isStrongholdWorld(world)) {
                    stopRun(worldId);
                    return;
                }
                syncRunPlayers(world);
                cleanupDeadSpawned();
                updateStuckMobPull(world);
                if (countAliveCurrentWave() > 0) {
                    return;
                }
                if (secondsUntilNextWave > 0) {
                    secondsUntilNextWave--;
                    return;
                }
                secondsUntilNextWave = WAVE_INTERVAL_SECONDS;
                wave++;
                spawnWave(world, wave);
            }, 20L, 20L);
            this.autoCastTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAutoCast, 20L, AUTOCAST_TICK_INTERVAL);
        }

        private void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (autoCastTask != null) {
                autoCastTask.cancel();
                autoCastTask = null;
            }
            for (UUID id : spawned) {
                var e = plugin.getServer().getEntity(id);
                if (e instanceof LivingEntity living && !living.isDead()) {
                    living.remove();
                }
            }
            spawned.clear();
            currentWaveSpawned.clear();
            mobMotionStates.clear();
            for (Map.Entry<UUID, SurvivorState> entry : new HashMap<>(playerStates).entrySet()) {
                restorePlayerAfterRun(entry.getKey(), entry.getValue());
            }
            playerStates.clear();
        }

        private void onEntityDeath(LivingEntity entity) {
            if (entity == null) {
                return;
            }
            UUID deadId = entity.getUniqueId();
            spawned.remove(deadId);
            currentWaveSpawned.remove(deadId);
            mobMotionStates.remove(deadId);

            Player killer = entity.getKiller();
            if (killer != null && killer.isOnline()) {
                handleMobKillXp(killer, entity);
            }
        }

        private void spawnWave(World world, int waveNumber) {
            List<Player> players = world.getPlayers().stream().filter(Player::isOnline).toList();
            if (players.isEmpty()) {
                return;
            }
            currentWaveSpawned.clear();
            int spawnCount = Math.min(10, 2 + waveNumber);
            for (int i = 0; i < spawnCount; i++) {
                Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                Location spawn = findSpawnNear(target.getLocation(), origin, 14.0, 30.0);
                if (spawn == null) {
                    continue;
                }
                LivingEntity mob = StrongholdMobSpawnUtil.spawnStrongholdHostile(plugin.getCustomMobManager(), waveMobPool, spawn);
                if (mob == null) {
                    continue;
                }
                spawned.add(mob.getUniqueId());
                currentWaveSpawned.add(mob.getUniqueId());
                mobMotionStates.put(mob.getUniqueId(), new MobMotionState(
                        mob.getLocation().clone(),
                        System.currentTimeMillis(),
                        System.currentTimeMillis()));
                if (mob instanceof Mob hostile) {
                    if (pausedPlayers.contains(target.getUniqueId())) {
                        hostile.setTarget(null);
                        hostile.setAI(false);
                    } else {
                        hostile.setTarget(target);
                    }
                }
                world.spawnParticle(Particle.SMOKE, spawn, 10, 0.2, 0.2, 0.2, 0.01);
            }
            for (Player player : players) {
                send(player, MessageType.INFO, "Wave " + ChatColor.WHITE + waveNumber + ChatColor.GRAY + " started.");
            }
        }

        private void initializePlayers(World world) {
            for (Player player : world.getPlayers()) {
                registerPlayer(player);
            }
        }

        private void syncRunPlayers(World world) {
            for (Player player : world.getPlayers()) {
                registerPlayer(player);
            }
            for (UUID playerId : new HashSet<>(playerStates.keySet())) {
                Player online = Bukkit.getPlayer(playerId);
                if (online == null || !online.isOnline() || !Objects.equals(online.getWorld().getUID(), worldId)) {
                    hideProgressBar(playerId);
                } else {
                    updateProgressBar(online, playerStates.get(playerId));
                }
            }
        }

        private void cleanupDeadSpawned() {
            spawned.removeIf(id -> {
                var entity = plugin.getServer().getEntity(id);
                return !(entity instanceof LivingEntity living) || living.isDead();
            });
            currentWaveSpawned.removeIf(id -> {
                var entity = plugin.getServer().getEntity(id);
                return !(entity instanceof LivingEntity living) || living.isDead();
            });
            mobMotionStates.entrySet().removeIf(entry -> {
                var entity = plugin.getServer().getEntity(entry.getKey());
                return !(entity instanceof LivingEntity living) || living.isDead();
            });
        }

        private void updateStuckMobPull(World world) {
            if (world == null) {
                return;
            }
            List<Player> players = world.getPlayers().stream().filter(Player::isOnline).toList();
            if (players.isEmpty()) {
                return;
            }
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, MobMotionState> entry : mobMotionStates.entrySet()) {
                UUID mobId = entry.getKey();
                var entity = plugin.getServer().getEntity(mobId);
                if (!(entity instanceof Mob mob) || mob.isDead()) {
                    continue;
                }
                MobMotionState state = entry.getValue();
                Location current = mob.getLocation();
                boolean movedRecently = state.lastLocation != null && current.distanceSquared(state.lastLocation) > STUCK_MOVE_EPSILON_SQ;
                if (movedRecently) {
                    state.lastLocation = current.clone();
                    state.lastMovedAtMs = now;
                }
                Player nearest = players.stream()
                        .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(current)))
                        .orElse(null);
                if (nearest == null) {
                    continue;
                }
                boolean stuckLongEnough = now - state.lastMovedAtMs >= STUCK_PULL_DELAY_MS;
                double distanceSqToNearest = nearest.getLocation().distanceSquared(current);
                boolean tooFarFromPlayer = distanceSqToNearest >= MOB_RELOCATE_TRIGGER_DISTANCE * MOB_RELOCATE_TRIGGER_DISTANCE;
                if (!shouldRelocateMob(stuckLongEnough, tooFarFromPlayer, now, state.lastTeleportAtMs)) {
                    continue;
                }
                Location pulled = tooFarFromPlayer
                        ? findSpawnNear(nearest.getLocation(), nearest.getLocation(), MOB_RELOCATE_MIN_RADIUS, MOB_RELOCATE_MAX_RADIUS)
                        : pullTowardPlayer(current, nearest.getLocation(), world);
                if (pulled == null) {
                    continue;
                }
                mob.teleport(pulled);
                mob.setTarget(nearest);
                state.lastLocation = pulled.clone();
                state.lastMovedAtMs = now;
                state.lastTeleportAtMs = now;
            }
        }

        private boolean shouldRelocateMob(boolean stuckLongEnough, boolean tooFarFromPlayer, long now, long lastTeleportAtMs) {
            if (!stuckLongEnough && !tooFarFromPlayer) {
                return false;
            }
            return now - lastTeleportAtMs >= MOB_RELOCATE_COOLDOWN_MS;
        }

        private Location pullTowardPlayer(Location mobLocation, Location playerLocation, World world) {
            if (mobLocation == null || playerLocation == null || world == null) {
                return null;
            }
            Vector toward = playerLocation.toVector().subtract(mobLocation.toVector());
            toward.setY(0.0);
            if (toward.lengthSquared() <= 0.0001) {
                return null;
            }
            double horizontalDistance = Math.sqrt(toward.lengthSquared());
            double pullDistance = Math.min(STUCK_PULL_DISTANCE, Math.max(0.5, horizontalDistance - 1.0));
            Vector offset = toward.normalize().multiply(pullDistance);
            Location destination = mobLocation.clone().add(offset);
            int y = world.getHighestBlockYAt(destination);
            destination.setY(y + 1.0);
            if (!destination.getBlock().getType().isAir()) {
                destination.add(0.0, 1.0, 0.0);
            }
            return destination;
        }

        private int countAliveCurrentWave() {
            int alive = 0;
            for (UUID id : currentWaveSpawned) {
                var entity = plugin.getServer().getEntity(id);
                if (entity instanceof LivingEntity living && !living.isDead()) {
                    alive++;
                }
            }
            return alive;
        }

        private int countAliveAllSpawned() {
            int alive = 0;
            for (UUID id : spawned) {
                var entity = plugin.getServer().getEntity(id);
                if (entity instanceof LivingEntity living && !living.isDead()) {
                    alive++;
                }
            }
            return alive;
        }

        private void registerPlayer(Player player) {
            if (player == null || !player.isOnline()) {
                return;
            }
            SurvivorState existing = playerStates.get(player.getUniqueId());
            if (existing != null) {
                if (existing.progressBar != null && !existing.progressBar.getPlayers().contains(player)) {
                    existing.progressBar.addPlayer(player);
                }
                updateProgressBar(player, existing);
                return;
            }
            PlayerClass currentClass = PlayerClassManager.getInstance().getPlayerClass(player);
            SurvivorState state = new SurvivorState(currentClass);
            playerStates.put(player.getUniqueId(), state);

            PlayerClassManager.getInstance().setPlayerClass(player, PlayerClass.VILLAGER);
            state.progressBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SOLID);
            state.progressBar.addPlayer(player);
            state.progressBar.setVisible(true);
            state.pendingUpgrades = rollUpgradeChoices(state, 3);
            updateProgressBar(player, state);
            send(player, MessageType.INFO, "Stronghold start: class set to " + ChatColor.WHITE + "Classless" + ChatColor.GRAY + ".");
            openUpgradeGui(player, state);
        }

        private void handleMobKillXp(Player killer, LivingEntity entity) {
            SurvivorState state = playerStates.get(killer.getUniqueId());
            if (state == null) {
                return;
            }
            int xpGain = 12 + Math.min(28, wave * 2);
            if (entity instanceof Mob) {
                xpGain += 3;
            }
            grantXp(killer, state, xpGain);
        }

        private void grantXp(Player player, SurvivorState state, int amount) {
            if (amount <= 0) {
                return;
            }
            state.xp += amount;
            int required = xpRequiredForLevel(state.level);
            boolean leveledUp = false;
            while (state.xp >= required) {
                state.xp -= required;
                state.level++;
                leveledUp = true;
                required = xpRequiredForLevel(state.level);
            }
            updateProgressBar(player, state);
            if (leveledUp) {
                send(player, MessageType.SUCCESS,
                        "Level up! " + ChatColor.WHITE + "Lv. " + state.level + ChatColor.GRAY + " reached.");
                state.pendingUpgrades = rollUpgradeChoices(state, 3);
                openUpgradeGui(player, state);
            }
        }

        private boolean grantDebugXp(Player player, int amount) {
            SurvivorState state = playerStates.get(player.getUniqueId());
            if (state == null) {
                return false;
            }
            grantXp(player, state, amount);
            return true;
        }

        private StageStatus getStageStatus(UUID playerId) {
            if (playerId == null || !playerStates.containsKey(playerId)) {
                return null;
            }
            return new StageStatus(Math.max(1, wave), countAliveAllSpawned());
        }

        private void updateProgressBar(Player player, SurvivorState state) {
            if (player == null || state == null || state.progressBar == null) {
                return;
            }
            int required = xpRequiredForLevel(state.level);
            double progress = required <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, state.xp / (double) required));
            state.progressBar.setTitle(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Stronghold Lv." + state.level
                    + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + state.xp + ChatColor.GRAY + "/" + ChatColor.WHITE + required + " XP");
            state.progressBar.setProgress(progress);
            state.progressBar.setVisible(true);
        }

        private void hideProgressBar(UUID playerId) {
            SurvivorState state = playerStates.get(playerId);
            if (state == null || state.progressBar == null) {
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                state.progressBar.removePlayer(player);
            }
            state.progressBar.setVisible(false);
        }

        private void openUpgradeGui(Player player, SurvivorState state) {
            if (player == null || state == null) {
                return;
            }
            if (state.pendingUpgrades == null || state.pendingUpgrades.isEmpty()) {
                state.pendingUpgrades = rollUpgradeChoices(state, 3);
            }
            state.awaitingUpgradeSelection = true;
            setUpgradePausedState(player, state, true);
            Inventory inv = Bukkit.createInventory(player, 27, UPGRADE_GUI_TITLE);
            inv.setItem(11, upgradeItem(state.pendingUpgrades.get(0), state));
            inv.setItem(13, upgradeItem(state.pendingUpgrades.get(1), state));
            inv.setItem(15, upgradeItem(state.pendingUpgrades.get(2), state));
            player.openInventory(inv);
        }

        private ItemStack upgradeItem(UpgradeChoice choice, SurvivorState state) {
            Material material = choice.type == UpgradeType.STAT ? Material.NETHER_STAR : Material.ENCHANTED_BOOK;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + choice.displayName);
                List<String> lore = new ArrayList<>();
                lore.addAll(TooltipUtil.bulletList(choice.description));
                if (choice.type == UpgradeType.SPELL_UNLOCK || choice.type == UpgradeType.SPELL_UPGRADE) {
                    int rank = state.ownedSpellRanks.getOrDefault(choice.baseSpellId, 0);
                    lore.add(ChatColor.GRAY + "Current Rank: " + ChatColor.WHITE + rank);
                    lore.add(ChatColor.GRAY + "Result Spell: " + ChatColor.WHITE + choice.resultSpellId);
                } else if (choice.type == UpgradeType.GLOBAL_COOLDOWN) {
                    lore.add(ChatColor.GRAY + "Cooldown Tier: " + ChatColor.WHITE + state.cooldownUpgradeTier);
                    lore.add(ChatColor.GRAY + "Effect: " + ChatColor.GREEN + "-10% global auto-cast cooldown");
                } else if (choice.statType != null) {
                    lore.add(ChatColor.GRAY + "Temporary Bonus: " + ChatColor.GREEN + "+" + choice.statAmount + " " + choice.statType.getDisplayName());
                }
                lore.add(TooltipUtil.sectionDividerByPixels(150));
                lore.addAll(TooltipUtil.clickInstructions("to choose this upgrade", null));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        private void handleUpgradeClick(Player player, int slot) {
            SurvivorState state = playerStates.get(player.getUniqueId());
            if (state == null || state.pendingUpgrades == null || state.pendingUpgrades.isEmpty()) {
                player.closeInventory();
                return;
            }
            int idx = switch (slot) {
                case 11 -> 0;
                case 13 -> 1;
                case 15 -> 2;
                default -> -1;
            };
            if (idx < 0 || idx >= state.pendingUpgrades.size()) {
                return;
            }
            UpgradeChoice selected = state.pendingUpgrades.get(idx);
            applyUpgrade(player, state, selected);
            state.pendingUpgrades = List.of();
            state.awaitingUpgradeSelection = false;
            state.skipNextUpgradeReopen = true;
            setUpgradePausedState(player, state, false);
            player.closeInventory();
        }

        private void handleUpgradeClose(Player player) {
            SurvivorState state = playerStates.get(player.getUniqueId());
            if (state == null) {
                return;
            }
            if (state.skipNextUpgradeReopen) {
                state.skipNextUpgradeReopen = false;
                return;
            }
            if (!state.awaitingUpgradeSelection || state.pendingUpgrades == null || state.pendingUpgrades.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(player.getUniqueId());
                if (online == null || !online.isOnline()) {
                    return;
                }
                SurvivorState onlineState = playerStates.get(online.getUniqueId());
                if (onlineState == null || !onlineState.awaitingUpgradeSelection || onlineState.pendingUpgrades.isEmpty()) {
                    return;
                }
                openUpgradeGui(online, onlineState);
            });
        }

        private void setUpgradePausedState(Player player, SurvivorState state, boolean paused) {
            if (player == null || state == null) {
                return;
            }
            state.upgradePaused = paused;
            if (paused) {
                pausedPlayers.add(player.getUniqueId());
                player.setInvisible(true);
                setEnemyFreezeState(true);
                return;
            }
            pausedPlayers.remove(player.getUniqueId());
            player.setInvisible(false);
            if (pausedPlayers.isEmpty()) {
                setEnemyFreezeState(false);
            }
        }

        private void setEnemyFreezeState(boolean frozen) {
            for (UUID id : spawned) {
                var entity = plugin.getServer().getEntity(id);
                if (!(entity instanceof LivingEntity living) || living.isDead()) {
                    continue;
                }
                if (living instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.setAI(!frozen);
                }
            }
        }

        private boolean isUpgradePaused(UUID playerId) {
            return playerId != null && pausedPlayers.contains(playerId);
        }

        private void applyUpgrade(Player player, SurvivorState state, UpgradeChoice choice) {
            if (choice.type == UpgradeType.STAT && choice.statType != null && choice.statAmount > 0) {
                state.tempStatBonuses.merge(choice.statType, choice.statAmount, Integer::sum);
                applyTempStatDelta(player.getUniqueId(), choice.statType, choice.statAmount);
                send(player, MessageType.SUCCESS, "Stronghold buff: " + ChatColor.GREEN + "+" + choice.statAmount
                        + " " + choice.statType.getDisplayName() + ChatColor.GRAY + " (temporary).");
                return;
            }
            if (choice.type == UpgradeType.GLOBAL_COOLDOWN) {
                state.cooldownUpgradeTier++;
                send(player, MessageType.SUCCESS, "Auto-cast cadence improved: "
                        + ChatColor.GREEN + "-10% global cooldown" + ChatColor.GRAY + " (Tier "
                        + ChatColor.WHITE + state.cooldownUpgradeTier + ChatColor.GRAY + ").");
                return;
            }
            if (choice.baseSpellId == null || choice.resultSpellId == null) {
                return;
            }
            String base = choice.baseSpellId.toLowerCase(Locale.ROOT);
            int nextRank = Math.max(1, state.ownedSpellRanks.getOrDefault(base, 0) + 1);
            state.ownedSpellRanks.put(base, nextRank);
            state.activeSpellByBase.put(base, choice.resultSpellId.toLowerCase(Locale.ROOT));
            send(player, MessageType.SUCCESS, "Auto-cast " + ChatColor.WHITE + choice.displayName + ChatColor.GRAY + " acquired.");
        }

        private List<UpgradeChoice> rollUpgradeChoices(SurvivorState state, int count) {
            refreshAutoCastPoolIfNeeded();
            List<UpgradeChoice> spellCandidates = new ArrayList<>();
            for (String baseId : autoCastBasePool) {
                UpgradeChoice spellChoice = spellUpgradeChoiceFor(state, baseId);
                if (spellChoice != null) {
                    spellCandidates.add(spellChoice);
                }
            }

            List<UpgradeChoice> rolled = new ArrayList<>();
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            while (!spellCandidates.isEmpty() && rolled.size() < count) {
                int pick = rng.nextInt(spellCandidates.size());
                rolled.add(spellCandidates.remove(pick));
            }

            if (rolled.size() < count) {
                List<UpgradeChoice> statCandidates = new ArrayList<>(List.of(
                        new UpgradeChoice(UpgradeType.STAT, "Power Surge", "Temporary Strength boost for this run only.", null, null, StatsManager.StatType.STR, 2),
                        new UpgradeChoice(UpgradeType.STAT, "Swiftfoot", "Temporary Agility boost for this run only.", null, null, StatsManager.StatType.AGI, 2),
                        new UpgradeChoice(UpgradeType.STAT, "Arcane Focus", "Temporary Intelligence boost for this run only.", null, null, StatsManager.StatType.INT, 2),
                        new UpgradeChoice(UpgradeType.STAT, "Vital Reserve", "Temporary Vitality boost for this run only.", null, null, StatsManager.StatType.VIT, 2),
                        new UpgradeChoice(UpgradeType.GLOBAL_COOLDOWN, "Arcane Tempo", "Reduce all auto-cast cooldowns globally by 10%.", null, null, null, 0)
                ));
                while (!statCandidates.isEmpty() && rolled.size() < count) {
                    int pick = rng.nextInt(statCandidates.size());
                    rolled.add(statCandidates.remove(pick));
                }
            }

            while (rolled.size() < count) {
                rolled.add(new UpgradeChoice(UpgradeType.STAT, "Vital Reserve", "Temporary Vitality boost for this run only.", null, null, StatsManager.StatType.VIT, 2));
            }
            return rolled;
        }

        private void refreshAutoCastPoolIfNeeded() {
            if (!autoCastBasePool.isEmpty()) {
                return;
            }
            initializeAutoCastPool();
        }

        private UpgradeChoice spellUpgradeChoiceFor(SurvivorState state, String baseSpellId) {
            SpellRegistry registry = SpellRegistry.getInstance();
            String base = baseSpellId.toLowerCase(Locale.ROOT);
            int rank = state.ownedSpellRanks.getOrDefault(base, 0);
            if (rank <= 0) {
                SpellRegistry.SpellEntry baseEntry = registry.getSpell(base);
                if (baseEntry == null) {
                    return null;
                }
                return new UpgradeChoice(UpgradeType.SPELL_UNLOCK, "Unlock " + baseEntry.definition().displayName(),
                        "Adds this spell to your auto-cast loadout.", base, baseEntry.definition().id(), null, 0);
            }
            SpellProgression progression = registry.getProgression(base);
            if (progression == null || progression.upgradeSpellIds() == null || progression.upgradeSpellIds().isEmpty()) {
                return null;
            }
            int nextIndex = rank - 1;
            if (nextIndex < 0 || nextIndex >= progression.upgradeSpellIds().size()) {
                return null;
            }
            String nextSpell = progression.upgradeSpellIds().get(nextIndex);
            SpellRegistry.SpellEntry upgraded = registry.getSpell(nextSpell);
            if (upgraded == null) {
                return null;
            }
            return new UpgradeChoice(UpgradeType.SPELL_UPGRADE, "Upgrade: " + upgraded.definition().displayName(),
                    "Improves an already owned auto-cast spell.", base, upgraded.definition().id(), null, 0);
        }

        private void tickAutoCast() {
            World world = plugin.getServer().getWorld(worldId);
            if (world == null) {
                return;
            }
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, SurvivorState> entry : playerStates.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline() || !Objects.equals(player.getWorld().getUID(), worldId)) {
                    continue;
                }
                if (pausedPlayers.contains(player.getUniqueId())) {
                    continue;
                }
                SurvivorState state = entry.getValue();
                for (String spellId : state.activeSpellByBase.values()) {
                    if (requiresLockTarget(spellId) && findNearestLockTarget(player, 18.0) == null) {
                        continue;
                    }
                    SpellRegistry.SpellEntry spellEntry = SpellRegistry.getInstance().getSpell(spellId);
                    if (spellEntry == null) {
                        continue;
                    }
                    SpellDefinition definition = spellEntry.definition();
                    long cooldown = computeAutoCastCooldownMs(definition, state);
                    long last = state.lastCastAtBySpell.getOrDefault(spellId, 0L);
                    if (now - last < cooldown) {
                        continue;
                    }
                    castAutoSpell(player, spellEntry);
                    state.lastCastAtBySpell.put(spellId, now);
                }
            }
        }

        private long computeAutoCastCooldownMs(SpellDefinition definition, SurvivorState state) {
            if (definition == null) {
                return BASE_AUTOCAST_COOLDOWN_MS;
            }
            String spellId = definition.id() == null ? "" : definition.id().toLowerCase(Locale.ROOT);
            long cooldown = Math.max(BASE_AUTOCAST_COOLDOWN_MS, SpellCastManager.getInstance().getCooldownMs(definition));
            if (spellId.startsWith("mage_fireball")) {
                cooldown = Math.max(cooldown, 2_200L);
            } else if (spellId.startsWith("archer_quickshot")) {
                cooldown = Math.max(cooldown, 1_850L);
            }
            int tier = state == null ? 0 : Math.max(0, state.cooldownUpgradeTier);
            double multiplier = Math.max(0.45, 1.0 - (tier * 0.10));
            return Math.max(600L, Math.round(cooldown * multiplier));
        }

        private boolean requiresLockTarget(String spellId) {
            if (spellId == null) {
                return false;
            }
            String normalized = spellId.toLowerCase(Locale.ROOT);
            return normalized.contains("homing")
                    || normalized.contains("seeker")
                    || normalized.startsWith("archer_quickshot");
        }

        private LivingEntity findNearestLockTarget(Player caster, double range) {
            return me.nakilex.levelplugin.spells.SpellEffectUtil.getLivingTargets(caster.getLocation(), Math.max(2.0, range),
                            living -> !living.equals(caster))
                    .stream()
                    .min(java.util.Comparator.comparingDouble(living -> living.getLocation().distanceSquared(caster.getLocation())))
                    .orElse(null);
        }

        private void castAutoSpell(Player player, SpellRegistry.SpellEntry spellEntry) {
            try {
                me.nakilex.levelplugin.spells.input.SpellInputEvent fakeInput =
                        new me.nakilex.levelplugin.spells.input.SpellInputEvent(
                                player,
                                me.nakilex.levelplugin.spells.input.SpellInputType.BASIC_ATTACK,
                                me.nakilex.levelplugin.spells.input.SpellInputMode.MOUSE_COMBO,
                                "AUTO");
                spellEntry.handler().cast(new SpellContext(plugin, player, spellEntry.definition(), fakeInput));
            } catch (Exception ignored) {
                // Guard auto-cast loop from individual spell runtime issues.
            }
        }

        private void applyTempStatDelta(UUID playerId, StatsManager.StatType statType, int delta) {
            if (playerId == null || statType == null || delta == 0) {
                return;
            }
            StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(playerId);
            switch (statType) {
                case STR -> stats.bonusStrength += delta;
                case AGI -> stats.bonusAgility += delta;
                case INT -> stats.bonusIntelligence += delta;
                case DEX -> stats.bonusDexterity += delta;
                case VIT -> stats.bonusVitality += delta;
                case WIL -> stats.bonusWill += delta;
                case TEC -> stats.bonusTechnique += delta;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                StatsManager.getInstance().recalcDerivedStats(player);
            }
        }

        private void restorePlayerAfterRun(UUID playerId, SurvivorState state) {
            if (state == null) {
                return;
            }
            state.awaitingUpgradeSelection = false;
            state.skipNextUpgradeReopen = true;
            state.upgradePaused = false;
            pausedPlayers.remove(playerId);
            if (pausedPlayers.isEmpty()) {
                setEnemyFreezeState(false);
            }
            for (Map.Entry<StatsManager.StatType, Integer> buff : state.tempStatBonuses.entrySet()) {
                applyTempStatDelta(playerId, buff.getKey(), -Math.max(0, buff.getValue()));
            }
            Player online = Bukkit.getPlayer(playerId);
            if (online != null && online.isOnline()) {
                online.setInvisible(false);
                PlayerClassManager.getInstance().setPlayerClass(online, state.originalClass);
                if (state.progressBar != null) {
                    state.progressBar.removePlayer(online);
                    state.progressBar.setVisible(false);
                }
                online.closeInventory();
            }
        }

        private Location findSpawnNear(Location playerLoc, Location fallbackOrigin, double minRadius, double maxRadius) {
            World world = playerLoc.getWorld();
            if (world == null) {
                return null;
            }
            double safeMinRadius = Math.max(MIN_ENEMY_SPAWN_RADIUS, minRadius);
            double safeMaxRadius = Math.max(safeMinRadius + 0.5, maxRadius);
            Location grassSpawn = findSpawnNearWithGroundRule(world, playerLoc, fallbackOrigin, safeMinRadius, safeMaxRadius, true);
            if (grassSpawn != null) {
                return grassSpawn;
            }
            return findSpawnNearWithGroundRule(world, playerLoc, fallbackOrigin, safeMinRadius, safeMaxRadius, false);
        }

        private Location findSpawnNearWithGroundRule(World world,
                                                     Location playerLoc,
                                                     Location fallbackOrigin,
                                                     double minRadius,
                                                     double maxRadius,
                                                     boolean grassOnly) {
            for (int attempt = 0; attempt < 20; attempt++) {
                double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                double dist = ThreadLocalRandom.current().nextDouble(minRadius, maxRadius);
                Vector offset = new Vector(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
                Location base = playerLoc.clone().add(offset);
                int y = world.getHighestBlockYAt(base);
                Material groundType = world.getBlockAt(base.getBlockX(), y, base.getBlockZ()).getType();
                if (!isAllowedSpawnGround(groundType, grassOnly)) {
                    continue;
                }
                Location spawn = new Location(world, base.getX(), Math.max(y + 1, fallbackOrigin.getY()), base.getZ());
                if (spawn.getBlock().getType().isAir() && spawn.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    return spawn;
                }
            }
            return null;
        }

        private boolean isAllowedSpawnGround(Material groundType, boolean grassOnly) {
            if (groundType == null || !groundType.isSolid()) {
                return false;
            }
            if (grassOnly) {
                return groundType == Material.GRASS_BLOCK;
            }
            return !groundType.name().contains("LEAVES");
        }
    }

    private static final class SurvivorState {
        private final PlayerClass originalClass;
        private final Map<String, Integer> ownedSpellRanks = new HashMap<>();
        private final Map<String, String> activeSpellByBase = new HashMap<>();
        private final Map<String, Long> lastCastAtBySpell = new HashMap<>();
        private final Map<StatsManager.StatType, Integer> tempStatBonuses = new EnumMap<>(StatsManager.StatType.class);
        private BossBar progressBar;
        private int level = 1;
        private int xp = 0;
        private List<UpgradeChoice> pendingUpgrades = List.of();
        private boolean awaitingUpgradeSelection;
        private boolean skipNextUpgradeReopen;
        private boolean upgradePaused;
        private int cooldownUpgradeTier;

        private SurvivorState(PlayerClass originalClass) {
            this.originalClass = originalClass == null ? PlayerClass.VILLAGER : originalClass;
        }
    }

    private enum UpgradeType {
        SPELL_UNLOCK,
        SPELL_UPGRADE,
        STAT,
        GLOBAL_COOLDOWN
    }

    private record UpgradeChoice(UpgradeType type,
                                 String displayName,
                                 String description,
                                 String baseSpellId,
                                 String resultSpellId,
                                 StatsManager.StatType statType,
                                 int statAmount) {
    }

    public record StageStatus(int wave, int enemiesRemaining) {
    }

    private static final class MobMotionState {
        private Location lastLocation;
        private long lastMovedAtMs;
        private long lastTeleportAtMs;

        private MobMotionState(Location lastLocation, long lastMovedAtMs, long lastTeleportAtMs) {
            this.lastLocation = lastLocation;
            this.lastMovedAtMs = lastMovedAtMs;
            this.lastTeleportAtMs = lastTeleportAtMs;
        }
    }
}
