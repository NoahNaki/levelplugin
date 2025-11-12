package me.nakilex.levelplugin.dungeon.trial;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.DungeonRunObserver;
import me.nakilex.levelplugin.dungeon.DungeonRunResult;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Manages arcane trial runs and prestige tracking. */
public final class ArcaneTrialManager implements DungeonRunObserver {

    private static final int PRESTIGE_COST = 500;

    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final BattlePassManager battlePassManager;
    private final PlayerConfig playerConfig;
    private final EnvironmentManager environmentManager;
    private final Map<Integer, ArcaneTrialDefinition> definitions = new LinkedHashMap<>();
    private final Map<UUID, ArcaneTrialState> stateCache = new HashMap<>();
    private final Map<UUID, ActiveTrial> activeTrials = new HashMap<>();
    private final org.bukkit.scheduler.BukkitTask tickTask;

    public ArcaneTrialManager(Main plugin,
                              DungeonManager dungeonManager,
                              BattlePassManager battlePassManager,
                              PlayerConfig playerConfig,
                              EnvironmentManager environmentManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.battlePassManager = battlePassManager;
        this.playerConfig = playerConfig;
        this.environmentManager = environmentManager;
        registerDefaults();
        dungeonManager.addRunObserver(this);
        Bukkit.getPluginManager().registerEvents(new ArcaneTrialListener(this), plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 30, 20L * 30);
    }

    private void registerDefaults() {
        definitions.clear();
        definitions.put(1, ArcaneTrialDefinition.builder("ember_chamber", 1)
                .displayName(ChatColor.GREEN + "Ember Chamber")
                .layoutKey("ember_chamber")
                .description("Face elemental sparks to stabilise your arcane conduit.")
                .markReward(60)
                .recommendedLevel(45)
                .battlePassProgress(40)
                .timeLimitMinutes(15)
                .build());
        definitions.put(2, ArcaneTrialDefinition.builder("celestial_arena", 2)
                .displayName(ChatColor.AQUA + "Celestial Arena")
                .layoutKey("celestial_arena")
                .description("Defeat mirrored copies of your combat style.")
                .markReward(90)
                .recommendedLevel(60)
                .battlePassProgress(55)
                .timeLimitMinutes(17)
                .build());
        definitions.put(3, ArcaneTrialDefinition.builder("void_labyrinth", 3)
                .displayName(ChatColor.LIGHT_PURPLE + "Void Labyrinth")
                .layoutKey("void_labyrinth")
                .description("Navigate collapsing pathways while purging void anomalies.")
                .markReward(130)
                .recommendedLevel(75)
                .battlePassProgress(70)
                .timeLimitMinutes(18)
                .build());
        definitions.put(4, ArcaneTrialDefinition.builder("eternal_spire", 4)
                .displayName(ChatColor.GOLD + "Eternal Spire")
                .layoutKey("eternal_spire")
                .description("Climb the spire and shatter the arcane apex guardian.")
                .markReward(180)
                .recommendedLevel(90)
                .battlePassProgress(90)
                .timeLimitMinutes(20)
                .build());
    }

    public void shutdown() {
        dungeonManager.removeRunObserver(this);
        if (tickTask != null) {
            tickTask.cancel();
        }
        activeTrials.clear();
        stateCache.clear();
    }

    public void openBoard(Player player) {
        ArcaneTrialState state = stateFor(player.getUniqueId());
        ArcaneTrialDefinition next = definitions.get(Math.min(state.getHighestTier() + 1, definitions.size()));
        player.openInventory(ArcaneTrialBoard.create(state, next, definitions));
    }

    public void startTrial(Player player, int tier) {
        if (activeTrials.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You already have an active arcane trial.");
            return;
        }
        ArcaneTrialDefinition def = definitions.get(tier);
        if (def == null) {
            player.sendMessage(ChatColor.RED + "Unknown trial tier.");
            return;
        }
        if (!hasShrine(player)) {
            player.sendMessage(ChatColor.RED + "Upgrade your sanctum shrine to access arcane trials.");
            return;
        }
        activeTrials.put(player.getUniqueId(), new ActiveTrial(def));
        ChatFormatter.sendBoxedCenteredMessages(player, "§5",
                ChatColor.LIGHT_PURPLE + "Arcane Trial: " + def.getDisplayName(),
                ChatColor.GRAY + def.getDescription());
        dungeonManager.startInstance(player, def.getLayoutKey());
    }

    public void prestige(Player player) {
        ArcaneTrialState state = stateFor(player.getUniqueId());
        if (state.getMarks() < PRESTIGE_COST) {
            player.sendMessage(ChatColor.RED + "You need " + PRESTIGE_COST + " marks to prestige.");
            return;
        }
        state.spendMarks(PRESTIGE_COST);
        state.prestigeUp();
        saveState(player.getUniqueId(), state);
        ChatFormatter.sendBoxedCenteredMessages(player, "§d",
                ChatColor.LIGHT_PURPLE + "Prestige gained!",
                ChatColor.GRAY + "Your arcane mark multiplier has increased.");
    }

    @Override
    public void onDungeonCompleted(DungeonRunResult result) {
        if (result.getParticipants().size() != 1) {
            return;
        }
        UUID playerId = result.getParticipants().iterator().next();
        ActiveTrial trial = activeTrials.remove(playerId);
        if (trial == null) {
            return;
        }
        ArcaneTrialDefinition def = trial.definition;
        Player player = Bukkit.getPlayer(playerId);
        ArcaneTrialState state = stateFor(playerId);
        state.addMarks(def.getMarkReward());
        state.setHighestTier(def.getTier());
        saveState(playerId, state);
        if (player != null) {
            battlePassManager.addProgress(player, def.getBattlePassProgress(), "Arcane Trials");
            ChatFormatter.sendBoxedCenteredMessages(player, "§d",
                    ChatColor.LIGHT_PURPLE + "Trial Complete",
                    ChatColor.GRAY + "Marks earned: " + ChatColor.AQUA + def.getMarkReward(),
                    ChatColor.GRAY + "Total marks: " + ChatColor.AQUA + state.getMarks());
        }
    }

    private boolean hasShrine(Player player) {
        return environmentManager.getBuildingStage(player.getUniqueId(), "shrine") >= 2;
    }

    private ArcaneTrialState stateFor(UUID uuid) {
        return stateCache.computeIfAbsent(uuid, id -> {
            ConfigurationSection section = playerConfig.getArcaneTrialSection(id);
            return ArcaneTrialState.load(section);
        });
    }

    private void saveState(UUID uuid, ArcaneTrialState state) {
        ConfigurationSection section = playerConfig.getArcaneTrialSection(uuid);
        state.save(section);
        playerConfig.saveConfigFile();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, ActiveTrial> entry : activeTrials.entrySet()) {
            ActiveTrial trial = entry.getValue();
            if (now - trial.startTime > trial.definition.getTimeLimitMinutes() * 60L * 1000L) {
                expired.add(entry.getKey());
            }
        }
        for (UUID id : expired) {
            activeTrials.remove(id);
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                ChatFormatter.sendBoxedCenteredMessages(player, "§c",
                        ChatColor.RED + "Arcane trial expired",
                        ChatColor.GRAY + "Time limit exceeded.");
            }
        }
    }

    public Map<Integer, ArcaneTrialDefinition> getDefinitions() {
        return definitions;
    }

    public ArcaneTrialState getState(Player player) {
        return stateFor(player.getUniqueId());
    }

    private static final class ActiveTrial {
        final ArcaneTrialDefinition definition;
        final long startTime = System.currentTimeMillis();

        ActiveTrial(ArcaneTrialDefinition definition) {
            this.definition = definition;
        }
    }
}

