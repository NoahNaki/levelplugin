package me.nakilex.levelplugin.cooking.minigame;

import me.nakilex.levelplugin.cooking.stage.CookingStageExecutor;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/** Fill-bar mini-game where the player repeatedly right-clicks before the timer expires. */
public class DotStyleMixMiniGame implements CookingMiniGame {
    private static final long TICK_PERIOD = 1L;
    private static final int DEFAULT_BAR_SIZE = 10;

    @Override
    public CookingMiniGameType type() {
        return CookingMiniGameType.DOT_STYLE_MIX;
    }

    @Override
    public void begin(CookingMiniGameSession session,
                      CookingStageExecutor.StageExecutionContext context,
                      Runnable onSuccess,
                      Consumer<String> onFailure) {
        long durationTicks = Math.max(1L, session.stage().durationTicks());
        int requiredClicks = Math.max(1, session.stage().requiredClicks());
        int barSize = Math.max(1, session.stage().barSize() > 0 ? session.stage().barSize() : DEFAULT_BAR_SIZE);
        session.setElapsedTicks(0L);
        session.configureMixVisuals(barSize, requiredClicks);
        context.controller().displayService().clearStageDisplays(session.cookingSession());
        showVisual(session, context.player(), context.controller());
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.INFO,
                "Cooking mix challenge started. Right-click the workstation " + requiredClicks + " times before time runs out.");
        BukkitTask task = context.controller().plugin().getServer().getScheduler().runTaskTimer(
                context.controller().plugin(),
                () -> tick(session, context, durationTicks, onFailure),
                0L,
                TICK_PERIOD);
        session.setTask(task);
    }

    @Override
    public CookingStageExecutor.InteractionResult handleInteraction(CookingMiniGameSession session,
                                                                     CookingStageExecutor.StageInteractionContext context,
                                                                     Runnable onSuccess) {
        int clicks = session.incrementClicks();
        showVisual(session, context.player(), context.controller());
        if (clicks >= session.requiredClicks()) {
            session.finish();
            context.controller().displayService().clearMiniGameVisual(context.player());
            ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.SUCCESS, "Mixing complete!");
            onSuccess.run();
            return CookingStageExecutor.InteractionResult.COMPLETED;
        }
        return CookingStageExecutor.InteractionResult.ACCEPTED;
    }

    @Override
    public void cancel(CookingMiniGameSession session) {
        if (session != null) {
            session.finish();
        }
    }

    private void tick(CookingMiniGameSession session,
                      CookingStageExecutor.StageExecutionContext context,
                      long durationTicks,
                      Consumer<String> onFailure) {
        if (session.finished()) {
            return;
        }
        Player player = context.player();
        if (!player.isOnline()
                || !context.controller().isSessionActive(session.cookingSession())
                || !context.controller().isWorkstationPlaced(session.cookingSession())) {
            session.finish();
            onFailure.accept("Cooking mix minigame stopped because the session is no longer valid.");
            return;
        }
        long elapsed = session.elapsedTicks();
        showVisual(session, player, context.controller());
        if (elapsed >= durationTicks) {
            session.finish();
            context.controller().displayService().clearMiniGameVisual(player);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Cooking mix challenge failed.");
            onFailure.accept("Cooking mix minigame failed.");
            return;
        }
        session.setElapsedTicks(elapsed + 1L);
    }

    private void showVisual(CookingMiniGameSession session, Player player, CookingStageExecutor.StageSessionController controller) {
        controller.displayService().showMiniGameVisual(player,
                CookingMiniGameBarFormatter.mixBar(session.clicks(), session.requiredClicks(), session.barSize()));
    }
}
