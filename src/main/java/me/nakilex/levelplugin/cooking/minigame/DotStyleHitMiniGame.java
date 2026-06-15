package me.nakilex.levelplugin.cooking.minigame;

import me.nakilex.levelplugin.cooking.stage.CookingStageExecutor;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Moving-hook mini-game where the player right-clicks when the hook overlaps the target. */
public class DotStyleHitMiniGame implements CookingMiniGame {
    private static final long TICK_PERIOD = 1L;
    private static final int DEFAULT_BAR_SIZE = 10;
    private static final int DEFAULT_TARGET_SCORE = 3;
    private static final int DEFAULT_HEALTH = 3;
    private static final long DEFAULT_SPEED_TICKS = 5L;
    private static final long DEFAULT_DURATION_TICKS = 200L;

    @Override
    public CookingMiniGameType type() {
        return CookingMiniGameType.DOT_STYLE_HIT;
    }

    @Override
    public void begin(CookingMiniGameSession session,
                      CookingStageExecutor.StageExecutionContext context,
                      Runnable onSuccess,
                      Consumer<String> onFailure) {
        long durationTicks = configuredDuration(session);
        int barSize = Math.max(1, session.stage().barSize() > 0 ? session.stage().barSize() : DEFAULT_BAR_SIZE);
        int targetScore = Math.max(1, session.stage().targetScore() > 0 ? session.stage().targetScore() : DEFAULT_TARGET_SCORE);
        int health = Math.max(1, session.stage().health() > 0 ? session.stage().health() : DEFAULT_HEALTH);
        session.setElapsedTicks(0L);
        session.configureHitVisuals(barSize, targetScore, health, randomTargetIndex(barSize));
        context.controller().displayService().clearStageDisplays(session.cookingSession());
        showVisual(session, context.player(), context.controller());
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.INFO,
                "Cooking timing challenge started. Right-click when the moving hook overlaps the target.");
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
        if (session.hookIndex() == session.targetIndex()) {
            int score = session.incrementScore();
            if (score >= session.targetScore()) {
                session.finish();
                context.controller().displayService().clearMiniGameVisual(context.player());
                ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.SUCCESS, "Perfect timing!");
                onSuccess.run();
                return CookingStageExecutor.InteractionResult.COMPLETED;
            }
            session.setTargetIndex(randomTargetIndex(session.barSize()));
            showVisual(session, context.player(), context.controller());
            ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.SUCCESS, "Good hit!");
            return CookingStageExecutor.InteractionResult.ACCEPTED;
        }

        int health = session.decrementHealth();
        showVisual(session, context.player(), context.controller());
        if (health <= 0) {
            session.finish();
            context.controller().displayService().clearMiniGameVisual(context.player());
            ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.ERROR, "Cooking timing challenge failed.");
            context.controller().cancelSession(session.cookingSession(), "Cooking minigame failed.");
            return CookingStageExecutor.InteractionResult.COMPLETED;
        }
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING, "Missed! Try again.");
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
            onFailure.accept("Cooking minigame stopped because the session is no longer valid.");
            return;
        }
        long elapsed = session.elapsedTicks();
        if (elapsed > 0L && elapsed % speedTicks(session) == 0L) {
            session.stepHook();
        }
        showVisual(session, player, context.controller());
        if (elapsed >= durationTicks) {
            session.finish();
            context.controller().displayService().clearMiniGameVisual(player);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Cooking timing challenge failed.");
            onFailure.accept("Cooking minigame failed.");
            return;
        }
        session.setElapsedTicks(elapsed + 1L);
    }

    private void showVisual(CookingMiniGameSession session, Player player, CookingStageExecutor.StageSessionController controller) {
        controller.displayService().showMiniGameVisual(player, CookingMiniGameBarFormatter.hitBar(
                session.hookIndex(),
                session.targetIndex(),
                session.barSize(),
                session.score(),
                session.targetScore(),
                session.health(),
                session.stage().hitTargetSymbol(),
                session.stage().hitHookSymbol(),
                session.stage().hitLineSymbol(),
                session.stage().healthSymbol()));
    }

    private long configuredDuration(CookingMiniGameSession session) {
        return Math.max(1L, session.stage().durationTicks() > 0L ? session.stage().durationTicks() : DEFAULT_DURATION_TICKS);
    }

    private long speedTicks(CookingMiniGameSession session) {
        return Math.max(1L, session.stage().speedTicks() > 0L ? session.stage().speedTicks() : DEFAULT_SPEED_TICKS);
    }

    private int randomTargetIndex(int barSize) {
        return ThreadLocalRandom.current().nextInt(Math.max(1, barSize));
    }
}
