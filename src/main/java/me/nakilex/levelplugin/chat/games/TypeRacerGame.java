package me.nakilex.levelplugin.chat.games;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Presents a phrase that must be typed verbatim.
 */
public class TypeRacerGame extends AbstractChatGame {

    private final List<String> phrases;
    private final ChatGameReward reward = new ChatGameReward(0, 120, 0);

    private volatile String activePhrase;

    public TypeRacerGame(List<String> phrases) {
        super("type-racer", "Type Racer");
        this.phrases = phrases;
    }

    @Override
    public boolean canPlay() {
        return phrases != null && !phrases.isEmpty();
    }

    @Override
    protected void onStart(ChatGameManager manager) {
        if (!canPlay()) {
            stop(manager);
            return;
        }
        activePhrase = phrases.get(ThreadLocalRandom.current().nextInt(phrases.size()));
        manager.broadcastGameStart(this,
                ChatColor.GRAY + "Type this phrase exactly:",
                ChatColor.AQUA + ChatColor.ITALIC.toString() + activePhrase);
    }

    @Override
    protected void onStop(ChatGameManager manager) {
        activePhrase = null;
    }

    @Override
    protected Optional<ChatGameResult> onChat(Player player, String message) {
        String phrase = activePhrase;
        if (phrase == null) {
            return Optional.empty();
        }
        if (phrase.equals(message)) {
            return Optional.of(new ChatGameResult(
                    player.getUniqueId(),
                    player.getName(),
                    phrase,
                    reward));
        }
        return Optional.empty();
    }
}
