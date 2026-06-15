package me.nakilex.levelplugin.cooking.stage;

import me.nakilex.levelplugin.cooking.minigame.CookingMiniGame;
import me.nakilex.levelplugin.cooking.minigame.CookingMiniGameSession;
import me.nakilex.levelplugin.cooking.minigame.CookingMiniGameType;
import me.nakilex.levelplugin.cooking.minigame.DotStyleHitMiniGame;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Executes cooking MINI_GAME stages while mini-game implementations own game-specific behavior. */
public class MiniGameStageExecutor implements CookingStageExecutor {
    private final Map<CookingMiniGameType, CookingMiniGame> miniGames = new EnumMap<>(CookingMiniGameType.class);
    private final Map<ActiveCookingSession, CookingMiniGameSession> activeMiniGames = new ConcurrentHashMap<>();

    public MiniGameStageExecutor() {
        register(new DotStyleHitMiniGame());
    }

    @Override
    public CookingStageType type() {
        return CookingStageType.MINI_GAME;
    }

    @Override
    public void beginStage(ActiveCookingSession session, StageExecutionContext context) {
        CookingStage stage = context.controller().currentStage(session).orElse(null);
        if (stage == null) {
            context.controller().advanceStage(context.player(), session, context.rewardDropLocation());
            return;
        }
        CookingMiniGame miniGame = miniGames.get(stage.miniGameType());
        if (miniGame == null) {
            ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                    "Cooking minigame is not implemented yet.");
            context.controller().cancelSession(session, "Cooking minigame is not implemented: " + stage.miniGameType());
            return;
        }
        CookingMiniGameSession miniGameSession = new CookingMiniGameSession(session, stage, stage.miniGameType());
        activeMiniGames.put(session, miniGameSession);
        miniGame.begin(miniGameSession, context,
                () -> completeStage(session, context),
                reason -> context.controller().cancelSession(session, reason));
    }

    @Override
    public InteractionResult handleInteraction(ActiveCookingSession session, StageInteractionContext context) {
        CookingMiniGameSession miniGameSession = activeMiniGames.get(session);
        if (miniGameSession == null) {
            return InteractionResult.INVALID_SESSION;
        }
        CookingMiniGame miniGame = miniGames.get(miniGameSession.type());
        if (miniGame == null) {
            return InteractionResult.UNSUPPORTED_STAGE;
        }
        return miniGame.handleInteraction(miniGameSession, context,
                () -> completeStage(session, new StageExecutionContext(context.controller(), context.player(), context.rewardDropLocation())));
    }

    @Override
    public void cancelStage(ActiveCookingSession session) {
        CookingMiniGameSession miniGameSession = activeMiniGames.remove(session);
        if (miniGameSession == null) {
            return;
        }
        Optional.ofNullable(miniGames.get(miniGameSession.type())).ifPresent(miniGame -> miniGame.cancel(miniGameSession));
        Player player = Bukkit.getPlayer(session.playerId());
        if (player != null) {
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
        }
    }

    @Override
    public void completeStage(ActiveCookingSession session, StageExecutionContext context) {
        CookingMiniGameSession miniGameSession = activeMiniGames.remove(session);
        if (miniGameSession != null) {
            miniGameSession.finish();
        }
        context.controller().displayService().clearStageDisplays(session);
        session.progress().advance();
        context.controller().advanceStage(context.player(), session, context.rewardDropLocation());
    }

    private void register(CookingMiniGame miniGame) {
        miniGames.put(miniGame.type(), miniGame);
    }
}
