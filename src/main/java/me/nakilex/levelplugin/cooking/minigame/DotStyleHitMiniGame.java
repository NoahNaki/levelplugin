package me.nakilex.levelplugin.cooking.minigame;

import me.nakilex.levelplugin.cooking.stage.CookingStageExecutor;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/** Timing mini-game where the player right-clicks the workstation inside a centered hit window. */
public class DotStyleHitMiniGame implements CookingMiniGame {
    private static final long TICK_PERIOD = 1L;

    @Override
    public CookingMiniGameType type() {
        return CookingMiniGameType.DOT_STYLE_HIT;
    }

    @Override
    public void begin(CookingMiniGameSession session,
                      CookingStageExecutor.StageExecutionContext context,
                      Runnable onSuccess,
                      Consumer<String> onFailure) {
        long durationTicks = Math.max(1L, session.stage().durationTicks());
        long hitWindowTicks = Math.max(1L, session.stage().hitWindowTicks());
        long targetTick = durationTicks / 2L;
        session.setTiming(targetTick, hitWindowTicks);
        context.controller().displayService().clearStageDisplays(session.cookingSession());
        context.controller().displayService().updateMiniGameHitProgress(session.cookingSession(), 0L, targetTick, hitWindowTicks);
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.INFO,
                "Cooking timing challenge started. Right-click when the marker reaches the target.");
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
        long elapsed = session.elapsedTicks();
        long halfWindow = Math.max(1L, session.hitWindowTicks()) / 2L;
        long delta = elapsed - session.targetTick();
        if (Math.abs(delta) <= halfWindow) {
            session.finish();
            context.player().sendActionBar(Component.empty());
            ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.SUCCESS, "Perfect timing!");
            onSuccess.run();
            return CookingStageExecutor.InteractionResult.COMPLETED;
        }
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                delta < 0 ? "Too early! Try again." : "Too late! Try again.");
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
        context.controller().displayService().updateMiniGameHitProgress(
                session.cookingSession(), elapsed, session.targetTick(), session.hitWindowTicks());
        player.sendActionBar(Component.text(formatActionBar(elapsed, session.targetTick(), session.hitWindowTicks())));
        if (elapsed >= durationTicks) {
            session.finish();
            player.sendActionBar(Component.empty());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Cooking timing challenge failed.");
            onFailure.accept("Cooking minigame failed.");
            return;
        }
        session.setElapsedTicks(elapsed + 1L);
    }

    private String formatActionBar(long elapsed, long targetTick, long hitWindowTicks) {
        long halfWindow = Math.max(1L, hitWindowTicks) / 2L;
        long delta = elapsed - targetTick;
        if (Math.abs(delta) <= halfWindow) {
            return ChatColor.GREEN + "HIT NOW!";
        }
        if (delta < 0) {
            return ChatColor.YELLOW + "Get ready...";
        }
        return ChatColor.RED + "Too late!";
    }
}
