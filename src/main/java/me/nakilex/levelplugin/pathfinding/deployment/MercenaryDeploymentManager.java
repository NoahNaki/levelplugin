package me.nakilex.levelplugin.pathfinding.deployment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.pathfinding.MercenaryManager;
import me.nakilex.levelplugin.pathfinding.deployment.MercenaryDeploymentState.ActiveDeployment;
import me.nakilex.levelplugin.pathfinding.deployment.MercenaryDeploymentState.CompletedDeployment;
import me.nakilex.levelplugin.pathfinding.deployment.gui.MercenaryDeploymentBoard;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Coordinates mercenary deployments, including daily rotation, persistence, and
 * reward delivery.  The manager stitches together the mercenary system,
 * calendar, guild progression, quest rewards, and battle pass progress to
 * create a cohesive idle gameplay loop.
 */
public class MercenaryDeploymentManager {

    private final Main plugin;
    private final MercenaryManager mercenaryManager;
    private final QuestManager questManager;
    private final BattlePassManager battlePassManager;
    private final PlayerConfig playerConfig;
    private final GuildManager guildManager;
    private final Map<String, MercenaryDeploymentDefinition> definitions = new LinkedHashMap<>();
    private final Map<UUID, MercenaryDeploymentState> playerStates = new HashMap<>();
    private final List<String> dailyRotation = new ArrayList<>();
    private long rotationEpochDay = Long.MIN_VALUE;
    private BukkitTask tickTask;

    public MercenaryDeploymentManager(Main plugin, MercenaryManager mercenaryManager) {
        this.plugin = plugin;
        this.mercenaryManager = mercenaryManager;
        this.questManager = plugin.getQuestManager();
        this.battlePassManager = plugin.getBattlePassManager();
        this.playerConfig = plugin.getPlayerConfig();
        this.guildManager = plugin.getGuildManager();
        registerDefaults();
        refreshDailyRotation();
        Bukkit.getPluginManager().registerEvents(new MercenaryDeploymentListener(this), plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 200L, 200L);
    }

    /** Open the contract board for the player. */
    public void openBoard(Player player) {
        MercenaryDeploymentState state = state(player.getUniqueId());
        resolveCompleted(player, state, System.currentTimeMillis(), true);
        player.openInventory(MercenaryDeploymentBoard.create(plugin, this, player, state));
    }

    /** Called by the calendar each time a new day starts. */
    public void advanceDay() {
        refreshDailyRotation();
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            MercenaryDeploymentState state = playerStates.get(player.getUniqueId());
            if (state != null) {
                resolveCompleted(player, state, now, true);
            }
        }
    }

    /** Handle player login to load persisted deployment state. */
    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        MercenaryDeploymentState state = loadState(uuid);
        playerStates.put(uuid, state);
        resolveCompleted(player, state, System.currentTimeMillis(), false);
        if (!state.completedDeployments().isEmpty()) {
            ChatFormatter.sendBoxedCenteredMessages(player, "§6",
                    "§eMercenary contracts complete!",
                    ChatColor.GRAY + "Visit the board to claim " + state.completedDeployments().size() + " reward(s).");
        }
    }

    /** Persist a player's state when they leave the server. */
    public void handleQuit(UUID uuid) {
        MercenaryDeploymentState state = playerStates.remove(uuid);
        if (state != null) {
            saveState(uuid, state);
        }
    }

    /** Shut down timers and save remaining player data. */
    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (Map.Entry<UUID, MercenaryDeploymentState> entry : playerStates.entrySet()) {
            saveState(entry.getKey(), entry.getValue());
        }
        playerStates.clear();
    }

    /**
     * Begin a deployment for the given player if requirements are met.
     */
    public boolean startDeployment(Player player, String deploymentId, MercenarySpecialization specialization) {
        MercenaryDeploymentDefinition def = definitions.get(deploymentId);
        if (def == null) {
            return false;
        }
        MercenaryDeploymentState state = state(player.getUniqueId());
        resolveCompleted(player, state, System.currentTimeMillis(), false);
        if (state.getActive(deploymentId) != null) {
            player.sendMessage(ChatColor.RED + "You already have this contract underway.");
            return false;
        }
        int maxActive = maxActiveSlots(player);
        if (state.activeDeployments().size() >= maxActive) {
            player.sendMessage(ChatColor.RED + "You have no vacant contract slots.");
            return false;
        }
        if (!mercenaryManager.hasMercenary(player)) {
            player.sendMessage(ChatColor.RED + "Bind a mercenary before sending them on a contract.");
            return false;
        }
        long now = System.currentTimeMillis();
        boolean hasBoundSpecialist = mercenaryManager.hasProfile(player, specialization.profileClass());
        double chance = computeSuccessChance(def, specialization, player, hasBoundSpecialist);
        double rewardMultiplier = computeRewardMultiplier(def, specialization, player, hasBoundSpecialist);
        ActiveDeployment active = new ActiveDeployment(
                deploymentId,
                specialization,
                now,
                def.durationMillis(),
                chance,
                rewardMultiplier
        );
        state.addActive(active);
        saveState(player.getUniqueId(), state);
        player.sendMessage(ChatColor.GOLD + "Dispatched mercenary squad on " + def.displayName() + ChatColor.GOLD + ".");
        return true;
    }

    /** Cancel an in-progress deployment without reward. */
    public boolean cancelDeployment(Player player, String deploymentId) {
        MercenaryDeploymentState state = state(player.getUniqueId());
        ActiveDeployment removed = state.removeActive(deploymentId);
        if (removed == null) {
            return false;
        }
        saveState(player.getUniqueId(), state);
        player.sendMessage(ChatColor.YELLOW + "You recalled the squad before completion.");
        return true;
    }

    /** Claim rewards from a completed deployment. */
    public boolean claim(Player player, String deploymentId) {
        MercenaryDeploymentState state = state(player.getUniqueId());
        List<CompletedDeployment> completed = state.completedDeployments();
        CompletedDeployment match = null;
        for (CompletedDeployment cd : completed) {
            if (cd.deploymentId().equals(deploymentId)) {
                match = cd;
                break;
            }
        }
        if (match == null) {
            return false;
        }
        MercenaryDeploymentDefinition def = definitions.get(deploymentId);
        if (def == null) {
            completed.remove(match);
            saveState(player.getUniqueId(), state);
            player.sendMessage(ChatColor.RED + "This contract is no longer valid.");
            return false;
        }
        deliverReward(player, def, match);
        completed.remove(match);
        saveState(player.getUniqueId(), state);
        return true;
    }

    /** Definitions displayed on the board today. */
    public List<MercenaryDeploymentDefinition> getDailyDeployments() {
        List<MercenaryDeploymentDefinition> list = new ArrayList<>();
        for (String id : dailyRotation) {
            MercenaryDeploymentDefinition def = definitions.get(id);
            if (def != null) {
                list.add(def);
            }
        }
        return list;
    }

    public Optional<MercenaryDeploymentDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            MercenaryDeploymentState state = playerStates.get(player.getUniqueId());
            if (state != null) {
                resolveCompleted(player, state, now, true);
            }
        }
    }

    private void resolveCompleted(Player player, MercenaryDeploymentState state, long now, boolean notify) {
        List<CompletedDeployment> newlyCompleted = new ArrayList<>();
        for (ActiveDeployment deployment : new ArrayList<>(state.activeDeployments())) {
            if (now >= deployment.endsAt()) {
                boolean success = rollSuccess(player.getUniqueId(), deployment);
                CompletedDeployment completed = new CompletedDeployment(
                        deployment.deploymentId(),
                        deployment.specialization(),
                        success,
                        deployment.rewardMultiplier(),
                        deployment.successChance(),
                        now
                );
                newlyCompleted.add(completed);
                state.addCompleted(completed);
                state.removeActive(deployment.deploymentId());
            }
        }
        if (!newlyCompleted.isEmpty()) {
            saveState(player.getUniqueId(), state);
            if (notify) {
                ChatFormatter.sendBoxedCenteredMessages(player, "§6",
                        "§eMercenary contract finished!",
                        ChatColor.GRAY + "Visit the board to resolve " + newlyCompleted.size() + " report(s).");
            }
        }
    }

    private boolean rollSuccess(UUID playerId, ActiveDeployment deployment) {
        long seed = deployment.startedAt() ^ playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits();
        seed ^= deployment.deploymentId().hashCode() * 31L;
        Random random = new Random(seed);
        double chance = Math.max(0.05, Math.min(0.95, deployment.successChance()));
        return random.nextDouble() <= chance;
    }

    private double computeSuccessChance(MercenaryDeploymentDefinition def,
                                        MercenarySpecialization spec,
                                        Player player,
                                        boolean hasBoundSpecialist) {
        double chance = def.baseSuccessChance();
        if (spec == def.recommended()) {
            chance += 0.15;
        }
        if (hasBoundSpecialist) {
            chance += 0.05;
        }
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild != null) {
            chance += Math.min(0.15, guild.getLevel() * 0.02);
        }
        return Math.max(0.1, Math.min(0.95, chance));
    }

    private double computeRewardMultiplier(MercenaryDeploymentDefinition def,
                                           MercenarySpecialization spec,
                                           Player player,
                                           boolean hasBoundSpecialist) {
        double multiplier = 1.0;
        if (spec == def.recommended()) {
            multiplier += 0.1;
        }
        if (hasBoundSpecialist) {
            multiplier += 0.05;
        }
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild != null) {
            multiplier += Math.min(0.15, guild.getLevel() * 0.01);
        }
        return Math.min(multiplier, 1.5);
    }

    private void deliverReward(Player player, MercenaryDeploymentDefinition def, CompletedDeployment completed) {
        QuestReward base = completed.success() ? def.successReward() : def.failureReward();
        QuestReward reward = scaleReward(base, completed.rewardMultiplier());
        if (reward != null) {
            questManager.applyReward(player, reward);
        }
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild != null) {
            int coins = (int) Math.round((completed.success() ? def.guildCoinReward() : def.guildCoinReward() * 0.25)
                    * completed.rewardMultiplier());
            int exp = (int) Math.round((completed.success() ? def.guildExpReward() : def.guildExpReward() * 0.25)
                    * completed.rewardMultiplier());
            guild.addCoins(coins);
            guild.addExp(exp);
        }
        if (battlePassManager != null) {
            int progress = completed.success() ? def.battlePassProgress() : Math.max(1, def.battlePassProgress() / 3);
            if (progress > 0) {
                battlePassManager.addProgress(player, progress, "Mercenary Contracts");
            }
        }
        String status = completed.success() ? "§aSUCCESS" : "§cFAILED";
        ChatFormatter.sendBoxedCenteredMessages(player, "§6",
                ChatColor.GOLD + def.displayName(),
                ChatColor.GRAY + "Status: " + status,
                ChatColor.GRAY + "Specialist: " + completed.specialization().displayName(),
                ChatColor.GRAY + "Success Chance: " + String.format("%.0f%%", completed.successChance() * 100));
    }

    private QuestReward scaleReward(QuestReward base, double multiplier) {
        if (base == null) {
            return null;
        }
        int xp = (int) Math.round(base.getXp() * multiplier);
        int coins = (int) Math.round(base.getCoins() * multiplier);
        int gems = (int) Math.round(base.getGems() * multiplier);
        return new QuestReward(xp, coins, gems, new ArrayList<>(base.getItemIds()),
                new ArrayList<>(base.getUnlockClasses()));
    }

    private MercenaryDeploymentState state(UUID uuid) {
        return playerStates.computeIfAbsent(uuid, id -> loadState(uuid));
    }

    private MercenaryDeploymentState loadState(UUID uuid) {
        MercenaryDeploymentState state = new MercenaryDeploymentState();
        FileConfiguration config = playerConfig.getConfig();
        String basePath = "players." + uuid + ".mercenary.deployments";
        List<Map<?, ?>> activeList = config.getMapList(basePath + ".active");
        for (Map<?, ?> map : activeList) {
            String id = Objects.toString(map.get("id"), null);
            String specId = Objects.toString(map.get("spec"), null);
            Number started = (Number) map.get("started");
            Number duration = (Number) map.get("duration");
            Number chance = (Number) map.get("chance");
            Number multiplier = (Number) map.get("mult");
            MercenarySpecialization spec = MercenarySpecialization.fromId(specId);
            if (id == null || spec == null || started == null || duration == null || chance == null || multiplier == null) {
                continue;
            }
            state.addActive(new ActiveDeployment(
                    id,
                    spec,
                    started.longValue(),
                    duration.longValue(),
                    chance.doubleValue(),
                    multiplier.doubleValue()
            ));
        }
        List<Map<?, ?>> completedList = config.getMapList(basePath + ".completed");
        for (Map<?, ?> map : completedList) {
            String id = Objects.toString(map.get("id"), null);
            String specId = Objects.toString(map.get("spec"), null);
            Number success = map.get("success") instanceof Number ? (Number) map.get("success") : null;
            Boolean successBool = success == null ? (map.get("success") instanceof Boolean ? (Boolean) map.get("success") : null)
                    : success.intValue() != 0;
            Number mult = (Number) map.get("mult");
            Number chance = (Number) map.get("chance");
            Number completedAt = (Number) map.get("time");
            MercenarySpecialization spec = MercenarySpecialization.fromId(specId);
            if (id == null || spec == null || mult == null || chance == null || completedAt == null || successBool == null) {
                continue;
            }
            state.addCompleted(new CompletedDeployment(id, spec, successBool,
                    mult.doubleValue(), chance.doubleValue(), completedAt.longValue()));
        }
        return state;
    }

    private void saveState(UUID uuid, MercenaryDeploymentState state) {
        FileConfiguration config = playerConfig.getConfig();
        String basePath = "players." + uuid + ".mercenary.deployments";
        if (state == null || state.isEmpty()) {
            config.set(basePath, null);
            playerConfig.saveConfigFile();
            return;
        }
        List<Map<String, Object>> active = new ArrayList<>();
        for (ActiveDeployment deployment : state.activeDeployments()) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", deployment.deploymentId());
            data.put("spec", deployment.specialization().id());
            data.put("started", deployment.startedAt());
            data.put("duration", deployment.durationMillis());
            data.put("chance", deployment.successChance());
            data.put("mult", deployment.rewardMultiplier());
            active.add(data);
        }
        List<Map<String, Object>> completed = new ArrayList<>();
        for (CompletedDeployment deployment : state.completedDeployments()) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", deployment.deploymentId());
            data.put("spec", deployment.specialization().id());
            data.put("success", deployment.success());
            data.put("mult", deployment.rewardMultiplier());
            data.put("chance", deployment.successChance());
            data.put("time", deployment.completedAt());
            completed.add(data);
        }
        config.set(basePath + ".active", active);
        config.set(basePath + ".completed", completed);
        playerConfig.saveConfigFile();
    }

    private int maxActiveSlots(Player player) {
        int slots = 1;
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild != null) {
            if (guild.getLevel() >= 3) {
                slots++;
            }
            if (guild.getLevel() >= 6) {
                slots++;
            }
        }
        return slots;
    }

    private void refreshDailyRotation() {
        long epochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
        if (epochDay == rotationEpochDay) {
            return;
        }
        rotationEpochDay = epochDay;
        dailyRotation.clear();
        List<String> ids = new ArrayList<>(definitions.keySet());
        Collections.shuffle(ids, new Random(epochDay * 3418731287123L));
        int pick = Math.min(4, ids.size());
        for (int i = 0; i < pick; i++) {
            dailyRotation.add(ids.get(i));
        }
    }

    private void registerDefaults() {
        definitions.clear();
        definitions.put("shadow_patrol", new MercenaryDeploymentDefinition.Builder("shadow_patrol")
                .name(ChatColor.GOLD + "Shadow Patrol")
                .recommended(MercenarySpecialization.ROGUE)
                .durationMinutes(180)
                .difficulty(2)
                .baseSuccess(0.65)
                .addDescription("Sweep through twilight alleys and clear rogue enclaves.")
                .addDescription("Best undertaken by nimble blades.")
                .reward(new QuestReward(350, 420, 0, Collections.emptyList()))
                .failureReward(new QuestReward(100, 120, 0, Collections.emptyList()))
                .battlePassProgress(20)
                .guildCoinReward(75)
                .guildExpReward(110)
                .build());
        definitions.put("arcane_excavation", new MercenaryDeploymentDefinition.Builder("arcane_excavation")
                .name(ChatColor.AQUA + "Arcane Excavation")
                .recommended(MercenarySpecialization.MAGE)
                .durationMinutes(240)
                .difficulty(3)
                .baseSuccess(0.6)
                .addDescription("Recover lost relics from unstable ley clusters.")
                .addDescription("Requires disciplined spellcraft to survive the anomalies.")
                .reward(new QuestReward(450, 550, 0, Collections.emptyList()))
                .failureReward(new QuestReward(150, 160, 0, Collections.emptyList()))
                .battlePassProgress(28)
                .guildCoinReward(95)
                .guildExpReward(150)
                .build());
        definitions.put("beast_cull", new MercenaryDeploymentDefinition.Builder("beast_cull")
                .name(ChatColor.RED + "Savage Beast Cull")
                .recommended(MercenarySpecialization.WARRIOR)
                .durationMinutes(150)
                .difficulty(2)
                .baseSuccess(0.7)
                .addDescription("Drive back feral packs threatening remote caravans.")
                .reward(new QuestReward(320, 380, 0, Collections.emptyList()))
                .failureReward(new QuestReward(120, 110, 0, Collections.emptyList()))
                .battlePassProgress(18)
                .guildCoinReward(70)
                .guildExpReward(105)
                .build());
        definitions.put("skyline_watch", new MercenaryDeploymentDefinition.Builder("skyline_watch")
                .name(ChatColor.GREEN + "Skyline Watch")
                .recommended(MercenarySpecialization.ARCHER)
                .durationMinutes(120)
                .difficulty(1)
                .baseSuccess(0.75)
                .addDescription("Guard the airship lanes and intercept marauding drakes.")
                .reward(new QuestReward(260, 320, 0, Collections.emptyList()))
                .failureReward(new QuestReward(90, 100, 0, Collections.emptyList()))
                .battlePassProgress(15)
                .guildCoinReward(60)
                .guildExpReward(90)
                .build());
        definitions.put("rift_delvers", new MercenaryDeploymentDefinition.Builder("rift_delvers")
                .name(ChatColor.LIGHT_PURPLE + "Rift Delvers")
                .recommended(MercenarySpecialization.MAGE)
                .durationMinutes(300)
                .difficulty(4)
                .baseSuccess(0.55)
                .addDescription("Seal micro-rifts spawning in the lower dungeon corridors.")
                .reward(new QuestReward(520, 640, 0, Collections.emptyList()))
                .failureReward(new QuestReward(180, 200, 0, Collections.emptyList()))
                .battlePassProgress(35)
                .guildCoinReward(120)
                .guildExpReward(190)
                .build());
        definitions.put("iron_vanguard", new MercenaryDeploymentDefinition.Builder("iron_vanguard")
                .name(ChatColor.DARK_RED + "Iron Vanguard Escort")
                .recommended(MercenarySpecialization.WARRIOR)
                .durationMinutes(360)
                .difficulty(4)
                .baseSuccess(0.5)
                .addDescription("Escort a siege caravan through contested territory.")
                .reward(new QuestReward(600, 780, 0, Collections.emptyList()))
                .failureReward(new QuestReward(200, 230, 0, Collections.emptyList()))
                .battlePassProgress(40)
                .guildCoinReward(140)
                .guildExpReward(210)
                .build());
    }
}
