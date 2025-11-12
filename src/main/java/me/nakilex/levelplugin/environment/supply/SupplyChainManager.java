package me.nakilex.levelplugin.environment.supply;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Coordinates life-skill supply chains for guild environments. */
public final class SupplyChainManager {

    private final Main plugin;
    private final EnvironmentManager environmentManager;
    private final GuildManager guildManager;
    private final BattlePassManager battlePassManager;
    private final GuildQuestManager guildQuestManager;
    private final Map<String, SupplyChainDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, SupplyChainState> stateCache = new HashMap<>();
    private final org.bukkit.scheduler.BukkitTask tickTask;

    public SupplyChainManager(Main plugin,
                              EnvironmentManager environmentManager,
                              GuildManager guildManager,
                              BattlePassManager battlePassManager,
                              GuildQuestManager guildQuestManager) {
        this.plugin = plugin;
        this.environmentManager = environmentManager;
        this.guildManager = guildManager;
        this.battlePassManager = battlePassManager;
        this.guildQuestManager = guildQuestManager;
        registerDefaults();
        Bukkit.getPluginManager().registerEvents(new SupplyChainListener(this), plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 30, 20L * 30);
    }

    private void registerDefaults() {
        definitions.clear();
        definitions.put("timber_run", SupplyChainDefinition.builder("timber_run")
                .displayName(ChatColor.GREEN + "Timber Run")
                .description("Ship lumber from the surrounding groves to reinforce the town walls.")
                .requiredBuilding("lumberyard", 2)
                .stage(SupplyChainStage.builder(0)
                        .name("Gather Fresh Logs")
                        .require(Material.OAK_LOG, 64)
                        .require(Material.SPRUCE_LOG, 64)
                        .guildCoins(220)
                        .guildExp(180)
                        .battlePassProgress(35)
                        .rewardDescription("Unlocks timber cart cosmetic for the environment")
                        .productionSeconds(600)
                        .build())
                .stage(SupplyChainStage.builder(1)
                        .name("Refine and Treat")
                        .require(Material.COAL, 48)
                        .require(Material.CAMPFIRE, 8)
                        .guildCoins(280)
                        .guildExp(210)
                        .battlePassProgress(45)
                        .rewardDescription("Grants 5% environment upgrade discount for 24h")
                        .productionSeconds(720)
                        .build())
                .build());

        definitions.put("alloy_forge", SupplyChainDefinition.builder("alloy_forge")
                .displayName(ChatColor.YELLOW + "Alloy Forge")
                .description("Smelt rare alloys to upgrade siege weaponry.")
                .requiredBuilding("forge", 3)
                .stage(SupplyChainStage.builder(0)
                        .name("Ore Stockpiling")
                        .require(Material.IRON_INGOT, 96)
                        .require(Material.COPPER_INGOT, 96)
                        .guildCoins(320)
                        .guildExp(260)
                        .battlePassProgress(55)
                        .rewardDescription("Unlocks upgraded ballista ammo during sieges")
                        .productionSeconds(900)
                        .build())
                .stage(SupplyChainStage.builder(1)
                        .name("Quench the Alloy")
                        .require(Material.BLAZE_POWDER, 24)
                        .require(Material.LAVA_BUCKET, 4)
                        .guildCoins(380)
                        .guildExp(320)
                        .battlePassProgress(65)
                        .rewardDescription("Adds +3% siege damage bonus for the owning guild")
                        .productionSeconds(960)
                        .build())
                .build());

        definitions.put("siege_supply", SupplyChainDefinition.builder("siege_supply")
                .displayName(ChatColor.GOLD + "Siege Supply Lines")
                .description("Establish supply lines to keep mercenaries fed during long sieges.")
                .requiredBuilding("barracks", 3)
                .stage(SupplyChainStage.builder(0)
                        .name("Provisioning")
                        .require(Material.COOKED_BEEF, 128)
                        .require(Material.BREAD, 128)
                        .guildCoins(420)
                        .guildExp(360)
                        .battlePassProgress(70)
                        .rewardDescription("Mercenary contracts resolve 10% faster for one day")
                        .productionSeconds(1020)
                        .build())
                .stage(SupplyChainStage.builder(1)
                        .name("Logistics Coordination")
                        .require(Material.MAP, 16)
                        .require(Material.FLETCHING_TABLE, 6)
                        .guildCoins(500)
                        .guildExp(420)
                        .battlePassProgress(85)
                        .rewardDescription("Adds +1 mercenary deployment slot")
                        .productionSeconds(1080)
                        .build())
                .build());
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        stateCache.clear();
    }

    public void rotateDaily(long epochDay) {
        for (Guild guild : guildManager.getGuilds()) {
            SupplyChainState state = stateFor(guild);
            if (state.getRotationEpoch() != epochDay) {
                selectChain(guild, state);
                state.setRotationEpoch(epochDay);
                saveState(guild, state);
            }
        }
    }

    public void openBoard(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Join a guild to manage supply chains.");
            return;
        }
        SupplyChainState state = stateFor(guild);
        SupplyChainDefinition def = definitionForState(guild, state);
        if (def == null) {
            player.sendMessage(ChatColor.RED + "Your guild has no unlocked supply chains yet.");
            return;
        }
        SupplyChainStage stage = currentStage(def, state);
        player.openInventory(SupplyChainBoard.create(def, stage, state));
    }

    public void handleDeposit(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Only guild members can contribute supplies.");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold an item to contribute.");
            return;
        }
        SupplyChainState state = stateFor(guild);
        SupplyChainDefinition def = definitionForState(guild, state);
        if (def == null) {
            player.sendMessage(ChatColor.RED + "No active supply chain available.");
            return;
        }
        SupplyChainStage stage = currentStage(def, state);
        Map<Material, Integer> requirements = stage.getRequirements();
        Material material = held.getType();
        if (!requirements.containsKey(material)) {
            player.sendMessage(ChatColor.RED + "This item is not needed for the current recipe.");
            return;
        }
        int required = requirements.get(material);
        int delivered = state.getContributions().getOrDefault(material.name(), 0);
        if (delivered >= required) {
            player.sendMessage(ChatColor.YELLOW + "Your guild has already met the " + material + " quota.");
            return;
        }
        int amount = Math.min(held.getAmount(), required - delivered);
        state.getContributions().merge(material.name(), amount, Integer::sum);
        state.getContributorAmounts().merge(player.getUniqueId().toString(), amount, Integer::sum);
        int remaining = held.getAmount() - amount;
        if (remaining <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            held.setAmount(remaining);
            player.getInventory().setItemInMainHand(held);
        }
        guildQuestManager.progressObjective(player, QuestObjectiveType.COLLECT, material.name().toLowerCase(), amount);
        player.sendMessage(ChatColor.GREEN + "Contributed " + amount + " " + material.name().toLowerCase() + ".");
        if (isStageSatisfied(stage, state)) {
            if (state.getProductionCompleteAt() <= System.currentTimeMillis()) {
                long finish = System.currentTimeMillis() + stage.getProductionSeconds() * 1000L;
                state.setProductionCompleteAt(finish);
                broadcast(guild, ChatColor.AQUA + "Supply stage " + stage.getName() + " is now processing (" + stage.getProductionSeconds() / 60 + "m).");
            }
        }
        saveState(guild, state);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (Guild guild : guildManager.getGuilds()) {
            SupplyChainState state = stateFor(guild);
            if (state.getProductionCompleteAt() > 0 && now >= state.getProductionCompleteAt()) {
                SupplyChainDefinition def = definitionForState(guild, state);
                if (def != null) {
                    completeStage(guild, state, def);
                }
            }
        }
    }

    private void completeStage(Guild guild, SupplyChainState state, SupplyChainDefinition def) {
        SupplyChainStage stage = currentStage(def, state);
        state.setProductionCompleteAt(0L);
        state.getContributions().clear();
        for (Map.Entry<String, Integer> entry : state.getContributorAmounts().entrySet()) {
            try {
                UUID id = UUID.fromString(entry.getKey());
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    battlePassManager.addProgress(player, stage.getBattlePassProgress(), "Supply Chain");
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        state.getContributorAmounts().clear();
        guild.addCoins(stage.getGuildCoins());
        guild.addExp(stage.getGuildExp());
        ChatFormatter.broadcastGuildMessage(guild,
                ChatColor.GREEN + "Supply stage complete: " + stage.getName(),
                ChatColor.YELLOW + "+" + stage.getGuildCoins() + " coins "
                        + ChatColor.AQUA + "+" + stage.getGuildExp() + " guild XP",
                ChatColor.GRAY + stage.getRewardDescription());
        state.setStageIndex(state.getStageIndex() + 1);
        if (state.getStageIndex() >= def.getStages().size()) {
            state.getCompletedChains().add(def.getId());
            state.setStageIndex(0);
            selectChain(guild, state);
        }
        saveState(guild, state);
        guildManager.save();
    }

    private boolean isStageSatisfied(SupplyChainStage stage, SupplyChainState state) {
        for (Map.Entry<Material, Integer> e : stage.getRequirements().entrySet()) {
            int delivered = state.getContributions().getOrDefault(e.getKey().name(), 0);
            if (delivered < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    private SupplyChainDefinition definitionForState(Guild guild, SupplyChainState state) {
        String active = state.getActiveChainId();
        if (active != null) {
            SupplyChainDefinition def = definitions.get(active);
            if (def != null && isUnlocked(guild, def)) {
                return def;
            }
        }
        return selectChain(guild, state);
    }

    private SupplyChainDefinition selectChain(Guild guild, SupplyChainState state) {
        for (SupplyChainDefinition def : definitions.values()) {
            if (state.getCompletedChains().contains(def.getId())) continue;
            if (isUnlocked(guild, def)) {
                state.setActiveChainId(def.getId());
                state.setStageIndex(0);
                state.getContributions().clear();
                state.getContributorAmounts().clear();
                state.setProductionCompleteAt(0L);
                return def;
            }
        }
        state.setActiveChainId(null);
        return null;
    }

    private boolean isUnlocked(Guild guild, SupplyChainDefinition def) {
        int highest = 0;
        for (UUID member : guild.getMembers()) {
            highest = Math.max(highest, environmentManager.getBuildingStage(member, def.getRequiredBuilding()));
            if (highest >= def.getRequiredStage()) {
                return true;
            }
        }
        return highest >= def.getRequiredStage();
    }

    private SupplyChainStage currentStage(SupplyChainDefinition def, SupplyChainState state) {
        int index = Math.min(state.getStageIndex(), def.getStages().size() - 1);
        return def.getStages().get(index);
    }

    private SupplyChainState stateFor(Guild guild) {
        return stateCache.computeIfAbsent(guild.getName(), name -> {
            ConfigurationSection root = guild.getProgressionData();
            ConfigurationSection section = root.getConfigurationSection("supply_chain");
            if (section == null) {
                section = root.createSection("supply_chain");
            }
            return SupplyChainState.load(section);
        });
    }

    private void saveState(Guild guild, SupplyChainState state) {
        ConfigurationSection root = guild.getProgressionData();
        ConfigurationSection section = root.getConfigurationSection("supply_chain");
        if (section == null) {
            section = root.createSection("supply_chain");
        }
        state.save(section);
        guildManager.save();
    }

    private void broadcast(Guild guild, String... lines) {
        for (UUID id : guild.getMembers()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                ChatFormatter.sendBoxedCenteredMessages(player, "§b", lines);
            }
        }
    }

    public Map<String, SupplyChainDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public SupplyChainState getState(Guild guild) {
        return stateFor(guild);
    }
}

