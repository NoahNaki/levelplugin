package me.nakilex.levelplugin.player.attributes.lifeskill;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Resolves discipline metadata and progression managers in one reusable place. */
public final class LifeSkillRegistry {

    private LifeSkillRegistry() {
    }

    public static ToolDiscipline parseDiscipline(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "mining" -> ToolDiscipline.MINING;
            case "farming" -> ToolDiscipline.FARMING;
            case "fishing" -> ToolDiscipline.FISHING;
            case "woodcutting", "woodcut" -> ToolDiscipline.WOODCUTTING;
            default -> null;
        };
    }

    public static String key(ToolDiscipline discipline) {
        return discipline == null ? "" : discipline.name().toLowerCase(Locale.ROOT);
    }

    public static String displayName(ToolDiscipline discipline) {
        if (discipline == null) return "Unknown";
        String name = discipline.name().toLowerCase(Locale.ROOT);
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    public static Map<ToolDiscipline, LifeSkillProgression> progressions(Main plugin) {
        Map<ToolDiscipline, LifeSkillProgression> map = new EnumMap<>(ToolDiscipline.class);
        map.put(ToolDiscipline.MINING, plugin.getMiningManager());
        map.put(ToolDiscipline.FARMING, plugin.getFarmingManager());
        map.put(ToolDiscipline.FISHING, plugin.getFishingManager());
        map.put(ToolDiscipline.WOODCUTTING, plugin.getWoodcuttingManager());
        return map;
    }
}
