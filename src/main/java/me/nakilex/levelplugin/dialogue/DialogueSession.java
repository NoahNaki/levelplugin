package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.quests.dialogue.QuestDialogueLine;
import me.nakilex.levelplugin.quests.dialogue.QuestDialogueText;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class DialogueSession {
    public enum State { TYPING, WAITING, ANSWERING, FINISHED }

    private final Player player;
    private final DialogueDefinition definition;
    private final Integer npcId;
    private final Location origin;
    private final Runnable onComplete;
    private final Consumer<DialogueAnswer> onAnswer;
    private String pageId;
    private State state = State.TYPING;
    private long stateStartedAt;
    private int selectedAnswerIndex;
    private int pageNumber;
    private int pageCount;
    private int pageLineIndex;
    private int pageLineCount;
    private PreparedLine preparedLine;
    private List<Component> completedPageLines = List.of();
    private List<DialogueAnswer> visibleAnswers = List.of();
    private List<Component> replyLines = List.of();

    DialogueSession(Player player, DialogueDefinition definition, Integer npcId, Location origin,
                    Runnable onComplete, Consumer<DialogueAnswer> onAnswer) {
        this.player = Objects.requireNonNull(player, "player");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.npcId = npcId;
        this.origin = origin == null ? null : origin.clone();
        this.onComplete = onComplete == null ? () -> {} : onComplete;
        this.onAnswer = onAnswer;
    }

    public UUID playerId() { return player.getUniqueId(); }
    public Player player() { return player; }
    public DialogueDefinition definition() { return definition; }
    public Integer npcId() { return npcId; }
    public Location origin() { return origin == null ? null : origin.clone(); }
    public String pageId() { return pageId; }
    public State state() { return state; }
    public int selectedAnswerIndex() { return selectedAnswerIndex; }
    public int lineNumber() { return pageNumber; }
    public int lineCount() { return pageCount; }
    public int pageLineIndex() { return pageLineIndex; }
    public int pageLineCount() { return pageLineCount; }
    public boolean hasCurrentLine() { return preparedLine != null; }
    public List<Component> completedPageLines() { return completedPageLines; }
    public List<DialogueAnswer> visibleAnswers() { return visibleAnswers; }
    public List<Component> replyLines() { return replyLines; }

    void enterPage(String pageId, long now, DialoguePlaceholderFormatter formatter, DialogueConditionEvaluator conditionEvaluator) {
        this.pageId = pageId;
        DialoguePage page = definition.page(pageId);
        this.pageNumber = pageIndex(pageId) + 1;
        this.pageCount = definition.pages().size();
        this.pageLineIndex = 0;
        this.pageLineCount = page.lines().size();
        this.completedPageLines = List.of();
        this.visibleAnswers = page.answers().stream()
                .filter(answer -> conditionEvaluator.canUse(player, answer.condition()))
                .map(answer -> new DialogueAnswer(answer.id(), formatter.format(player, answer.text()), answer.gotoPageId(),
                        answer.replyLines(), answer.condition(), answer.sound(), answer.actions()))
                .toList();
        this.replyLines = List.of();
        this.selectedAnswerIndex = visibleAnswers.isEmpty() ? -1 : 0;
        if (pageLineCount == 0) {
            this.preparedLine = null;
            this.state = visibleAnswers.isEmpty() ? State.WAITING : State.ANSWERING;
            this.stateStartedAt = now;
            return;
        }
        prepareLine(page, formatter, now);
    }

    void enterWaiting(long now) {
        state = State.WAITING;
        stateStartedAt = now;
    }

    boolean advanceLine(long now, DialoguePlaceholderFormatter formatter) {
        if (preparedLine != null) {
            List<Component> completed = new ArrayList<>(completedPageLines);
            completed.add(preparedLine.text().fullComponent());
            completedPageLines = List.copyOf(completed);
        }
        pageLineIndex++;
        DialoguePage page = definition.page(pageId);
        if (pageLineIndex >= pageLineCount) {
            preparedLine = null;
            state = visibleAnswers.isEmpty() ? State.WAITING : State.ANSWERING;
            stateStartedAt = now;
            return false;
        }
        prepareLine(page, formatter, now);
        return true;
    }

    void select(int index) {
        if (visibleAnswers.isEmpty()) {
            selectedAnswerIndex = -1;
            return;
        }
        selectedAnswerIndex = Math.floorMod(index, visibleAnswers.size());
    }

    void replyLines(List<Component> replyLines) {
        this.replyLines = replyLines == null ? List.of() : List.copyOf(replyLines);
    }

    void finish() { state = State.FINISHED; }
    void runComplete() { onComplete.run(); }
    void runAnswer(DialogueAnswer answer) { if (onAnswer != null) onAnswer.accept(answer); }

    QuestDialogueLine line() { return preparedLine == null ? null : preparedLine.line(); }

    String speakerName() {
        QuestDialogueLine line = line();
        return line == null ? definition.defaultSpeaker() : line.speakerName();
    }

    Component visibleText(long now) {
        if (preparedLine == null) return Component.empty();
        long elapsed = Math.max(0L, now - stateStartedAt);
        if (state != State.TYPING) return preparedLine.text().fullComponent();
        return preparedLine.text().sliceForElapsed(elapsed, preparedLine.line().typingMillis());
    }

    boolean typingComplete(long now) {
        return preparedLine != null && now - stateStartedAt >= preparedLine.typingDuration();
    }

    boolean waitComplete(long now) {
        return preparedLine != null && definition.settings().autoAdvance()
                && now - stateStartedAt >= preparedLine.line().waitMillis();
    }

    private void prepareLine(DialoguePage page, DialoguePlaceholderFormatter formatter, long now) {
        QuestDialogueLine line = lineAt(page, pageLineIndex, formatter);
        this.preparedLine = new PreparedLine(line, QuestDialogueText.parse(formatter.format(player, line.text())), now);
        this.state = preparedLine.typingDuration() <= 0 ? State.WAITING : State.TYPING;
        this.stateStartedAt = now;
    }

    private QuestDialogueLine lineAt(DialoguePage page, int index, DialoguePlaceholderFormatter formatter) {
        String raw = page.lines().get(index);
        return QuestDialogueLine.fromLegacy(formatter.format(player, raw), definition.defaultSpeaker(),
                definition.settings().typingMillisPerCharacter(), definition.settings().waitMillis());
    }

    private int pageIndex(String pageId) {
        int index = 0;
        for (String id : definition.pages().keySet()) {
            if (id.equals(pageId)) return index;
            index++;
        }
        return 0;
    }

    private record PreparedLine(QuestDialogueLine line, QuestDialogueText text, long startedAt) {
        long typingDuration() { return text.typingDuration(line.typingMillis()); }
    }
}
