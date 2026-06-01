package me.nakilex.levelplugin.quests.dialogue;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Tracks one player's progress through a simple linear quest dialogue.
 */
public class QuestDialogueSession {
    public enum State {
        TYPING,
        WAITING,
        FINISHED
    }

    /** Renders the currently visible portion of a line and clears it when the session ends. */
    public interface Renderer {
        void render(Player player, QuestDialogueLine line, String visibleText, State state, int lineNumber, int lineCount);

        void clear(Player player);
    }

    private static final Runnable NO_OP = () -> {};

    private final Player player;
    private final int npcId;
    private final List<QuestDialogueLine> lines;
    private final Runnable onFinish;
    private final Consumer<QuestDialogueSession> onFinished;
    private final LongSupplier clock;
    private final Renderer renderer;

    private int lineIndex;
    private State state = State.TYPING;
    private long stateStartedAt;

    public QuestDialogueSession(Player player, int npcId, List<QuestDialogueLine> lines, Runnable onFinish,
                                Consumer<QuestDialogueSession> onFinished, LongSupplier clock, Renderer renderer) {
        this.player = Objects.requireNonNull(player, "player");
        this.npcId = npcId;
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (this.lines.isEmpty()) {
            throw new IllegalArgumentException("lines cannot be empty");
        }
        this.onFinish = onFinish == null ? NO_OP : onFinish;
        this.onFinished = Objects.requireNonNull(onFinished, "onFinished");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        startLine();
    }

    /** Update the current typing animation or automatically advance after the configured wait. */
    public void tick() {
        if (state == State.FINISHED) {
            return;
        }

        QuestDialogueLine line = currentLine();
        long elapsed = Math.max(0L, clock.getAsLong() - stateStartedAt);
        if (state == State.TYPING) {
            if (elapsed < line.typingMillis()) {
                double percent = elapsed / (double) line.typingMillis();
                int chars = (int) (line.text().length() * Math.min(1.0, percent));
                render(line.text().substring(0, chars));
                return;
            }
            enterWaiting();
            if (line.waitMillis() > 0) {
                return;
            }
        }

        elapsed = Math.max(0L, clock.getAsLong() - stateStartedAt);
        if (elapsed >= line.waitMillis()) {
            nextLine();
        }
    }

    /** Skip an in-progress typing animation, or advance a fully revealed line. */
    public void nextOrSkip() {
        if (state == State.TYPING) {
            enterWaiting();
            return;
        }
        if (state == State.WAITING) {
            nextLine();
        }
    }

    /** Stop the dialogue without invoking its completion callback. */
    public void cancel() {
        if (state == State.FINISHED) {
            return;
        }
        state = State.FINISHED;
        renderer.clear(player);
    }

    public Player getPlayer() {
        return player;
    }

    public int getNpcId() {
        return npcId;
    }

    public State getState() {
        return state;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    private void startLine() {
        state = State.TYPING;
        stateStartedAt = clock.getAsLong();
        if (currentLine().typingMillis() == 0) {
            enterWaiting();
        } else {
            render("");
        }
    }

    private void nextLine() {
        lineIndex++;
        if (lineIndex >= lines.size()) {
            finish();
            return;
        }
        startLine();
    }

    private void enterWaiting() {
        state = State.WAITING;
        stateStartedAt = clock.getAsLong();
        showFullLine();
    }

    private void showFullLine() {
        render(currentLine().text());
    }

    private void render(String visibleText) {
        renderer.render(player, currentLine(), visibleText, state, lineIndex + 1, lines.size());
    }

    private QuestDialogueLine currentLine() {
        return lines.get(lineIndex);
    }

    private void finish() {
        state = State.FINISHED;
        renderer.clear(player);
        onFinished.accept(this);
        onFinish.run();
    }
}
