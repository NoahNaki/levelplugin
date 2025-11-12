package me.nakilex.levelplugin.dungeon.rift;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.DungeonRunObserver;
import me.nakilex.levelplugin.dungeon.DungeonRunResult;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Coordinates the stage-based frontier rift ladder for guilds. */
public final class FrontierRiftManager implements DungeonRunObserver {

    private final Main plugin;
    private final GuildManager guildManager;
    private final GuildSiegeManager siegeManager;
    private final DungeonManager dungeonManager;
    private final BattlePassManager battlePassManager;
    private final GuildQuestManager guildQuestManager;
    private final PartyManager partyManager;
    private final Map<Integer, FrontierRiftDefinition> definitions = new HashMap<>();
    private final List<FrontierRiftDefinition> orderedDefinitions = new ArrayList<>();
    private final Map<String, FrontierRiftMutator> mutators = new HashMap<>();
    private final Map<String, FrontierRiftState> stateCache = new HashMap<>();
    private final Map<UUID, ActiveRun> activeRuns = new HashMap<>();
    private final Random random = new Random();
    private final org.bukkit.scheduler.BukkitTask tickTask;

    public FrontierRiftManager(Main plugin,
                               GuildManager guildManager,
                               GuildSiegeManager siegeManager,
                               DungeonManager dungeonManager,
                               BattlePassManager battlePassManager,
                               GuildQuestManager guildQuestManager,
                               PartyManager partyManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.siegeManager = siegeManager;
        this.dungeonManager = dungeonManager;
        this.battlePassManager = battlePassManager;
        this.guildQuestManager = guildQuestManager;
        this.partyManager = partyManager;
        registerDefaults();
        dungeonManager.addRunObserver(this);
        Bukkit.getPluginManager().registerEvents(new FrontierRiftListener(this), plugin);
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 30, 20L * 30);
    }

    private void registerDefaults() {
        orderedDefinitions.clear();
        definitions.clear();
        register(FrontierRiftDefinition.builder("scout_patrol", 1)
                .displayName(ChatColor.GREEN + "Frontier Patrol")
                .layoutKey("frontier_patrol")
                .description("Sweep the reclaimed outskirts for raiders and mark safe spawn points.")
                .baseGuildCoins(250)
                .baseGuildExp(180)
                .battlePassProgress(40)
                .recommendedPower("Item Score 120+")
                .timeLimitMinutes(18)
                .build());
        register(FrontierRiftDefinition.builder("scarlet_depths", 2)
                .displayName(ChatColor.YELLOW + "Scarlet Depths")
                .layoutKey("scarlet_depths")
                .description("Seal crystallised breaches beneath the town and purge the corrupted mobs.")
                .baseGuildCoins(325)
                .baseGuildExp(230)
                .battlePassProgress(55)
                .recommendedPower("Item Score 150+")
                .timeLimitMinutes(20)
                .build());
        register(FrontierRiftDefinition.builder("ashen_sky", 3)
                .displayName(ChatColor.GOLD + "Ashen Sky Bastion")
                .layoutKey("ashen_sky")
                .description("Hold the shattered bridge long enough for siege crews to stabilise the skyway.")
                .baseGuildCoins(420)
                .baseGuildExp(285)
                .battlePassProgress(70)
                .recommendedPower("Item Score 185+")
                .timeLimitMinutes(22)
                .build());
        register(FrontierRiftDefinition.builder("obsidian_gate", 4)
                .displayName(ChatColor.RED + "Obsidian Gate")
                .layoutKey("obsidian_gate")
                .description("Break the gate wards and defeat the elite legion before reinforcements arrive.")
                .baseGuildCoins(520)
                .baseGuildExp(340)
                .battlePassProgress(85)
                .recommendedPower("Item Score 215+")
                .timeLimitMinutes(24)
                .build());
        register(FrontierRiftDefinition.builder("luminous_core", 5)
                .displayName(ChatColor.DARK_PURPLE + "Luminous Core")
                .layoutKey("luminous_core")
                .description("Dive into the heart of the rift and collapse the planar core before it destabilises.")
                .baseGuildCoins(650)
                .baseGuildExp(420)
                .battlePassProgress(110)
                .recommendedPower("Item Score 250+")
                .timeLimitMinutes(26)
                .build());

        mutators.clear();
        registerMutator(new FrontierRiftMutator("steady",
                ChatColor.GRAY + "Steady Skies",
                ChatColor.DARK_GRAY + "Baseline conditions with no additional modifiers.",
                1.0,
                0.0));
        registerMutator(new FrontierRiftMutator("tempest",
                ChatColor.AQUA + "Tempest Winds",
                ChatColor.GRAY + "Rift storms empower casters but improve loot yields by 25%.",
                1.25,
                0.05));
        registerMutator(new FrontierRiftMutator("siege_vanguard",
                ChatColor.GOLD + "Siege Vanguard",
                ChatColor.GRAY + "Siege preparations grant bonus reinforcement NPCs but enemies hit harder.",
                1.35,
                0.10));
        registerMutator(new FrontierRiftMutator("shadowfall",
                ChatColor.DARK_PURPLE + "Shadowfall",
                ChatColor.GRAY + "Visibility drops drastically; stealth elites spawn but rewards scale 45%.",
                1.45,
                0.15));
    }

    private void register(FrontierRiftDefinition def) {
        orderedDefinitions.add(def);
        definitions.put(def.getStage(), def);
    }

    private void registerMutator(FrontierRiftMutator mutator) {
        mutators.put(mutator.getId(), mutator);
    }

    public void shutdown() {
        dungeonManager.removeRunObserver(this);
        if (tickTask != null) {
            tickTask.cancel();
        }
        activeRuns.clear();
    }

    /** Rotate daily mutators and decay idle ladders. */
    public void rotateDaily(long epochDay) {
        for (Guild guild : guildManager.getGuilds()) {
            FrontierRiftState state = stateFor(guild);
            boolean dirty = false;
            if (state.getMutatorEpoch() != epochDay) {
                FrontierRiftMutator mutator = pickMutator(state.getMutatorId());
                state.setMutator(mutator.getId(), epochDay);
                dirty = true;
            }
            long idleMillis = System.currentTimeMillis() - state.getLastCompletion();
            if (idleMillis > 3L * 24L * 60L * 60L * 1000L && state.getCurrentStage() > 1) {
                state.setCurrentStage(state.getCurrentStage() - 1);
                dirty = true;
            }
            if (dirty) {
                saveState(guild, state);
            }
        }
    }

    /** Open the ladder board UI. */
    public void openBoard(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "You must be in a guild to inspect frontier rifts.");
            return;
        }
        FrontierRiftState state = stateFor(guild);
        player.openInventory(buildBoard(player, guild, state));
    }

    /** Attempt to start the guild's next frontier stage. */
    public void startNextStage(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "You must be in a guild to lead a frontier expedition.");
            return;
        }
        String owner = siegeManager.getOwnerGuild();
        if (owner == null || !owner.equalsIgnoreCase(guild.getName())) {
            player.sendMessage(ChatColor.RED + "Only the guild currently holding the siege environment may launch frontier runs.");
            return;
        }
        if (isParticipantBusy(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You already have an active frontier expedition in progress.");
            return;
        }
        FrontierRiftState state = stateFor(guild);
        FrontierRiftDefinition def = definitionForStage(state.getCurrentStage());
        FrontierRiftMutator mutator = currentMutator(state.getMutatorId());
        Set<UUID> participants = collectParticipants(player);
        if (participants.stream().anyMatch(this::isParticipantBusy)) {
            player.sendMessage(ChatColor.RED + "One or more party members are already on an expedition.");
            return;
        }
        ActiveRun run = new ActiveRun(guild.getName(), def, mutator, participants);
        for (UUID id : participants) {
            activeRuns.put(id, run);
        }
        ChatFormatter.sendBoxedCenteredMessages(player, "§b",
                ChatColor.AQUA + "Launching Frontier Stage " + def.getStage(),
                ChatColor.GRAY + "Mutator: " + mutator.getDisplayName() + ChatColor.GRAY + " — " + mutator.getDescription());
        dungeonManager.startInstance(player, def.getLayoutKey());
    }

    @Override
    public void onDungeonCompleted(DungeonRunResult result) {
        if (result.getParticipants().isEmpty()) {
            return;
        }
        UUID sample = result.getParticipants().iterator().next();
        ActiveRun run = activeRuns.remove(sample);
        if (run == null) {
            return;
        }
        for (UUID id : run.participants) {
            if (!id.equals(sample)) {
                activeRuns.remove(id, run);
            }
        }
        if (!result.getLayoutKey().equalsIgnoreCase(run.definition.getLayoutKey())) {
            return;
        }
        Guild guild = guildManager.getGuild(run.guildName);
        if (guild == null) {
            return;
        }
        FrontierRiftState state = stateFor(guild);
        state.advanceStage();
        state.setBestStage(Math.max(state.getBestStage(), run.definition.getStage()));
        saveState(guild, state);

        double multiplier = Math.max(1.0, run.definition.getStage() * 0.15 + run.mutator.getRewardMultiplier());
        int coins = (int) Math.round(run.definition.getBaseGuildCoins() * multiplier);
        int exp = (int) Math.round(run.definition.getBaseGuildExp() * multiplier);
        guild.addCoins(coins);
        guild.addExp(exp);
        guildManager.save();

        for (UUID id : run.participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                battlePassManager.addProgress(p, run.definition.getBattlePassProgress(), "Frontier Rift");
                guildQuestManager.progressObjective(p, QuestObjectiveType.SIEGE_PARTICIPATE, "frontier_rift", 1);
                ChatFormatter.sendBoxedCenteredMessages(p, "§a",
                        ChatColor.GREEN + "Frontier Stage Cleared",
                        ChatColor.GRAY + "Stage " + run.definition.getStage() + ChatColor.GRAY + " complete!",
                        ChatColor.YELLOW + "+" + coins + " Guild Coins  §8|  " + ChatColor.AQUA + "+" + exp + " Guild XP");
            }
        }
    }

    private void tick() {
        if (activeRuns.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<ActiveRun> expired = new ArrayList<>();
        for (ActiveRun run : Set.copyOf(activeRuns.values())) {
            if (now - run.startTime > run.definition.getTimeLimitMinutes() * 60L * 1000L) {
                expired.add(run);
            }
        }
        for (ActiveRun run : expired) {
            for (UUID id : run.participants) {
                activeRuns.remove(id, run);
            }
            Guild guild = guildManager.getGuild(run.guildName);
            if (guild == null) {
                continue;
            }
            FrontierRiftState state = stateFor(guild);
            state.recordFailure();
            saveState(guild, state);
            for (UUID id : run.participants) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    ChatFormatter.sendBoxedCenteredMessages(p, "§c",
                            ChatColor.RED + "Frontier Expedition Failed",
                            ChatColor.GRAY + "Stage " + run.definition.getStage() + " timed out.");
                }
            }
        }
    }

    private boolean isParticipantBusy(UUID id) {
        return activeRuns.containsKey(id);
    }

    private Set<UUID> collectParticipants(Player player) {
        Party party = partyManager.getParty(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            return Set.of(player.getUniqueId());
        }
        List<UUID> members = party.getMembers();
        List<UUID> eligible = new ArrayList<>();
        for (UUID id : members) {
            Player member = Bukkit.getPlayer(id);
            if (member != null && member.isOnline()) {
                eligible.add(id);
            }
        }
        if (eligible.isEmpty()) {
            return Set.of(player.getUniqueId());
        }
        return Set.copyOf(eligible);
    }

    private FrontierRiftDefinition definitionForStage(int stage) {
        FrontierRiftDefinition def = definitions.get(stage);
        if (def != null) {
            return def;
        }
        return orderedDefinitions.get(orderedDefinitions.size() - 1);
    }

    private FrontierRiftMutator pickMutator(String previousId) {
        if (mutators.isEmpty()) {
            return new FrontierRiftMutator("steady", ChatColor.GRAY + "Steady", "", 1.0, 0.0);
        }
        List<FrontierRiftMutator> list = new ArrayList<>(mutators.values());
        FrontierRiftMutator selected = list.get(random.nextInt(list.size()));
        if (selected.getId().equals(previousId) && list.size() > 1) {
            selected = list.get((random.nextInt(list.size() - 1) + 1) % list.size());
        }
        return selected;
    }

    private FrontierRiftMutator currentMutator(String id) {
        FrontierRiftMutator mutator = mutators.get(id);
        if (mutator != null) {
            return mutator;
        }
        return mutators.values().stream().findFirst().orElse(new FrontierRiftMutator("steady", ChatColor.GRAY + "Steady", "", 1.0, 0.0));
    }

    private FrontierRiftState stateFor(Guild guild) {
        return stateCache.computeIfAbsent(guild.getName(), name -> {
            ConfigurationSection root = guild.getProgressionData();
            ConfigurationSection section = root.getConfigurationSection("frontier_rift");
            if (section == null) {
                section = root.createSection("frontier_rift");
            }
            return FrontierRiftState.load(section);
        });
    }

    private void saveState(Guild guild, FrontierRiftState state) {
        ConfigurationSection root = guild.getProgressionData();
        ConfigurationSection section = root.getConfigurationSection("frontier_rift");
        if (section == null) {
            section = root.createSection("frontier_rift");
        }
        state.save(section);
        guildManager.save();
    }

    private static final class ActiveRun {
        final String guildName;
        final FrontierRiftDefinition definition;
        final FrontierRiftMutator mutator;
        final Set<UUID> participants;
        final long startTime = System.currentTimeMillis();

        ActiveRun(String guildName, FrontierRiftDefinition definition, FrontierRiftMutator mutator, Set<UUID> participants) {
            this.guildName = guildName;
            this.definition = definition;
            this.mutator = mutator;
            this.participants = Collections.unmodifiableSet(participants);
        }
    }
}

