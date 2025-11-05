package me.nakilex.levelplugin.chat.games;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/** Presents a scrambled word players must unscramble. */
public class WordScrambleGame extends AbstractChatGame {

    private final List<String> words;
    private final ChatGameReward reward = new ChatGameReward(40, 60, 0);

    private volatile String solution;
    private volatile String scrambled;

    public WordScrambleGame(List<String> words) {
        super("word-scramble", "Word Scramble");
        this.words = words;
    }

    @Override
    public boolean canPlay() {
        return words != null && !words.isEmpty();
    }

    @Override
    protected void onStart(ChatGameManager manager) {
        if (!canPlay()) {
            stop(manager);
            return;
        }
        String word = words.get(ThreadLocalRandom.current().nextInt(words.size()));
        this.solution = word;
        this.scrambled = scramble(word);
        manager.broadcastGameStart(this,
                ChatColor.GRAY + "Unscramble the word!",
                ChatColor.AQUA + ChatColor.BOLD.toString() + scrambled);
    }

    @Override
    protected void onStop(ChatGameManager manager) {
        this.solution = null;
        this.scrambled = null;
    }

    @Override
    protected Optional<ChatGameResult> onChat(Player player, String message) {
        String answer = solution;
        if (answer == null) {
            return Optional.empty();
        }
        String normalized = ChatColor.stripColor(message == null ? "" : message).trim().toLowerCase();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (normalized.equalsIgnoreCase(answer)) {
            return Optional.of(new ChatGameResult(
                    player.getUniqueId(),
                    player.getName(),
                    answer,
                    reward));
        }
        return Optional.empty();
    }

    private String scramble(String word) {
        List<Character> chars = word.chars()
                .mapToObj(c -> (char) c)
                .collect(java.util.stream.Collectors.toList());
        String shuffled = word;
        int attempts = 0;
        while (shuffled.equalsIgnoreCase(word) && attempts++ < 10) {
            Collections.shuffle(chars, ThreadLocalRandom.current());
            StringBuilder builder = new StringBuilder(chars.size());
            for (char c : chars) {
                builder.append(c);
            }
            shuffled = builder.toString();
        }
        return shuffled;
    }
}
