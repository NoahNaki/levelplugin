package me.nakilex.levelplugin.cooking.minigame;

import me.nakilex.levelplugin.cooking.stage.CookingStageExecutor;

/** Runtime implementation contract for cooking mini-game stage behavior. */
public interface CookingMiniGame {
    CookingMiniGameType type();

    void begin(CookingMiniGameSession session,
               CookingStageExecutor.StageExecutionContext context,
               Runnable onSuccess,
               java.util.function.Consumer<String> onFailure);

    CookingStageExecutor.InteractionResult handleInteraction(CookingMiniGameSession session,
                                                              CookingStageExecutor.StageInteractionContext context,
                                                              Runnable onSuccess);

    void cancel(CookingMiniGameSession session);
}
