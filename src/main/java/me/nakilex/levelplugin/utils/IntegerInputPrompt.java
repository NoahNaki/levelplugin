package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

/**
 * Chat prompt specialized for integer entry with common validation helpers
 * so individual callers don't need to reimplement parsing and range checks.
 */
public class IntegerInputPrompt extends NumericInputPrompt<Integer> {

    private static final String DEFAULT_INVALID_INT = "Invalid input! Please enter a valid number.";
    private static final String DEFAULT_OUT_OF_RANGE = "You do not have enough coins for that.";

    public IntegerInputPrompt(Plugin plugin,
                              Player player,
                              String promptText,
                              String invalidInputMessage,
                              String invalidRangeMessage,
                              IntPredicate validator,
                              IntConsumer onAccept) {
        super(
                plugin,
                player,
                promptText,
                ChatColor.RED + invalidInputMessage,
                ChatColor.RED + invalidRangeMessage,
                IntegerInputPrompt::parseInteger,
                validator::test,
                onAccept::accept
        );
    }

    public static IntegerInputPrompt coinAmountWithinBalance(Plugin plugin,
                                                             Player player,
                                                             String promptText,
                                                             IntSupplier maxSupplier,
                                                             boolean requirePositive,
                                                             IntConsumer onAccept) {
        return nonNegativeWithMax(
                plugin,
                player,
                promptText,
                DEFAULT_INVALID_INT,
                DEFAULT_OUT_OF_RANGE,
                maxSupplier,
                requirePositive ? amount -> amount > 0 : null,
                onAccept
        );
    }

    /**
     * Builds a prompt that requires a non-negative amount not exceeding a
     * supplied cap. An optional extra validator can further constrain the
     * value (for example, enforcing greater-than-zero).
     */
    public static IntegerInputPrompt nonNegativeWithMax(Plugin plugin,
                                                        Player player,
                                                        String promptText,
                                                        String invalidInputMessage,
                                                        String invalidRangeMessage,
                                                        IntSupplier maxSupplier,
                                                        IntPredicate extraValidator,
                                                        IntConsumer onAccept) {
        Objects.requireNonNull(maxSupplier, "maxSupplier");

        IntPredicate baseValidator = amount -> amount >= 0 && amount <= maxSupplier.getAsInt();
        IntPredicate combined = extraValidator == null ? baseValidator : baseValidator.and(extraValidator);

        return new IntegerInputPrompt(
                plugin,
                player,
                promptText,
                invalidInputMessage,
                invalidRangeMessage,
                combined,
                onAccept
        );
    }

    public static IntegerInputPrompt nonNegativeWithMax(Plugin plugin,
                                                        Player player,
                                                        String promptText,
                                                        String invalidInputMessage,
                                                        String invalidRangeMessage,
                                                        IntSupplier maxSupplier,
                                                        IntConsumer onAccept) {
        return nonNegativeWithMax(plugin, player, promptText, invalidInputMessage, invalidRangeMessage, maxSupplier, null, onAccept);
    }

    private static Integer parseInteger(String input) {
        if (!input.matches("\\d+")) {
            throw new NumberFormatException("Not an integer");
        }
        return Integer.parseInt(input);
    }
}
