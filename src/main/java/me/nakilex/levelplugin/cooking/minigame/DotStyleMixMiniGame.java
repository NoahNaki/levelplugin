package me.nakilex.levelplugin.cooking.minigame;

import me.nakilex.levelplugin.cooking.stage.CookingStageExecutor;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/** Click-count mini-game where the player repeatedly right-clicks before the timer expires. */
public class DotStyleMixMiniGame implements CookingMiniGame {
    private static final long TICK_PERIOD = 1L;

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
        session.setElapsedTicks(0L);
        session.setRequiredClicks(requiredClicks);
        context.controller().displayService().clearStageDisplays(session.cookingSession());
        context.controller().displayService().updateMiniGameMixProgress(
                session.cookingSession(), session.clicks(), requiredClicks, durationTicks);
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
        long remainingTicks = Math.max(0L, session.stage().durationTicks() - session.elapsedTicks());
        context.controller().displayService().updateMiniGameMixProgress(
                session.cookingSession(), clicks, session.requiredClicks(), remainingTicks);
        context.player().sendActionBar(Component.text(formatActionBar(clicks, session.requiredClicks())));
        if (clicks >= session.requiredClicks()) {
            session.finish();
            context.player().sendActionBar(Component.empty());
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
        long remainingTicks = Math.max(0L, durationTicks - elapsed);
        context.controller().displayService().updateMiniGameMixProgress(
                session.cookingSession(), session.clicks(), session.requiredClicks(), remainingTicks);
        player.sendActionBar(Component.text(formatActionBar(session.clicks(), session.requiredClicks())));
        if (elapsed >= durationTicks) {
            session.finish();
            player.sendActionBar(Component.empty());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Cooking mix challenge failed.");
            onFailure.accept("Cooking mix minigame failed.");
            return;
        }
        session.setElapsedTicks(elapsed + 1L);
    }

    private String formatActionBar(int clicks, int requiredClicks) {
        return ChatColor.AQUA + "Mix! " + ChatColor.WHITE + clicks + ChatColor.GRAY + "/" + ChatColor.WHITE + requiredClicks;
    }
}
