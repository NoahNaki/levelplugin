package me.nakilex.levelplugin.environment.npc;

import org.bukkit.ChatColor;

import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Central mapping for kingdom Citizens NPC display names and their ModelEngine behaviour.
 *
 * Keep NPC/model definitions here instead of inside EnvironmentAreaInstanceManager so new
 * kingdom NPCs can be added without touching the building paste/restore flow.
 */
public final class KingdomNpcModelRegistry {
    private static final Map<String, NpcModelDefinition> NPC_DEFINITIONS = Map.ofEntries(
            Map.entry("blacksmith", new NpcModelDefinition("scene_blacksmith_1.bbmodel", null, true)),
            Map.entry("fisherman willy", new NpcModelDefinition("npc_fisherman.bbmodel", null, true)),
            Map.entry("fisherman", new NpcModelDefinition("scene_fishermen_boat.bbmodel", "boat", false)),
            Map.entry("fisherman 1", new NpcModelDefinition("scene_fishermen_boat.bbmodel", "boat", false)),
            Map.entry("fisherman2", new NpcModelDefinition("scene_fishermen_fishing.bbmodel", "fishing", false)),
            Map.entry("fisherman 2", new NpcModelDefinition("scene_fishermen_fishing.bbmodel", "fishing", false)),
            Map.entry("fisherman3", new NpcModelDefinition("scene_fishermen_stick.bbmodel", "stick", false)),
            Map.entry("fisherman 3", new NpcModelDefinition("scene_fishermen_stick.bbmodel", "stick", false)),
            Map.entry("fisherman4", new NpcModelDefinition("scene_fishermen_sitting.bbmodel", "sitting", false)),
            Map.entry("fisherman 4", new NpcModelDefinition("scene_fishermen_sitting.bbmodel", "sitting", false))
    );

    private KingdomNpcModelRegistry() {}

    public static String resolveModelId(String npcName) {
        NpcModelDefinition definition = resolveDefinition(npcName);
        return definition == null ? null : definition.modelId();
    }

    public static String resolveAmbientAnimation(String npcName) {
        return resolveSceneAnimation(npcName);
    }

    public static String resolveSceneAnimation(String npcName) {
        NpcModelDefinition definition = resolveDefinition(npcName);
        return definition == null ? null : definition.sceneAnimation();
    }

    public static boolean shouldUseDefaultAmbientAnimations(String npcName) {
        NpcModelDefinition definition = resolveDefinition(npcName);
        return definition == null || definition.useDefaultAmbientAnimations();
    }

    public static String debugResolution(String npcName) {
        String normalizedName = normalizeName(npcName);
        if (normalizedName.isBlank()) {
            return "raw='" + npcName + "', normalized='', matched=false";
        }

        NpcModelDefinition exactMatch = NPC_DEFINITIONS.get(normalizedName);
        if (exactMatch != null) {
            return "raw='" + npcName + "', normalized='" + normalizedName + "', matched=exact, model='"
                    + exactMatch.modelId() + "', scene='" + exactMatch.sceneAnimation()
                    + "', defaultAmbient=" + exactMatch.useDefaultAmbientAnimations();
        }

        for (Map.Entry<String, NpcModelDefinition> entry : NPC_DEFINITIONS.entrySet()) {
            if (normalizedName.contains(entry.getKey())) {
                NpcModelDefinition definition = entry.getValue();
                return "raw='" + npcName + "', normalized='" + normalizedName + "', matched=contains:'"
                        + entry.getKey() + "', model='" + definition.modelId() + "', scene='"
                        + definition.sceneAnimation() + "', defaultAmbient="
                        + definition.useDefaultAmbientAnimations();
            }
        }

        return "raw='" + npcName + "', normalized='" + normalizedName + "', matched=false, known=["
                + knownNames() + "]";
    }

    private static String knownNames() {
        StringJoiner joiner = new StringJoiner(", ");
        for (String name : NPC_DEFINITIONS.keySet()) {
            joiner.add(name);
        }
        return joiner.toString();
    }

    private static NpcModelDefinition resolveDefinition(String npcName) {
        String normalizedName = normalizeName(npcName);
        if (normalizedName.isBlank()) {
            return null;
        }

        NpcModelDefinition exactMatch = NPC_DEFINITIONS.get(normalizedName);
        if (exactMatch != null) {
            return exactMatch;
        }

        for (Map.Entry<String, NpcModelDefinition> entry : NPC_DEFINITIONS.entrySet()) {
            if (normalizedName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static String normalizeName(String npcName) {
        if (npcName == null) {
            return "";
        }
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', npcName));
        if (stripped == null) {
            return "";
        }
        return stripped.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll(" +", " ");
    }

    private record NpcModelDefinition(String modelId, String sceneAnimation, boolean useDefaultAmbientAnimations) {}
}
