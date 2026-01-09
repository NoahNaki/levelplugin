package me.nakilex.levelplugin.hud.conditions;

import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderService;
import org.bukkit.entity.Player;

public class HudComparisonCondition implements HudCondition {
    private final String left;
    private final String operator;
    private final String right;

    public HudComparisonCondition(String left, String operator, String right) {
        this.left = left == null ? "" : left.trim();
        this.operator = operator == null ? "==" : operator.trim();
        this.right = right == null ? "" : right.trim();
    }

    @Override
    public boolean matches(Player player, HudConditionContext context) {
        HudPlaceholderService placeholders = context == null ? null : context.getPlaceholderService();
        String leftValue = resolveOperand(player, placeholders, left);
        String rightValue = resolveOperand(player, placeholders, right);
        Double leftNumber = toNumber(leftValue);
        Double rightNumber = toNumber(rightValue);
        if (leftNumber != null && rightNumber != null) {
            return compareNumbers(leftNumber, rightNumber);
        }
        return compareStrings(leftValue, rightValue);
    }

    @Override
    public String describe(Player player, HudConditionContext context) {
        HudPlaceholderService placeholders = context == null ? null : context.getPlaceholderService();
        String leftValue = resolveOperand(player, placeholders, left);
        String rightValue = resolveOperand(player, placeholders, right);
        return left + " " + operator + " " + right + " (resolved: " + leftValue + " " + operator + " " + rightValue + ")";
    }

    private String resolveOperand(Player player, HudPlaceholderService placeholders, String raw) {
        if (raw.equalsIgnoreCase("dead")) {
            return String.valueOf(player != null && (player.isDead() || player.getHealth() <= 0.0));
        }
        if (raw.equalsIgnoreCase("underwater")) {
            return String.valueOf(player != null && player.isInWater());
        }
        if (raw.startsWith("papi:")) {
            String token = "%" + raw.substring(5) + "%";
            return placeholders == null ? "" : placeholders.resolve(player, token);
        }
        if (raw.startsWith("'") && raw.endsWith("'") && raw.length() > 1) {
            return raw.substring(1, raw.length() - 1);
        }
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private Double toNumber(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean compareNumbers(double leftValue, double rightValue) {
        return switch (operator) {
            case "==" -> leftValue == rightValue;
            case "!=" -> leftValue != rightValue;
            case ">" -> leftValue > rightValue;
            case "<" -> leftValue < rightValue;
            case ">=" -> leftValue >= rightValue;
            case "<=" -> leftValue <= rightValue;
            default -> false;
        };
    }

    private boolean compareStrings(String leftValue, String rightValue) {
        String leftText = leftValue == null ? "" : leftValue;
        String rightText = rightValue == null ? "" : rightValue;
        return switch (operator) {
            case "==" -> leftText.equalsIgnoreCase(rightText);
            case "!=" -> !leftText.equalsIgnoreCase(rightText);
            default -> false;
        };
    }
}
