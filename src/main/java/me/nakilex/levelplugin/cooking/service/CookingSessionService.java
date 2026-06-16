package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.registry.CookingRecipeRegistry;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.stage.CookingStageExecutor;
import me.nakilex.levelplugin.cooking.stage.CookingStageExecutorRegistry;
import me.nakilex.levelplugin.cooking.stage.InsertItemStageExecutor;
import me.nakilex.levelplugin.cooking.stage.MiniGameStageExecutor;
import me.nakilex.levelplugin.cooking.stage.WaitStageExecutor;
import me.nakilex.levelplugin.cooking.util.CookingLocationKey;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Orchestrates active cooking sessions while stage executors own stage-specific runtime behavior. */
public class CookingSessionService implements CookingStageExecutor.StageSessionController {
    private final Main plugin;
    private final CookingRecipeRegistry recipeRegistry;
    private final ActiveCookingSessionRegistry sessionRegistry;
    private final PlacedCookingWorkstationRegistry placedWorkstations;
    private final CookingRewardService rewardService;
    private final CookingIngredientRefundService refundService;
    private final CookingDisplayService displayService;
    private final CookingEffectsService effectsService;
    private final CookingStageExecutorRegistry executorRegistry;

    public CookingSessionService(Main plugin,
                                 CookingRecipeRegistry recipeRegistry,
                                 ActiveCookingSessionRegistry sessionRegistry,
                                 PlacedCookingWorkstationRegistry placedWorkstations) {
        this(plugin, recipeRegistry, sessionRegistry, placedWorkstations, new CookingRewardService());
    }

    public CookingSessionService(Main plugin,
                                 CookingRecipeRegistry recipeRegistry,
                                 ActiveCookingSessionRegistry sessionRegistry,
                                 PlacedCookingWorkstationRegistry placedWorkstations,
                                 CookingRewardService rewardService) {
        this.plugin = plugin;
        this.recipeRegistry = recipeRegistry;
        this.sessionRegistry = sessionRegistry;
        this.placedWorkstations = placedWorkstations;
        this.rewardService = rewardService;
        this.refundService = new CookingIngredientRefundService();
        this.displayService = new CookingDisplayService(plugin);
        this.effectsService = new CookingEffectsService();
        this.executorRegistry = new CookingStageExecutorRegistry()
                .register(new InsertItemStageExecutor())
                .register(new MiniGameStageExecutor())
                .register(new WaitStageExecutor());
    }

    public ActiveCookingSessionRegistry.CreateResult startSession(Player player, PlacedCookingWorkstation workstation, CookingRecipe recipe) {
        ActiveCookingSessionRegistry.CreateResult result = sessionRegistry.create(
                player.getUniqueId(), workstation.locationKey(), recipe.id());
        if (result == ActiveCookingSessionRegistry.CreateResult.CREATED) {
            sessionRegistry.getByPlayer(player.getUniqueId()).ifPresent(session -> {
                displayService.spawnDisplays(session, recipe);
                beginCurrentStage(player, session, workstation.locationKey().toLocation());
            });
        }
        return result;
    }

    @Override
    public Optional<CookingStage> currentStage(ActiveCookingSession session) {
        if (session == null) {
            return Optional.empty();
        }
        return recipe(session)
                .filter(recipe -> session.progress().currentStageIndex() < recipe.stages().size())
                .map(recipe -> recipe.stages().get(session.progress().currentStageIndex()));
    }

    public CookingStageExecutor.InteractionResult insertHeldIngredient(Player player, PlacedCookingWorkstation workstation, ItemStack held, Location rewardDropLocation) {
        Optional<ActiveCookingSession> sessionOptional = sessionRegistry.getByWorkstation(workstation.locationKey());
        if (sessionOptional.isEmpty()) {
            return CookingStageExecutor.InteractionResult.NO_ACTIVE_SESSION;
        }
        ActiveCookingSession session = sessionOptional.get();
        if (!session.playerId().equals(player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "This cooking workstation is busy.");
            return CookingStageExecutor.InteractionResult.WRONG_PLAYER;
        }
        if (recipe(session).isEmpty()) {
            cancelSession(session, "Cooking recipe is no longer registered.");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Cooking recipe is no longer registered.");
            return CookingStageExecutor.InteractionResult.INVALID_SESSION;
        }
        Optional<CookingStageExecutor> executor = currentExecutor(session);
        if (executor.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Next cooking stage is not implemented yet.");
            return CookingStageExecutor.InteractionResult.UNSUPPORTED_STAGE;
        }
        return executor.get().handleInteraction(session,
                new CookingStageExecutor.StageInteractionContext(this, player, held, rewardDropLocation));
    }

    public boolean cancelSessionByPlayer(java.util.UUID playerId) {
        return cancelSessionByPlayer(playerId, false, null);
    }

    public boolean cancelSessionByPlayer(java.util.UUID playerId, boolean refundIngredients, String logReason) {
        Optional<ActiveCookingSession> session = sessionRegistry.getByPlayer(playerId);
        session.ifPresent(active -> cancelSession(active, logReason, refundIngredients));
        return session.isPresent();
    }

    public void cancelSessionByWorkstation(CookingLocationKey workstationKey) {
        sessionRegistry.getByWorkstation(workstationKey).ifPresent(session -> cancelSession(session, null));
    }

    public void shutdownAndRefundAll() {
        for (ActiveCookingSession session : sessionRegistry.all()) {
            recipe(session).ifPresent(recipe -> refundService.refundInsertedIngredients(session, recipe, plugin.getLogger()));
            cancelSession(session, "Plugin shutting down");
        }
        sessionRegistry.clear();
    }

    @Override
    public Main plugin() {
        return plugin;
    }

    @Override
    public CookingDisplayService displayService() {
        return displayService;
    }

    @Override
    public CookingEffectsService effectsService() {
        return effectsService;
    }

    @Override
    public Optional<CookingRecipe> recipe(ActiveCookingSession session) {
        if (session == null) {
            return Optional.empty();
        }
        return recipeRegistry.get(session.recipeId());
    }

    @Override
    public void advanceStage(Player player, ActiveCookingSession session, Location rewardDropLocation) {
        Optional<CookingRecipe> recipeOptional = recipe(session);
        if (recipeOptional.isEmpty()) {
            cancelSession(session, "Cooking recipe is no longer registered.");
            return;
        }
        CookingRecipe recipe = recipeOptional.get();
        if (session.progress().currentStageIndex() >= recipe.stages().size()) {
            complete(player, session, recipe, rewardDropLocation);
            return;
        }
        beginCurrentStage(player, session, rewardDropLocation);
    }

    @Override
    public void cancelSession(ActiveCookingSession session, String logReason) {
        cancelSession(session, logReason, false);
    }

    public void cancelSession(ActiveCookingSession session, String logReason, boolean refundIngredients) {
        if (refundIngredients) {
            recipe(session).ifPresent(recipe -> refundService.refundInsertedIngredients(session, recipe, plugin.getLogger()));
        }
        currentExecutor(session).ifPresent(executor -> executor.cancelStage(session));
        cleanupSession(session);
        sessionRegistry.removeByWorkstation(session.workstationKey());
        if (logReason != null && !logReason.isBlank()) {
            plugin.getLogger().warning("[Cooking] Cancelled session for recipe '" + session.recipeId() + "': " + logReason);
        }
    }

    @Override
    public boolean isSessionActive(ActiveCookingSession session) {
        return session != null
                && sessionRegistry.getByWorkstation(session.workstationKey()).filter(active -> active == session).isPresent();
    }

    @Override
    public boolean isWorkstationPlaced(ActiveCookingSession session) {
        return session != null && placedWorkstations.find(session.workstationKey()).isPresent();
    }

    private void beginCurrentStage(Player player, ActiveCookingSession session, Location rewardDropLocation) {
        Optional<CookingStage> stageOptional = currentStage(session);
        if (stageOptional.isEmpty()) {
            recipe(session).ifPresent(recipe -> complete(player, session, recipe, rewardDropLocation));
            return;
        }
        CookingStage stage = stageOptional.get();
        Optional<CookingStageExecutor> executorOptional = executorRegistry.get(stage.type());
        if (executorOptional.isEmpty()) {
            session.setActiveStageType(stage.type());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Next cooking stage is not implemented yet.");
            return;
        }
        session.setActiveStageType(stage.type());
        executorOptional.get().beginStage(session,
                new CookingStageExecutor.StageExecutionContext(this, player, rewardDropLocation));
    }

    private Optional<CookingStageExecutor> currentExecutor(ActiveCookingSession session) {
        if (session == null || session.activeStageType() == null) {
            return currentStage(session).flatMap(stage -> executorRegistry.get(stage.type()));
        }
        return executorRegistry.get(session.activeStageType());
    }

    private void complete(Player player, ActiveCookingSession session, CookingRecipe recipe, Location rewardDropLocation) {
        Location workstationLocation = rewardDropLocation != null ? rewardDropLocation : session.workstationKey().toLocation();
        rewardService.grantRewards(player, workstationLocation, recipe.rewards());
        effectsService.playCookingComplete(player, workstationLocation);
        cleanupSession(session);
        sessionRegistry.removeByWorkstation(session.workstationKey());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Completed cooking recipe " + ChatColor.YELLOW + recipe.displayName() + ChatColor.GREEN + ".");
    }

    private void cleanupSession(ActiveCookingSession session) {
        session.cancelWaitTask();
        session.clearActiveStageType();
        displayService.cleanup(session);
    }

}
