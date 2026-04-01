package me.nakilex.levelplugin.utils;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Lightweight .bbmodel animation/timeline_setup registry used by runtime animation fallback logic.
 */
public final class BbModelAnimationRegistry {

    public enum AnimationSourceType {
        KEYFRAMED_CLIP,
        POSE_CLIP,
        UNSUPPORTED_IMPORTED_SETUP
    }

    public record AnimationClip(String name,
                                double lengthSeconds,
                                boolean loop,
                                AnimationSourceType sourceType,
                                Map<String, String> metadata) {
    }

    public record ImportedModel(String modelId,
                                Map<String, AnimationClip> clips,
                                int explicitClipCount,
                                int timelineSetupCount,
                                int timelineConvertedToClips,
                                int timelineConvertedToPoses,
                                int timelineUnsupported) {
    }

    public record ImportedModelSummary(String modelId,
                                       int explicitClipCount,
                                       int timelineSetupCount,
                                       int timelineConvertedToClips,
                                       int timelineConvertedToPoses,
                                       int timelineUnsupported,
                                       List<String> keyframedClipNames,
                                       List<String> poseClipNames,
                                       List<String> unsupportedSetupNames) {
    }

    private static final Map<String, ImportedModel> CACHE = new ConcurrentHashMap<>();

    private BbModelAnimationRegistry() {
    }

    public static void warmup(Plugin plugin) {
        CACHE.clear();
        if (plugin == null) {
            return;
        }
        org.bukkit.plugin.Plugin modelEnginePlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("ModelEngine");
        if (modelEnginePlugin == null) {
            return;
        }
        File blueprintsDir = new File(modelEnginePlugin.getDataFolder(), "blueprints");
        if (!blueprintsDir.exists() || !blueprintsDir.isDirectory()) {
            return;
        }
        try (Stream<java.nio.file.Path> files = Files.list(blueprintsDir.toPath())) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName() != null
                            && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".bbmodel"))
                    .forEach(path -> {
                        ImportedModel imported = importSingle(path.toFile(), plugin);
                        if (imported != null) {
                            CACHE.put(imported.modelId().toLowerCase(Locale.ROOT), imported);
                            logSummary(plugin, imported);
                        }
                    });
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to warmup bbmodel animation registry: " + e.getMessage());
        }
    }

    public static List<String> getClipNames(String modelId) {
        ImportedModel model = getImportedModel(modelId);
        if (model == null || model.clips().isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(model.clips().keySet());
    }

    public static AnimationClip getClip(String modelId, String clipName) {
        ImportedModel model = getImportedModel(modelId);
        if (model == null || clipName == null || clipName.isBlank()) {
            return null;
        }
        for (Map.Entry<String, AnimationClip> entry : model.clips().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(clipName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static ImportedModelSummary getSummary(String modelId) {
        ImportedModel model = getImportedModel(modelId);
        if (model == null) {
            return null;
        }
        List<String> keyframed = new ArrayList<>();
        List<String> poses = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();
        for (AnimationClip clip : model.clips().values()) {
            if (clip == null) {
                continue;
            }
            switch (clip.sourceType()) {
                case KEYFRAMED_CLIP -> keyframed.add(clip.name());
                case POSE_CLIP -> poses.add(clip.name());
                case UNSUPPORTED_IMPORTED_SETUP -> unsupported.add(clip.name());
            }
        }
        return new ImportedModelSummary(
                model.modelId(),
                model.explicitClipCount(),
                model.timelineSetupCount(),
                model.timelineConvertedToClips(),
                model.timelineConvertedToPoses(),
                model.timelineUnsupported(),
                List.copyOf(keyframed),
                List.copyOf(poses),
                List.copyOf(unsupported)
        );
    }

    private static ImportedModel getImportedModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        return CACHE.get(modelId.toLowerCase(Locale.ROOT));
    }

    private static ImportedModel importSingle(File file, Plugin plugin) {
        String modelId = file.getName();
        if (modelId.toLowerCase(Locale.ROOT).endsWith(".bbmodel")) {
            modelId = modelId.substring(0, modelId.length() - ".bbmodel".length());
        }
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Map<String, AnimationClip> clips = new LinkedHashMap<>();

            List<String> explicit = extractNamesFromContainer(json, "animations");
            int explicitImported = 0;
            for (String name : explicit) {
                if (isPlaceholderAnimationName(name)) {
                    continue;
                }
                clips.putIfAbsent(name.toLowerCase(Locale.ROOT), new AnimationClip(
                        name,
                        0.0,
                        true,
                        AnimationSourceType.KEYFRAMED_CLIP,
                        Map.of("source", "animations")
                ));
                explicitImported++;
            }

            List<String> timelineSetups = extractNamesFromContainer(json, "timeline_setups");
            int convertedToPoses = 0;
            for (String name : timelineSetups) {
                String key = name.toLowerCase(Locale.ROOT);
                if (clips.containsKey(key)) {
                    continue;
                }
                clips.put(key, new AnimationClip(
                        name,
                        0.0,
                        false,
                        AnimationSourceType.POSE_CLIP,
                        Map.of("source", "timeline_setup", "kind", "pose")
                ));
                convertedToPoses++;
            }

            return new ImportedModel(
                    modelId,
                    Collections.unmodifiableMap(new LinkedHashMap<>(clips)),
                    explicitImported,
                    timelineSetups.size(),
                    explicitImported,
                    convertedToPoses,
                    Math.max(0, timelineSetups.size() - convertedToPoses)
            );
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to import bbmodel animations for " + modelId + ": " + e.getMessage());
            }
            return null;
        }
    }

    private static List<String> extractNamesFromContainer(String json, String rootKey) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        String payload = extractContainerPayload(json, rootKey);
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        names.addAll(extractObjectKeys(payload));
        names.addAll(extractQuotedFieldValues(payload, "name"));
        return new ArrayList<>(names);
    }

    private static String extractContainerPayload(String json, String key) {
        String needle = "\"" + key + "\"";
        int keyAt = json.indexOf(needle);
        if (keyAt < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyAt + needle.length());
        if (colon < 0) {
            return null;
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length()) {
            return null;
        }
        char open = json.charAt(start);
        if (open != '[' && open != '{') {
            return null;
        }
        char close = open == '[' ? ']' : '}';
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private static List<String> extractQuotedFieldValues(String payload, String field) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        String needle = "\"" + field + "\"";
        Set<String> values = new LinkedHashSet<>();
        int index = 0;
        while (index >= 0 && index < payload.length()) {
            int keyAt = payload.indexOf(needle, index);
            if (keyAt < 0) {
                break;
            }
            int colon = payload.indexOf(':', keyAt + needle.length());
            if (colon < 0) {
                break;
            }
            int quoteStart = payload.indexOf('"', colon + 1);
            if (quoteStart < 0) {
                break;
            }
            int quoteEnd = quoteStart + 1;
            boolean escaping = false;
            while (quoteEnd < payload.length()) {
                char c = payload.charAt(quoteEnd);
                if (escaping) {
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    break;
                }
                quoteEnd++;
            }
            if (quoteEnd < payload.length()) {
                String value = payload.substring(quoteStart + 1, quoteEnd).trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
            index = quoteEnd + 1;
        }
        return new ArrayList<>(values);
    }

    private static List<String> extractObjectKeys(String payload) {
        if (payload == null || payload.isBlank() || payload.charAt(0) != '{') {
            return List.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < payload.length(); i++) {
            char c = payload.charAt(i);
            if (inString) {
                if (escaping) {
                    current.append(c);
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    inString = false;
                    int j = i + 1;
                    while (j < payload.length() && Character.isWhitespace(payload.charAt(j))) {
                        j++;
                    }
                    if (j < payload.length() && payload.charAt(j) == ':' && depth == 1) {
                        String key = current.toString().trim();
                        if (!key.isBlank() && !"uuid".equalsIgnoreCase(key) && !"name".equalsIgnoreCase(key)) {
                            keys.add(key);
                        }
                    }
                    current.setLength(0);
                } else {
                    current.append(c);
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                current.setLength(0);
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return new ArrayList<>(keys);
    }

    private static boolean isPlaceholderAnimationName(String name) {
        if (name == null) {
            return true;
        }
        String token = name.trim();
        if (token.isBlank()) {
            return true;
        }
        if (token.chars().filter(ch -> ch == '=').count() >= 3) {
            return true;
        }
        return "pose".equalsIgnoreCase(token);
    }

    private static void logSummary(Plugin plugin, ImportedModel model) {
        if (plugin == null || model == null) {
            return;
        }
        if (model.explicitClipCount() == 0 && model.timelineSetupCount() == 0) {
            return;
        }
        plugin.getLogger().info("[Importer] Model " + model.modelId() + ": explicit animations imported="
                + model.explicitClipCount() + ", timeline setups found=" + model.timelineSetupCount()
                + ", converted clips=" + model.timelineConvertedToClips()
                + ", converted poses=" + model.timelineConvertedToPoses()
                + ", unsupported=" + model.timelineUnsupported());
    }
}
