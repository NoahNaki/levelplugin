package me.nakilex.levelplugin.cooking.stage;

import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.runtime.CookingWaitTask;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Executes WAIT stages through a scheduled countdown task. */
public class WaitStageExecutor implements CookingStageExecutor {
    private static final long WAIT_TICK_PERIOD = 20L;

    @Override
    public CookingStageType type() {
        return CookingStageType.WAIT;
    }

    @Override
    public void beginStage(ActiveCookingSession session, StageExecutionContext context) {
        CookingStage stage = context.controller().currentStage(session).orElse(null);
        if (stage == null) {
            context.controller().advanceStage(context.player(), session, context.rewardDropLocation());
            return;
        }
        long durationTicks = Math.max(1L, stage.durationTicks());
        context.controller().displayService().updateWaitProgress(session, -1L);
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.INFO,
                "Cooking timer started for " + Math.ceil(durationTicks / 20.0D) + "s.");
        CookingWaitTask waitTask = new CookingWaitTask(
                durationTicks,
                WAIT_TICK_PERIOD,
                () -> isWaitStageStillValid(session, context.controller()),
                remainingTicks -> updateWaitDisplay(session, context.controller(), remainingTicks),
                () -> completeStage(session, context),
                () -> context.controller().cancelSession(session, null)
        );
        BukkitTask task = waitTask.runTaskTimer(context.controller().plugin(), 0L, WAIT_TICK_PERIOD);
        session.setWaitTask(task);
    }

    @Override
    public InteractionResult handleInteraction(ActiveCookingSession session, StageInteractionContext context) {
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                "This stage does not accept ingredient insertion right now.");
        return InteractionResult.UNSUPPORTED_STAGE;
    }

    @Override
    public void cancelStage(ActiveCookingSession session) {
        session.cancelWaitTask();
    }

    @Override
    public void completeStage(ActiveCookingSession session, StageExecutionContext context) {
        session.cancelWaitTask();
        Player player = Bukkit.getPlayer(session.playerId());
        if (player == null || !player.isOnline()) {
            context.controller().cancelSession(session, null);
            return;
        }
        if (context.controller().recipe(session).isEmpty()) {
            context.controller().cancelSession(session, "Cooking recipe is no longer registered.");
            return;
        }
        session.progress().advance();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Cooking timer complete.");
        context.controller().advanceStage(player, session, session.workstationKey().toLocation());
    }

    private boolean isWaitStageStillValid(ActiveCookingSession session, StageSessionController controller) {
        return controller.isSessionActive(session) && controller.isWorkstationPlaced(session);
    }

    private void updateWaitDisplay(ActiveCookingSession session, StageSessionController controller, long remainingTicks) {
        long secondsLeft = (long) Math.ceil(remainingTicks / 20.0D);
        controller.displayService().updateWaitProgress(session, secondsLeft);
    }
}
