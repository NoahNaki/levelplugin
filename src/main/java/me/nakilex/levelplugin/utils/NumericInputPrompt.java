package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic chat prompt for requesting a numeric value from a player.
 * A parser and validator are supplied so it can be reused for different
 * numeric types (coins, percentages, ratings, etc.).
 */
public class NumericInputPrompt<T extends Number> extends StringPrompt {
    private final Plugin plugin;
    private final Player player;
    private final String promptText;
    private final String invalidInputMessage;
    private final String invalidRangeMessage;
    private final Function<String, T> parser;
    private final Predicate<T> validator;
    private final Consumer<T> onAccept;

    public NumericInputPrompt(Plugin plugin,
                              Player player,
                              String promptText,
                              String invalidInputMessage,
                              String invalidRangeMessage,
                              Function<String, T> parser,
                              Predicate<T> validator,
                              Consumer<T> onAccept) {
        this.plugin = plugin;
        this.player = player;
        this.promptText = promptText;
        this.invalidInputMessage = invalidInputMessage;
        this.invalidRangeMessage = invalidRangeMessage;
        this.parser = parser;
        this.validator = validator;
        this.onAccept = onAccept;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return promptText;
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        final T value;
        try {
            value = parser.apply(input);
        } catch (Exception ex) {
            player.sendMessage(invalidInputMessage);
            return this;
        }

        if (!validator.test(value)) {
            player.sendMessage(invalidRangeMessage);
            return Prompt.END_OF_CONVERSATION;
        }

        Bukkit.getScheduler().runTask(plugin, () -> onAccept.accept(value));
        return Prompt.END_OF_CONVERSATION;
    }
}
