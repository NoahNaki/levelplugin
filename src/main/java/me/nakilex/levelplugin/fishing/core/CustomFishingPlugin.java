package me.nakilex.levelplugin.fishing.core;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.FishingMechanism;
import me.nakilex.levelplugin.fishing.api.game.FishingGame;
import me.nakilex.levelplugin.fishing.core.action.CommandAction;
import me.nakilex.levelplugin.fishing.core.action.GiveItemAction;
import me.nakilex.levelplugin.fishing.core.action.MessageAction;
import me.nakilex.levelplugin.fishing.core.action.ParticleAction;
import me.nakilex.levelplugin.fishing.core.action.SoundAction;
import me.nakilex.levelplugin.fishing.core.action.XpAction;
import me.nakilex.levelplugin.fishing.core.command.CustomFishingCommand;
import me.nakilex.levelplugin.fishing.core.condition.BiomeCondition;
import me.nakilex.levelplugin.fishing.core.condition.LiquidDepthCondition;
import me.nakilex.levelplugin.fishing.core.condition.LiquidTypeCondition;
import me.nakilex.levelplugin.fishing.core.condition.PermissionCondition;
import me.nakilex.levelplugin.fishing.core.condition.TimeRangeCondition;
import me.nakilex.levelplugin.fishing.core.condition.WeatherCondition;
import me.nakilex.levelplugin.fishing.core.condition.WorldCondition;
import me.nakilex.levelplugin.fishing.core.condition.YRangeCondition;
import me.nakilex.levelplugin.fishing.core.config.ConfiguredAction;
import me.nakilex.levelplugin.fishing.core.config.ConfiguredCondition;
import me.nakilex.levelplugin.fishing.core.event.FishingListener;
import me.nakilex.levelplugin.fishing.core.feedback.FeedbackService;
import me.nakilex.levelplugin.fishing.core.feedback.FishingTheme;
import me.nakilex.levelplugin.fishing.core.game.GameDefinition;
import me.nakilex.levelplugin.fishing.core.game.TimingBarGame;
import me.nakilex.levelplugin.fishing.core.loot.LootEntry;
import me.nakilex.levelplugin.fishing.core.registry.ActionRegistry;
import me.nakilex.levelplugin.fishing.core.registry.ConditionRegistry;
import me.nakilex.levelplugin.fishing.core.registry.GameRegistry;
import me.nakilex.levelplugin.fishing.core.registry.MechanismRegistry;
import me.nakilex.levelplugin.utils.WeightUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class CustomFishingPlugin {
    private final Main plugin;
    private final MechanismRegistry mechanismRegistry = new MechanismRegistry();
    private final ConditionRegistry conditionRegistry = new ConditionRegistry();
    private final ActionRegistry actionRegistry = new ActionRegistry();
    private final GameRegistry gameRegistry = new GameRegistry();
    private final Map<UUID, FishingContext> contexts = new HashMap<>();
    private final Map<UUID, FishingGame> activeGames = new HashMap<>();
    private final FishingConfigManager configManager;
    private final FishingListener listener;
    private final FeedbackService feedbackService;

    public CustomFishingPlugin(Main plugin) {
        this.plugin = plugin;
        this.configManager = new FishingConfigManager(plugin, mechanismRegistry);
        this.listener = new FishingListener(this);
        this.feedbackService = new FeedbackService(configManager);
    }

    public void enable() {
        configManager.load();
        registerDefaults();
        registerRuntime();
    }

    public void disable() {
        activeGames.values().forEach(FishingGame::cancel);
        activeGames.clear();
        contexts.clear();
    }

    public void reload() {
        configManager.reload();
    }

    private void registerDefaults() {
        conditionRegistry.register("permission", new PermissionCondition());
        conditionRegistry.register("biome", new BiomeCondition());
        conditionRegistry.register("world", new WorldCondition());
        conditionRegistry.register("y_range", new YRangeCondition());
        conditionRegistry.register("weather", new WeatherCondition());
        conditionRegistry.register("time_range", new TimeRangeCondition());
        conditionRegistry.register("liquid_type", new LiquidTypeCondition());
        conditionRegistry.register("liquid_depth", new LiquidDepthCondition());

        actionRegistry.register("give_item", new GiveItemAction());
        actionRegistry.register("command", new CommandAction());
        actionRegistry.register("message", new MessageAction());
        actionRegistry.register("sound", new SoundAction());
        actionRegistry.register("particles", new ParticleAction());
        actionRegistry.register("xp", new XpAction());

        gameRegistry.register("TIMING_BAR", (definition, context, onComplete)
                -> new TimingBarGame(plugin, context, definition, feedbackService, getTheme(context.getMechanism()), onComplete));
    }

    private void registerRuntime() {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        Objects.requireNonNull(plugin.getCommand("customfishing"))
                .setExecutor(new CustomFishingCommand(this));
    }

    public void handleCast(PlayerFishEvent event) {
        Player player = event.getPlayer();
        Location hookLocation = event.getHook().getLocation();
        FishingMechanism mechanism = resolveMechanism(hookLocation);
        if (!configManager.getEnabledMechanisms().contains(mechanism)) {
            return;
        }
        ItemStack rodSnapshot = player.getInventory().getItemInMainHand();
        FishingContext context = new FishingContext(
                player.getUniqueId(),
                player.getWorld(),
                hookLocation.clone(),
                mechanism,
                rodSnapshot == null ? null : rodSnapshot.clone(),
                event.getHook().getEntityId(),
                new Random().nextLong(),
                System.currentTimeMillis(),
                hookLocation.getBlock().getBiome(),
                player.getWorld().hasStorm(),
                player.getWorld().isThundering(),
                player.getWorld().getTime(),
                calculateLiquidDepth(hookLocation, mechanism),
                hookLocation.getBlock().getType()
        );
        contexts.put(player.getUniqueId(), context);
        debug("Cast by " + player.getName() + " mechanism=" + mechanism);
    }

    public void handleBite(PlayerFishEvent event) {
        Player player = event.getPlayer();
        FishingContext context = contexts.get(player.getUniqueId());
        if (context == null) {
            return;
        }
        feedbackService.playBite(context);
        startGame(player, context);
    }

    public void handleCaught(PlayerFishEvent event) {
        event.setCancelled(true);
        if (event.getCaught() != null) {
            event.getCaught().remove();
        }
    }

    public void handlePlayerAction(Player player) {
        FishingGame game = activeGames.get(player.getUniqueId());
        if (game != null) {
            game.handlePlayerAction();
        }
    }

    public void handleQuit(UUID playerId) {
        cleanup(playerId);
    }

    public void handleAbort(UUID playerId) {
        cleanup(playerId);
    }

    public void simulate(Player player, FishingMechanism mechanism) {
        ItemStack rodSnapshot = player.getInventory().getItemInMainHand();
        FishingContext context = new FishingContext(
                player.getUniqueId(),
                player.getWorld(),
                player.getLocation().clone(),
                mechanism,
                rodSnapshot == null ? null : rodSnapshot.clone(),
                null,
                new Random().nextLong(),
                System.currentTimeMillis(),
                player.getLocation().getBlock().getBiome(),
                player.getWorld().hasStorm(),
                player.getWorld().isThundering(),
                player.getWorld().getTime(),
                calculateLiquidDepth(player.getLocation(), mechanism),
                player.getLocation().getBlock().getType()
        );
        contexts.put(player.getUniqueId(), context);
        startGame(player, context);
    }

    private void handleGameComplete(Player player, FishingContext context, boolean success) {
        activeGames.remove(player.getUniqueId());
        if (success) {
            LootEntry loot = resolveLoot(context);
            if (loot != null) {
                feedbackService.playSuccess(context);
                executeLootActions(context, loot);
            }
        } else {
            feedbackService.playFail(context);
        }
        cleanup(player.getUniqueId());
    }

    private void cleanup(UUID playerId) {
        FishingGame active = activeGames.remove(playerId);
        if (active != null) {
            active.cancel();
        }
        contexts.remove(playerId);
    }

    private void startGame(Player player, FishingContext context) {
        GameDefinition definition = selectGameDefinition(context);
        if (definition == null) {
            debug("No game definition found for " + player.getName());
            cleanup(player.getUniqueId());
            return;
        }
        if (activeGames.containsKey(player.getUniqueId())) {
            return;
        }
        FishingGame game = gameRegistry.create(definition, context, success -> handleGameComplete(player, context, success));
        if (game == null) {
            debug("Missing game type for " + definition.type());
            cleanup(player.getUniqueId());
            return;
        }
        feedbackService.playHooked(context);
        activeGames.put(player.getUniqueId(), game);
        game.start();
        debug("Started game " + definition.id() + " for " + player.getName());
    }

    public FeedbackService getFeedbackService() {
        return feedbackService;
    }

    public FishingTheme getTheme(FishingMechanism mechanism) {
        return configManager.getTheme(mechanism);
    }

    private GameDefinition selectGameDefinition(FishingContext context) {
        GameDefinition preferred = configManager.getGameDefinition(configManager.getDefaultGameId());
        if (preferred != null && conditionsPass(context, preferred.conditions())) {
            return preferred;
        }
        Optional<GameDefinition> match = configManager.getGameDefinitions().values().stream()
                .filter(def -> conditionsPass(context, def.conditions()))
                .findFirst();
        return match.orElse(null);
    }

    private LootEntry resolveLoot(FishingContext context) {
        List<LootEntry> pool = configManager.getLootPool(context.getMechanism());
        List<LootEntry> eligible = pool.stream()
                .filter(entry -> conditionsPass(context, entry.conditions()))
                .toList();
        if (eligible.isEmpty()) {
            debug("No eligible loot for " + context.getPlayerId());
            return null;
        }
        Random random = new Random(context.getSeed());
        return WeightUtil.pickWeighted(random, eligible, LootEntry::weight);
    }

    private boolean conditionsPass(FishingContext context, List<ConfiguredCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (ConfiguredCondition condition : conditions) {
            if (condition == null) {
                continue;
            }
            var tester = conditionRegistry.get(condition.type());
            if (tester == null || !tester.test(context, condition.args())) {
                return false;
            }
        }
        return true;
    }

    private void executeLootActions(FishingContext context, LootEntry loot) {
        if (loot.actions() == null || loot.actions().isEmpty()) {
            return;
        }
        for (ConfiguredAction action : loot.actions()) {
            if (action == null) {
                continue;
            }
            var executor = actionRegistry.get(action.type());
            if (executor != null) {
                executor.execute(context, action.args());
            }
        }
    }

    private FishingMechanism resolveMechanism(Location location) {
        Block block = location.getBlock();
        Material type = block.getType();
        if (type == Material.LAVA) {
            return FishingMechanism.LAVA;
        }
        if (type == Material.WATER || type == Material.BUBBLE_COLUMN) {
            return FishingMechanism.WATER;
        }
        if (isVoidLocation(location)) {
            return FishingMechanism.VOID;
        }
        return FishingMechanism.WATER;
    }

    private boolean isVoidLocation(Location location) {
        int minY = Math.max(location.getWorld().getMinHeight(), configManager.getVoidCheckMinY());
        for (int y = location.getBlockY() - 1; y >= minY; y--) {
            if (location.getWorld().getBlockAt(location.getBlockX(), y, location.getBlockZ()).getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    private int calculateLiquidDepth(Location location, FishingMechanism mechanism) {
        if (mechanism == FishingMechanism.VOID) {
            return 0;
        }
        Material target = location.getBlock().getType();
        if (target == Material.BUBBLE_COLUMN) {
            target = Material.WATER;
        }
        if (target != Material.WATER && target != Material.LAVA) {
            return 0;
        }
        int depth = 0;
        for (int y = location.getBlockY(); y >= location.getWorld().getMinHeight(); y--) {
            Material current = location.getWorld().getBlockAt(location.getBlockX(), y, location.getBlockZ()).getType();
            if (current != target && current != Material.BUBBLE_COLUMN) {
                break;
            }
            depth++;
        }
        return depth;
    }

    private void debug(String message) {
        if (configManager.isDebug()) {
            plugin.getLogger().info("[CustomFishing] " + message);
        }
    }

    public FishingConfigManager getConfigManager() {
        return configManager;
    }

    public boolean isActive(UUID playerId) {
        return activeGames.containsKey(playerId);
    }

}
