package me.nakilex.levelplugin.cooking.stage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.service.CookingDisplayService;
import me.nakilex.levelplugin.cooking.service.CookingEffectsService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Executes runtime behavior for a specific cooking stage type. */
public interface CookingStageExecutor {
    CookingStageType type();

    void beginStage(ActiveCookingSession session, StageExecutionContext context);

    InteractionResult handleInteraction(ActiveCookingSession session, StageInteractionContext context);

    void cancelStage(ActiveCookingSession session);

    void completeStage(ActiveCookingSession session, StageExecutionContext context);

    enum InteractionResult {
        ACCEPTED,
        COMPLETED,
        NO_ACTIVE_SESSION,
        WRONG_PLAYER,
        INVALID_SESSION,
        INVALID_INGREDIENT,
        NOT_ENOUGH_ITEMS,
        INGREDIENT_ALREADY_COMPLETE,
        UNSUPPORTED_STAGE
    }

    record StageExecutionContext(StageSessionController controller, Player player, Location rewardDropLocation) {}

    record StageInteractionContext(StageSessionController controller, Player player, ItemStack heldItem, Location rewardDropLocation) {}

    /** Narrow orchestration callbacks exposed to stage executors by the session service. */
    interface StageSessionController {
        Main plugin();

        CookingDisplayService displayService();

        CookingEffectsService effectsService();

        Optional<CookingRecipe> recipe(ActiveCookingSession session);

        Optional<CookingStage> currentStage(ActiveCookingSession session);

        void advanceStage(Player player, ActiveCookingSession session, Location rewardDropLocation);

        void cancelSession(ActiveCookingSession session, String logReason);

        void suppressRecipeBookOpen(Player player);

        boolean isSessionActive(ActiveCookingSession session);

        boolean isWorkstationPlaced(ActiveCookingSession session);
    }
}
