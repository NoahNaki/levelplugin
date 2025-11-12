package me.nakilex.levelplugin.guild.expedition;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.DungeonRunObserver;
import me.nakilex.levelplugin.dungeon.DungeonRunResult;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.GuildPermission;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Coordinates the expedition relic gameplay loop. */
public final class ExpeditionRelicManager implements DungeonRunObserver {

    private final Main plugin;
    private final GuildManager guildManager;
    private final GuildSiegeManager siegeManager;
    private final EnvironmentManager environmentManager;
    private final DungeonManager dungeonManager;
    private final BattlePassManager battlePassManager;
    private final GuildQuestManager guildQuestManager;
    private final PartyManager partyManager;
    private final Map<String, ExpeditionRelicDefinition> definitions = new LinkedHashMap<>();
    private final List<ExpeditionRelicDefinition> orderedDefinitions = new ArrayList<>();
    private final Map<String, ExpeditionRelicState> stateCache = new HashMap<>();
    private final Map<UUID, ActiveExpedition> activeExpeditions = new HashMap<>();
    private final org.bukkit.scheduler.BukkitTask tickTask;

    public ExpeditionRelicManager(Main plugin,
                                  GuildManager guildManager,
                                  GuildSiegeManager siegeManager,
                                  EnvironmentManager environmentManager,
                                  DungeonManager dungeonManager,
                                  BattlePassManager battlePassManager,
                                  GuildQuestManager guildQuestManager,
                                  PartyManager partyManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.siegeManager = siegeManager;
        this.environmentManager = environmentManager;
        this.dungeonManager = dungeonManager;
        this.battlePassManager = battlePassManager;
        this.guildQuestManager = guildQuestManager;
        this.partyManager = partyManager;
        registerDefaults();
        dungeonManager.addRunObserver(this);
        Bukkit.getPluginManager().registerEvents(new ExpeditionRelicListener(this), plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 30, 20L * 30);
    }

    private void registerDefaults() {
        definitions.clear();
        orderedDefinitions.clear();

        addDefinition(ExpeditionRelicDefinition.builder("sunken_reliquary")
                .displayName(ChatColor.AQUA + "Sunken Reliquary")
                .description("Plunge into drowned catacombs to recover the reliquary core.")
                .layoutKey("ember_chamber")
                .progressRequired(120)
                .investmentCost(350)
                .progressPerInvestment(15)
                .guildCoinReward(650)
                .guildExpReward(420)
                .battlePassReward(70)
                .effectDescription("Siege defenders gain +5% damage reduction.")
                .durationDays(5)
                .maintenanceMaterial(Material.HEART_OF_THE_SEA)
                .maintenanceBundle(1)
                .maintenanceExtensionDays(2)
                .requiredBuilding("war_room", 2)
                .timeLimitMinutes(18)
                .build());

        addDefinition(ExpeditionRelicDefinition.builder("astral_compass")
                .displayName(ChatColor.LIGHT_PURPLE + "Astral Compass")
                .description("Stabilise celestial motes before they collapse the arena.")
                .layoutKey("celestial_arena")
                .progressRequired(150)
                .investmentCost(400)
                .progressPerInvestment(18)
                .guildCoinReward(720)
                .guildExpReward(500)
                .battlePassReward(85)
                .effectDescription("Guild members earn +10% life-skill XP while active.")
                .durationDays(6)
                .maintenanceMaterial(Material.AMETHYST_CLUSTER)
                .maintenanceBundle(6)
                .maintenanceExtensionDays(2)
                .requiredBuilding("archives", 3)
                .timeLimitMinutes(19)
                .build());

        addDefinition(ExpeditionRelicDefinition.builder("void_lens")
                .displayName(ChatColor.DARK_PURPLE + "Void Lens")
                .description("Breach the void labyrinth and seal the hunger at its core.")
                .layoutKey("void_labyrinth")
                .progressRequired(180)
                .investmentCost(450)
                .progressPerInvestment(20)
                .guildCoinReward(820)
                .guildExpReward(580)
                .battlePassReward(95)
                .effectDescription("Mercenary deployments gain +12% success chance.")
                .durationDays(7)
                .maintenanceMaterial(Material.ENDER_EYE)
                .maintenanceBundle(4)
                .maintenanceExtensionDays(3)
                .requiredBuilding("observatory", 2)
                .timeLimitMinutes(20)
                .build());
    }

    private void addDefinition(ExpeditionRelicDefinition definition) {
        definitions.put(definition.getId(), definition);
        orderedDefinitions.add(definition);
    }

    public void shutdown() {
        dungeonManager.removeRunObserver(this);
        if (tickTask != null) {
            tickTask.cancel();
        }
        activeExpeditions.clear();
        stateCache.clear();
    }

    public void openBoard(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Join a guild to access expeditions.");
            return;
        }
        ExpeditionRelicState state = stateFor(guild);
        ExpeditionRelicDefinition target = definitionFor(state.getTargetRelicId());
        if (target == null) {
            player.sendMessage(ChatColor.RED + "No expedition relics are configured.");
            return;
        }
        ExpeditionRelicDefinition active = definitionFor(state.getActiveRelicId());
        player.openInventory(me.nakilex.levelplugin.guild.expedition.gui.ExpeditionRelicBoard.create(target, active, state));
    }

    public void invest(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Only guild members can invest in expeditions.");
            return;
        }
        if (!guildManager.hasPermission(player.getUniqueId(), GuildPermission.VAULT_ACCESS)) {
            player.sendMessage(ChatColor.RED + "You need vault access to spend guild coins.");
            return;
        }
        if (!ownsEnvironment(guild)) {
            player.sendMessage(ChatColor.RED + "Your guild must control the siege environment to invest.");
            return;
        }
        ExpeditionRelicState state = stateFor(guild);
        ExpeditionRelicDefinition target = definitionFor(state.getTargetRelicId());
        if (target == null) {
            player.sendMessage(ChatColor.RED + "No expedition target available.");
            return;
        }
        if (!hasRequiredBuilding(guild, target.getRequiredBuilding(), target.getRequiredStage())) {
            player.sendMessage(ChatColor.RED + "Upgrade your " + target.getRequiredBuilding() + " to stage "
                    + target.getRequiredStage() + " to invest in this relic.");
            return;
        }
        if (state.getProgress() >= target.getProgressRequired()) {
            player.sendMessage(ChatColor.YELLOW + "Progress bar is full. Launch the expedition.");
            return;
        }
        if (!guild.removeCoins(target.getInvestmentCost())) {
            player.sendMessage(ChatColor.RED + "Guild treasury lacks " + target.getInvestmentCost() + " coins.");
            return;
        }
        state.addProgress(target.getProgressPerInvestment());
        state.getContributions().merge(player.getUniqueId().toString(), target.getInvestmentCost(), Integer::sum);
        if (state.getProgress() >= target.getProgressRequired()) {
            state.setPendingLaunch(true);
            ChatFormatter.sendBoxedCenteredMessages(player, "§5",
                    ChatColor.LIGHT_PURPLE + "Expedition Prepared",
                    ChatColor.GRAY + "Gather your party and launch the relic run!");
        } else {
            player.sendMessage(ChatColor.GREEN + "Invested " + target.getInvestmentCost() + " coins. Progress: "
                    + state.getProgress() + "/" + target.getProgressRequired());
        }
        saveState(guild, state);
        guildManager.save();
        guildQuestManager.progressObjective(player, QuestObjectiveType.COLLECT, "expedition_invest", 1);
    }

    public void startExpedition(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Join a guild to launch expeditions.");
            return;
        }
        ExpeditionRelicState state = stateFor(guild);
        ExpeditionRelicDefinition target = definitionFor(state.getTargetRelicId());
        if (target == null) {
            player.sendMessage(ChatColor.RED + "No expedition target configured.");
            return;
        }
        if (state.getProgress() < target.getProgressRequired()) {
            player.sendMessage(ChatColor.RED + "Fill the expedition meter before launching.");
            return;
        }
        if (!ownsEnvironment(guild)) {
            player.sendMessage(ChatColor.RED + "Your guild must control the siege environment.");
            return;
        }
        Set<UUID> participants = collectParticipants(player);
        for (UUID id : participants) {
            if (activeExpeditions.containsKey(id)) {
                player.sendMessage(ChatColor.RED + "Someone in your party is already on an expedition.");
                return;
            }
        }
        ActiveExpedition expedition = new ActiveExpedition(guild.getName(), target, participants);
        for (UUID id : participants) {
            activeExpeditions.put(id, expedition);
        }
        state.setPendingLaunch(false);
        saveState(guild, state);
        ChatFormatter.sendBoxedCenteredMessages(player, "§5",
                ChatColor.LIGHT_PURPLE + "Relic Expedition Launched",
                ChatColor.GRAY + target.getDescription());
        dungeonManager.startInstance(player, target.getLayoutKey());
    }

    public void depositMaintenance(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Join a guild to deliver expedition upkeep.");
            return;
        }
        ExpeditionRelicState state = stateFor(guild);
        ExpeditionRelicDefinition active = definitionFor(state.getActiveRelicId());
        ExpeditionRelicDefinition target = definitionFor(state.getTargetRelicId());
        ExpeditionRelicDefinition definition = active != null ? active : target;
        if (definition == null) {
            player.sendMessage(ChatColor.RED + "No relic to maintain yet.");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold the upkeep item in your main hand.");
            return;
        }
        if (held.getType() != definition.getMaintenanceMaterial()) {
            player.sendMessage(ChatColor.RED + "This relic requires " + definition.getMaintenanceMaterial().name().toLowerCase()
                    + " for upkeep.");
            return;
        }
        int bundles = held.getAmount() / definition.getMaintenanceBundle();
        if (bundles <= 0) {
            player.sendMessage(ChatColor.RED + "You need at least " + definition.getMaintenanceBundle()
                    + " items per bundle.");
            return;
        }
        int itemsToConsume = bundles * definition.getMaintenanceBundle();
        held.setAmount(held.getAmount() - itemsToConsume);
        int extensionDays = bundles * definition.getMaintenanceExtensionDays();
        if (active != null && state.getActiveRelicExpiryEpochDay() > 0) {
            state.setActiveRelicExpiryEpochDay(state.getActiveRelicExpiryEpochDay() + extensionDays);
        } else {
            state.addMaintenanceBufferDays(extensionDays);
        }
        state.getMaintenanceContributors().merge(player.getUniqueId().toString(), itemsToConsume, Integer::sum);
        saveState(guild, state);
        player.sendMessage(ChatColor.GREEN + "Delivered upkeep. Added " + extensionDays + " day(s) to the relic reserve.");
    }

    @Override
    public void onDungeonCompleted(DungeonRunResult result) {
        if (result.getParticipants().isEmpty()) {
            return;
        }
        UUID sample = result.getParticipants().iterator().next();
        ActiveExpedition expedition = activeExpeditions.remove(sample);
        if (expedition == null) {
            return;
        }
        for (UUID id : expedition.participants) {
            if (!id.equals(sample)) {
                activeExpeditions.remove(id, expedition);
            }
        }
        if (!result.getLayoutKey().equalsIgnoreCase(expedition.definition.getLayoutKey())) {
            return;
        }
        Guild guild = guildManager.getGuild(expedition.guildName);
        if (guild == null) {
            return;
        }
        ExpeditionRelicState state = stateFor(guild);
        if (!expedition.definition.getId().equals(state.getTargetRelicId())) {
            return;
        }
        long epochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
        state.setProgress(0);
        state.getContributions().clear();
        state.setActiveRelicId(expedition.definition.getId());
        long baseDuration = epochDay + expedition.definition.getDurationDays();
        state.setActiveRelicExpiryEpochDay(baseDuration + state.getMaintenanceBufferDays());
        state.setMaintenanceBufferDays(0);
        state.getUnlockedRelics().add(expedition.definition.getId());
        state.setTargetRelicId(nextDefinition(expedition.definition.getId()).getId());
        saveState(guild, state);

        guild.addCoins(expedition.definition.getGuildCoinReward());
        guild.addExp(expedition.definition.getGuildExpReward());
        guildManager.save();

        for (UUID id : expedition.participants) {
            Player participant = Bukkit.getPlayer(id);
            if (participant != null) {
                battlePassManager.addProgress(participant, expedition.definition.getBattlePassReward(), "Expedition Relics");
                guildQuestManager.progressObjective(participant, QuestObjectiveType.SIEGE_PARTICIPATE, "expedition_relic", 1);
                ChatFormatter.sendBoxedCenteredMessages(participant, "§5",
                        ChatColor.LIGHT_PURPLE + "Relic Secured",
                        ChatColor.GRAY + expedition.definition.getDisplayName() + ChatColor.GRAY + " recovered!",
                        ChatColor.YELLOW + "+" + expedition.definition.getGuildCoinReward() + " Guild Coins  §8|  "
                                + ChatColor.AQUA + "+" + expedition.definition.getGuildExpReward() + " Guild XP");
            }
        }
    }

    public void rotateDaily(long epochDay) {
        for (Guild guild : guildManager.getGuilds()) {
            ExpeditionRelicState state = stateFor(guild);
            if (state.getRotationEpoch() == epochDay) {
                continue;
            }
            state.setRotationEpoch(epochDay);
            boolean changed = true;
            if (state.getActiveRelicId() != null && state.getActiveRelicExpiryEpochDay() > 0
                    && state.getActiveRelicExpiryEpochDay() <= epochDay) {
                ExpeditionRelicDefinition expired = definitionFor(state.getActiveRelicId());
                state.setActiveRelicId(null);
                state.setActiveRelicExpiryEpochDay(0);
                changed = true;
                notifyGuild(guild, ChatColor.RED + "The power of "
                        + (expired == null ? "a relic" : expired.getDisplayName())
                        + ChatColor.RED + " has faded.");
            }
            if (changed) {
                saveState(guild, state);
            }
        }
    }

    private void notifyGuild(Guild guild, String message) {
        for (UUID id : guild.getMembers()) {
            Player online = Bukkit.getPlayer(id);
            if (online != null) {
                ChatFormatter.sendCenteredMessage(online, message);
            }
        }
    }

    private boolean ownsEnvironment(Guild guild) {
        String owner = siegeManager.getOwnerGuild();
        return owner != null && owner.equalsIgnoreCase(guild.getName());
    }

    private boolean hasRequiredBuilding(Guild guild, String building, int stage) {
        int highest = 0;
        for (UUID member : guild.getMembers()) {
            highest = Math.max(highest, environmentManager.getBuildingStage(member, building));
            if (highest >= stage) {
                return true;
            }
        }
        return highest >= stage;
    }

    private ExpeditionRelicState stateFor(Guild guild) {
        return stateCache.computeIfAbsent(guild.getName(), name -> {
            ConfigurationSection root = guild.getProgressionData();
            ConfigurationSection section = root.getConfigurationSection("expedition_relics");
            if (section == null) {
                section = root.createSection("expedition_relics");
            }
            ExpeditionRelicState loaded = ExpeditionRelicState.load(section);
            if (loaded.getTargetRelicId() == null) {
                ExpeditionRelicDefinition first = orderedDefinitions.isEmpty() ? null : orderedDefinitions.get(0);
                if (first != null) {
                    loaded.setTargetRelicId(first.getId());
                }
            }
            return loaded;
        });
    }

    private void saveState(Guild guild, ExpeditionRelicState state) {
        ConfigurationSection root = guild.getProgressionData();
        ConfigurationSection section = root.getConfigurationSection("expedition_relics");
        if (section == null) {
            section = root.createSection("expedition_relics");
        }
        state.save(section);
        guildManager.save();
    }

    private ExpeditionRelicDefinition definitionFor(String id) {
        if (id == null) {
            return orderedDefinitions.isEmpty() ? null : orderedDefinitions.get(0);
        }
        return definitions.get(id);
    }

    private ExpeditionRelicDefinition nextDefinition(String currentId) {
        if (orderedDefinitions.isEmpty()) {
            throw new IllegalStateException("No expedition relics configured");
        }
        for (int i = 0; i < orderedDefinitions.size(); i++) {
            ExpeditionRelicDefinition def = orderedDefinitions.get(i);
            if (def.getId().equalsIgnoreCase(currentId)) {
                return orderedDefinitions.get((i + 1) % orderedDefinitions.size());
            }
        }
        return orderedDefinitions.get(0);
    }

    private Set<UUID> collectParticipants(Player leader) {
        Party party = partyManager.getParty(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            return Set.of(leader.getUniqueId());
        }
        List<UUID> members = party.getMembers();
        List<UUID> eligible = new ArrayList<>();
        for (UUID id : members) {
            Player online = Bukkit.getPlayer(id);
            if (online != null && online.isOnline()) {
                eligible.add(id);
            }
        }
        if (eligible.isEmpty()) {
            return Set.of(leader.getUniqueId());
        }
        return Set.copyOf(eligible);
    }

    private void tick() {
        if (activeExpeditions.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<ActiveExpedition> expired = new ArrayList<>();
        for (ActiveExpedition expedition : Set.copyOf(activeExpeditions.values())) {
            if (now - expedition.startTime > expedition.definition.getTimeLimitMinutes() * 60L * 1000L) {
                expired.add(expedition);
            }
        }
        for (ActiveExpedition expedition : expired) {
            for (UUID id : expedition.participants) {
                activeExpeditions.remove(id, expedition);
            }
            Guild guild = guildManager.getGuild(expedition.guildName);
            if (guild == null) {
                continue;
            }
            ExpeditionRelicState state = stateFor(guild);
            int penalty = Math.max(10, expedition.definition.getProgressPerInvestment());
            state.addProgress(-penalty);
            saveState(guild, state);
            for (UUID id : expedition.participants) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    ChatFormatter.sendBoxedCenteredMessages(player, "§c",
                            ChatColor.RED + "Expedition Failed",
                            ChatColor.GRAY + "The relic slipped away before completion.");
                }
            }
        }
    }

    private static final class ActiveExpedition {
        final String guildName;
        final ExpeditionRelicDefinition definition;
        final Set<UUID> participants;
        final long startTime = System.currentTimeMillis();

        ActiveExpedition(String guildName, ExpeditionRelicDefinition definition, Set<UUID> participants) {
            this.guildName = guildName;
            this.definition = definition;
            this.participants = Collections.unmodifiableSet(participants);
        }
    }
}
