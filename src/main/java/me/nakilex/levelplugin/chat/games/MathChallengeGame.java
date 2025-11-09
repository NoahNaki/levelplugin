package me.nakilex.levelplugin.chat.games;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Generates quick arithmetic expressions players must solve.
 */
public class MathChallengeGame extends AbstractChatGame {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+");

    private final int min;
    private final int max;
    private final List<MathOperation> operations;
    private final ChatGameReward reward = new ChatGameReward(30, 80, 1);

    private volatile String expression;
    private volatile int solution;

    public MathChallengeGame(int min, int max, List<String> operationTokens) {
        super("math-challenge", "Math Challenge");
        this.min = min;
        this.max = Math.max(max, min + 1);
        this.operations = parseOperations(operationTokens);
    }

    @Override
    public boolean canPlay() {
        return !operations.isEmpty();
    }

    @Override
    protected void onStart(ChatGameManager manager) {
        if (!canPlay()) {
            stop(manager);
            return;
        }
        generateChallenge();
        manager.broadcastGameStart(this,
                ChatColor.GRAY + "Solve this expression:",
                ChatColor.AQUA + ChatColor.BOLD.toString() + expression);
    }

    @Override
    protected void onStop(ChatGameManager manager) {
        expression = null;
    }

    @Override
    protected Optional<ChatGameResult> onChat(Player player, String message) {
        String activeExpression = expression;
        if (activeExpression == null) {
            return Optional.empty();
        }
        int guess;
        Matcher matcher = NUMBER_PATTERN.matcher(ChatColor.stripColor(message == null ? "" : message).trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            guess = Integer.parseInt(matcher.group());
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        if (guess == solution) {
            return Optional.of(new ChatGameResult(
                    player.getUniqueId(),
                    player.getName(),
                    activeExpression + " = " + solution,
                    reward));
        }
        return Optional.empty();
    }

    private void generateChallenge() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int operationsCount = operations.size() > 1 ? 2 : 1;
        int numbersNeeded = operationsCount + 1;
        int[] numbers = new int[numbersNeeded];
        for (int i = 0; i < numbersNeeded; i++) {
            numbers[i] = random.nextInt(min, max + 1);
        }
        List<MathOperation> chosen = new ArrayList<>(operationsCount);
        for (int i = 0; i < operationsCount; i++) {
            chosen.add(operations.get(random.nextInt(operations.size())));
        }
        this.expression = buildExpression(numbers, chosen);
        this.solution = evaluate(numbers, chosen);
    }

    private String buildExpression(int[] numbers, List<MathOperation> ops) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numbers.length; i++) {
            if (i > 0) {
                builder.append(' ').append(ops.get(i - 1).symbol).append(' ');
            }
            builder.append(numbers[i]);
        }
        return builder.toString();
    }

    private int evaluate(int[] numbers, List<MathOperation> ops) {
        List<Integer> values = new ArrayList<>();
        for (int n : numbers) {
            values.add(n);
        }
        List<MathOperation> pending = new ArrayList<>(ops);
        for (int i = 0; i < pending.size();) {
            MathOperation op = pending.get(i);
            if (op == MathOperation.MULTIPLY) {
                int result = values.get(i) * values.get(i + 1);
                values.set(i, result);
                values.remove(i + 1);
                pending.remove(i);
            } else {
                i++;
            }
        }
        int total = values.get(0);
        for (int i = 0; i < pending.size(); i++) {
            MathOperation op = pending.get(i);
            int next = values.get(i + 1);
            total = op == MathOperation.ADD ? total + next : total - next;
        }
        return total;
    }

    private List<MathOperation> parseOperations(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        EnumSet<MathOperation> set = EnumSet.noneOf(MathOperation.class);
        for (String token : tokens) {
            if (token == null) continue;
            try {
                set.add(MathOperation.valueOf(token.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // skip unknown entries
            }
        }
        return List.copyOf(set);
    }

    private enum MathOperation {
        ADD('+'),
        SUBTRACT('-'),
        MULTIPLY('×');

        private final char symbol;

        MathOperation(char symbol) {
            this.symbol = symbol;
        }
    }
}
