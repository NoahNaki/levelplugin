package me.nakilex.levelplugin.quests.dialogue;

import me.nakilex.levelplugin.utils.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Formatting-safe component representation used by the quest dialogue typewriter.
 * Visible characters are stored separately from formatting so animations never split
 * legacy color codes, MiniMessage tags, or custom glyph placeholders.
 */
public final class QuestDialogueText {
    private static final int MAX_LINE_CHARACTERS = 58;
    private static final int COMMA_PAUSE_MILLIS = 110;
    private static final int SENTENCE_PAUSE_MILLIS = 210;

    private final List<Token> tokens;
    private final List<Token> visibleTokens;

    private QuestDialogueText(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
        this.visibleTokens = tokens.stream().filter(Token::visible).toList();
    }

    public static QuestDialogueText parse(String text) {
        List<Token> flattened = new ArrayList<>();
        flatten(ChatUtil.formattedComponent(text), Style.empty(), flattened);
        return new QuestDialogueText(wrap(flattened));
    }

    public Component fullComponent() {
        return slice(visibleTokens.size());
    }

    public Component sliceForElapsed(long elapsedMillis, long typingMillis) {
        if (visibleTokens.isEmpty() || typingMillis <= 0) {
            return fullComponent();
        }

        double millisPerCharacter = typingMillis / (double) visibleTokens.size();
        double playedMillis = Math.max(0L, elapsedMillis);
        int visibleCharacters = 0;
        for (Token token : visibleTokens) {
            if (playedMillis < millisPerCharacter) {
                break;
            }
            playedMillis -= millisPerCharacter;
            visibleCharacters++;
            int punctuationPause = punctuationPause(token.plainText());
            if (punctuationPause > 0) {
                if (playedMillis < punctuationPause) {
                    break;
                }
                playedMillis -= punctuationPause;
            }
        }
        return slice(visibleCharacters);
    }

    public long typingDuration(long baseTypingMillis) {
        long punctuationMillis = visibleTokens.stream()
                .mapToLong(token -> punctuationPause(token.plainText()))
                .sum();
        return Math.max(0L, baseTypingMillis) + punctuationMillis;
    }

    private Component slice(int visibleCharacters) {
        Component result = Component.empty();
        int remaining = Math.max(0, visibleCharacters);
        for (Token token : tokens) {
            if (token.visible()) {
                if (remaining == 0) {
                    break;
                }
                remaining--;
            }
            result = result.append(token.component());
        }
        return result;
    }

    private static void flatten(Component component, Style inheritedStyle, List<Token> tokens) {
        Style style = inheritedStyle.merge(component.style());
        if (component instanceof TextComponent textComponent) {
            tokenize(textComponent.content(), style, tokens);
        }
        for (Component child : component.children()) {
            flatten(child, style, tokens);
        }
    }

    private static void tokenize(String content, Style style, List<Token> tokens) {
        for (int offset = 0; offset < content.length();) {
            if (content.startsWith("<glyph:", offset)) {
                int end = content.indexOf('>', offset);
                if (end >= 0) {
                    String glyph = content.substring(offset, end + 1);
                    tokens.add(Token.visible(glyph, style));
                    offset = end + 1;
                    continue;
                }
            }

            int codePoint = content.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            tokens.add("\n".equals(character) ? Token.lineBreak() : Token.visible(character, style));
            offset += Character.charCount(codePoint);
        }
    }

    private static List<Token> wrap(List<Token> tokens) {
        List<Token> wrapped = new ArrayList<>();
        List<Token> line = new ArrayList<>();
        int lineCharacters = 0;
        int lastSpace = -1;

        for (Token token : tokens) {
            if (token.newline()) {
                appendTrimmed(wrapped, line);
                wrapped.add(Token.lineBreak());
                line.clear();
                lineCharacters = 0;
                lastSpace = -1;
                continue;
            }

            line.add(token);
            lineCharacters++;
            if (token.whitespace()) {
                lastSpace = line.size() - 1;
            }
            if (lineCharacters <= MAX_LINE_CHARACTERS) {
                continue;
            }

            int splitAt = lastSpace >= 0 ? lastSpace : Math.max(1, line.size() - 1);
            List<Token> remainder = new ArrayList<>(line.subList(splitAt + (lastSpace >= 0 ? 1 : 0), line.size()));
            appendTrimmed(wrapped, line.subList(0, splitAt));
            wrapped.add(Token.lineBreak());
            line = remainder;
            trimLeadingSpaces(line);
            lineCharacters = visibleCount(line);
            lastSpace = lastSpace(line);
        }
        appendTrimmed(wrapped, line);
        return wrapped;
    }

    private static void appendTrimmed(List<Token> output, List<Token> line) {
        int end = line.size();
        while (end > 0 && line.get(end - 1).whitespace()) {
            end--;
        }
        output.addAll(line.subList(0, end));
    }

    private static void trimLeadingSpaces(List<Token> line) {
        while (!line.isEmpty() && line.get(0).whitespace()) {
            line.remove(0);
        }
    }

    private static int visibleCount(List<Token> tokens) {
        return (int) tokens.stream().filter(Token::visible).count();
    }

    private static int lastSpace(List<Token> tokens) {
        for (int index = tokens.size() - 1; index >= 0; index--) {
            if (tokens.get(index).whitespace()) {
                return index;
            }
        }
        return -1;
    }

    private static int punctuationPause(String text) {
        return switch (text) {
            case ",", ";", ":" -> COMMA_PAUSE_MILLIS;
            case ".", "!", "?" -> SENTENCE_PAUSE_MILLIS;
            default -> 0;
        };
    }

    private record Token(Component component, String plainText, boolean visible, boolean newline) {
        static Token visible(String text, Style style) {
            return new Token(Component.text(text).style(style), text, true, false);
        }

        static Token lineBreak() {
            return new Token(Component.newline(), "\n", false, true);
        }

        boolean whitespace() {
            return visible && plainText.isBlank();
        }
    }
}
