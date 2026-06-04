package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.quests.dialogue.QuestDialogueLine;
import me.nakilex.levelplugin.quests.dialogue.QuestDialogueText;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
    private int lineNumber;
    private int lineCount;
    private PreparedLine preparedLine;
    private List<DialogueAnswer> visibleAnswers = List.of();
    private List<net.kyori.adventure.text.Component> replyLines = List.of();

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
    public int lineNumber() { return lineNumber; }
    public int lineCount() { return lineCount; }
    public List<DialogueAnswer> visibleAnswers() { return visibleAnswers; }
    public List<net.kyori.adventure.text.Component> replyLines() { return replyLines; }

    void enterPage(String pageId, long now, DialoguePlaceholderFormatter formatter, DialogueConditionEvaluator conditionEvaluator) {
        this.pageId = pageId;
        DialoguePage page = definition.page(pageId);
        this.lineNumber = pageIndex(pageId) + 1;
        this.lineCount = definition.pages().size();
        QuestDialogueLine line = firstLine(page, formatter);
        this.preparedLine = new PreparedLine(line, QuestDialogueText.parse(formatter.format(player, line.text())), now);
        this.visibleAnswers = page.answers().stream()
                .filter(answer -> conditionEvaluator.canUse(player, answer.condition()))
                .map(answer -> new DialogueAnswer(answer.id(), formatter.format(player, answer.text()), answer.gotoPageId(),
                        answer.replyLines(), answer.condition(), answer.sound(), answer.actions()))
                .toList();
        this.replyLines = List.of();
        this.selectedAnswerIndex = visibleAnswers.isEmpty() ? -1 : 0;
        this.state = preparedLine.typingDuration() <= 0 ? (visibleAnswers.isEmpty() ? State.WAITING : State.ANSWERING) : State.TYPING;
        this.stateStartedAt = now;
    }

    void enterWaiting(long now) {
        state = visibleAnswers.isEmpty() ? State.WAITING : State.ANSWERING;
        stateStartedAt = now;
    }

    void select(int index) {
        if (visibleAnswers.isEmpty()) {
            selectedAnswerIndex = -1;
            return;
        }
        selectedAnswerIndex = Math.floorMod(index, visibleAnswers.size());
    }

    void replyLines(List<net.kyori.adventure.text.Component> replyLines) {
        this.replyLines = replyLines == null ? List.of() : List.copyOf(replyLines);
    }

    void finish() { state = State.FINISHED; }
    void runComplete() { onComplete.run(); }
    void runAnswer(DialogueAnswer answer) { if (onAnswer != null) onAnswer.accept(answer); }

    QuestDialogueLine line() { return preparedLine.line(); }

    net.kyori.adventure.text.Component visibleText(long now) {
        long elapsed = Math.max(0L, now - stateStartedAt);
        if (state != State.TYPING) return preparedLine.text().fullComponent();
        return preparedLine.text().sliceForElapsed(elapsed, preparedLine.line().typingMillis());
    }

    boolean typingComplete(long now) {
        return now - stateStartedAt >= preparedLine.typingDuration();
    }

    boolean waitComplete(long now) {
        return definition.settings().autoAdvance() && now - stateStartedAt >= preparedLine.line().waitMillis();
    }

    private QuestDialogueLine firstLine(DialoguePage page, DialoguePlaceholderFormatter formatter) {
        String raw = page.lines().isEmpty() ? definition.defaultSpeaker() + "|" : page.lines().get(0);
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
