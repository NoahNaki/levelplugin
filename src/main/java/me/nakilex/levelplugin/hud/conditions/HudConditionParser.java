package me.nakilex.levelplugin.hud.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HudConditionParser {
    private static final Pattern EXPRESSION = Pattern.compile("(.+?)\\s*(==|!=|>=|<=|>|<)\\s*(.+)");

    private final Logger logger;

    public HudConditionParser(Logger logger) {
        this.logger = logger;
    }

    public List<HudCondition> parse(Object raw) {
        if (!(raw instanceof List<?> conditions)) {
            return Collections.emptyList();
        }
        List<HudCondition> parsed = new ArrayList<>();
        for (Object entry : conditions) {
            if (entry instanceof String text) {
                String normalized = text.trim().toLowerCase(Locale.ROOT);
                if (normalized.equals("dead")) {
                    parsed.add(new HudComparisonCondition("dead", "==", "true"));
                    continue;
                }
                if (normalized.equals("not_dead")) {
                    parsed.add(new HudComparisonCondition("dead", "==", "false"));
                    continue;
                }
                parseExpression(text).ifPresent(parsed::add);
            } else if (entry instanceof Map<?, ?> map) {
                Object expr = map.get("expr");
                if (expr != null) {
                    parseExpression(expr.toString()).ifPresent(parsed::add);
                    continue;
                }
                Object type = map.get("type");
                if (type != null) {
                    String normalized = type.toString().trim().toLowerCase(Locale.ROOT);
                    if (normalized.equals("dead")) {
                        parsed.add(new HudComparisonCondition("dead", "==", "true"));
                        continue;
                    }
                    if (normalized.equals("not_dead")) {
                        parsed.add(new HudComparisonCondition("dead", "==", "false"));
                        continue;
                    }
                }
                Object left = map.get("left");
                Object op = map.get("op");
                Object right = map.get("right");
                if (left != null && op != null && right != null) {
                    parsed.add(new HudComparisonCondition(left.toString(), op.toString(), right.toString()));
                }
            }
        }
        return parsed;
    }

    private java.util.Optional<HudCondition> parseExpression(String expr) {
        if (expr == null || expr.isBlank()) {
            return java.util.Optional.empty();
        }
        Matcher matcher = EXPRESSION.matcher(expr.trim());
        if (!matcher.matches()) {
            logger.warning("Invalid HUD condition expression: " + expr);
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new HudComparisonCondition(matcher.group(1), matcher.group(2), matcher.group(3)));
    }
}
