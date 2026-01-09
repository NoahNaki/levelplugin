package me.nakilex.levelplugin.hud.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class HudConditionParser {
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
            if (entry instanceof Map<?, ?> map) {
                Object typeRaw = map.get("type");
                if (typeRaw == null) {
                    continue;
                }
                String typeName = typeRaw.toString().toUpperCase(Locale.ROOT);
                HudConditionType type;
                try {
                    type = HudConditionType.valueOf(typeName);
                } catch (IllegalArgumentException ex) {
                    logger.warning("Unknown HUD condition type: " + typeName);
                    continue;
                }
                parsed.add(type::matches);
            } else if (entry instanceof String name) {
                String typeName = name.toUpperCase(Locale.ROOT);
                try {
                    HudConditionType type = HudConditionType.valueOf(typeName);
                    parsed.add(type::matches);
                } catch (IllegalArgumentException ex) {
                    logger.warning("Unknown HUD condition type: " + typeName);
                }
            }
        }
        return parsed;
    }
}
