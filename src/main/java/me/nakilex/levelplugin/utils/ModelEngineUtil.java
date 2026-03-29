package me.nakilex.levelplugin.utils;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Shared helper for ModelEngine model resolution and application.
 */
public final class ModelEngineUtil {

    private ModelEngineUtil() {
    }

    public record ModelApplyResult(List<String> applied,
                                   List<String> failed,
                                   List<String> blueprintOnly) {
    }

    public static List<String> getModelIdsSafely(Plugin plugin) {
        try {
            return getModelIds();
        } catch (ReflectiveOperationException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to fetch ModelEngine model ids: " + e.getMessage());
            }
            return List.of();
        }
    }

    public static List<String> getBlueprintModelIds(Plugin plugin) {
        var modelEnginePlugin = Bukkit.getPluginManager().getPlugin("ModelEngine");
        if (modelEnginePlugin == null) {
            return List.of();
        }
        File blueprintsDir = new File(modelEnginePlugin.getDataFolder(), "blueprints");
        if (!blueprintsDir.exists() || !blueprintsDir.isDirectory()) {
            return List.of();
        }
        Set<String> ids = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Stream<java.nio.file.Path> paths = java.nio.file.Files.list(blueprintsDir.toPath())) {
            paths.filter(path -> path.getFileName() != null)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".bbmodel"))
                    .map(name -> name.substring(0, name.length() - ".bbmodel".length()))
                    .filter(name -> !name.isBlank())
                    .forEach(ids::add);
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to read ModelEngine blueprints: " + e.getMessage());
            }
        }
        return new ArrayList<>(ids);
    }

    public static ModelApplyResult applyModels(Entity entity,
                                               List<String> modelIds,
                                               Plugin plugin) {
        if (entity == null || modelIds == null || modelIds.isEmpty()) {
            return new ModelApplyResult(List.of(), List.of(), List.of());
        }
        List<String> modelEngineIds = getModelIdsSafely(plugin);
        List<String> blueprintIds = getBlueprintModelIds(plugin);

        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(entity);
        if (modeledEntity == null) {
            return new ModelApplyResult(List.of(), List.copyOf(modelIds), List.of());
        }
        modeledEntity.setBaseEntityVisible(false);
        modeledEntity.registerSelf();

        List<String> appliedModels = new ArrayList<>();
        List<String> failedModels = new ArrayList<>();
        List<String> blueprintOnlyModels = new ArrayList<>();
        for (String modelId : modelIds) {
            boolean applied = false;
            boolean blueprintOnly = false;
            for (String candidate : buildModelCandidates(modelId)) {
                String resolvedId = resolveModelId(candidate, modelEngineIds);
                ActiveModel activeModel = createActiveModelSafely(resolvedId, plugin);
                if (activeModel == null) {
                    if (!modelEngineIds.isEmpty()
                            && !containsIgnoreCase(modelEngineIds, resolvedId)
                            && containsIgnoreCase(blueprintIds, resolvedId)) {
                        blueprintOnly = true;
                    }
                    continue;
                }
                var added = modeledEntity.addModel(activeModel, true);
                if (added.isEmpty()) {
                    ActiveModel retryModel = createActiveModelSafely(resolvedId, plugin);
                    if (retryModel == null || modeledEntity.addModel(retryModel, true).isEmpty()) {
                        continue;
                    }
                    tryStartLoopAnimation(retryModel, plugin);
                } else {
                    tryStartLoopAnimation(activeModel, plugin);
                }
                applied = true;
                break;
            }
            if (applied) {
                appliedModels.add(modelId);
            } else {
                failedModels.add(modelId);
                if (blueprintOnly) {
                    blueprintOnlyModels.add(modelId);
                }
            }
        }
        return new ModelApplyResult(appliedModels, failedModels, blueprintOnlyModels);
    }

    public static ModelApplyResult applyFirstAvailableModel(Entity entity,
                                                             List<String> modelCandidates,
                                                             Plugin plugin) {
        if (entity == null || modelCandidates == null || modelCandidates.isEmpty()) {
            return new ModelApplyResult(List.of(), List.of(), List.of());
        }
        for (String candidate : modelCandidates) {
            ModelApplyResult result = applyModels(entity, List.of(candidate), plugin);
            if (!result.applied().isEmpty()) {
                return result;
            }
        }
        return applyModels(entity, modelCandidates, plugin);
    }

    public static List<String> buildModelCandidates(String token) {
        if (token == null) {
            return List.of();
        }
        String trimmed = token.trim();
        if (trimmed.isBlank()) {
            return List.of();
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        String base = lower.endsWith(".bbmodel")
                ? trimmed.substring(0, trimmed.length() - ".bbmodel".length())
                : trimmed;
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(trimmed);
        candidates.add(base);
        candidates.add(base + ".bbmodel");
        candidates.add(base + "_bbmodel");
        return new ArrayList<>(candidates);
    }

    /**
     * Updates a location's yaw/pitch so it faces the provided direction vector.
     */
    public static void orientLocationToVector(Location location, Vector direction) {
        if (location == null || direction == null || direction.lengthSquared() <= 0.000001) {
            return;
        }
        Vector normalized = direction.clone().normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-normalized.getX(), normalized.getZ()));
        float pitch = (float) Math.toDegrees(-Math.asin(normalized.getY()));
        location.setYaw(yaw);
        location.setPitch(pitch);
    }

    /**
     * Reorients and teleports an entity so attached models face the given vector.
     */
    public static void orientEntityToVector(Entity entity, Vector direction) {
        if (entity == null) {
            return;
        }
        Location oriented = entity.getLocation().clone();
        orientLocationToVector(oriented, direction);
        entity.teleport(oriented);
    }

    private static ActiveModel createActiveModelSafely(String modelId, Plugin plugin) {
        try {
            ActiveModel model = ModelEngineAPI.createActiveModel(modelId);
            if (model != null) {
                return model;
            }
        } catch (RuntimeException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to create ModelEngine model '" + modelId + "': " + e.getMessage());
            }
        }
        return createActiveModelByReflection(modelId, plugin);
    }

    private static void tryStartLoopAnimation(ActiveModel model, Plugin plugin) {
        if (model == null) {
            return;
        }
        try {
            AnimationHandler handler = model.getAnimationHandler();
            if (handler == null) {
                return;
            }
            String selected = selectLoopAnimation(handler.getAnimations());
            if (selected == null || selected.isBlank()) {
                return;
            }
            if (!handler.isPlayingAnimation(selected)) {
                handler.playAnimation(selected, 0.0, 0.0, 1.0, true);
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().fine("Model animation auto-play skipped: " + e.getMessage());
            }
        }
    }

    private static String selectLoopAnimation(Map<String, ?> animations) {
        if (animations == null || animations.isEmpty()) {
            return null;
        }
        for (String key : animations.keySet()) {
            if (key != null && key.equalsIgnoreCase("idle")) {
                return key;
            }
        }
        for (String key : animations.keySet()) {
            if (key == null) {
                continue;
            }
            String lower = key.toLowerCase(Locale.ROOT);
            if (lower.contains("idle") || lower.contains("walk") || lower.contains("loop")) {
                return key;
            }
        }
        return animations.keySet().iterator().next();
    }

    private static List<String> getModelIds() throws ReflectiveOperationException {
        Object api = ModelEngineAPI.getAPI();
        List<String> values = extractModelIds(api);
        if (!values.isEmpty()) {
            return values;
        }
        for (String accessor : List.of("getModelManager", "getModelRegistry", "getModelService")) {
            Object manager = tryInvoke(api, accessor);
            if (manager == null) {
                continue;
            }
            values = extractModelIds(manager);
            if (!values.isEmpty()) {
                return values;
            }
        }
        return values;
    }

    private static List<String> extractModelIds(Object source) throws ReflectiveOperationException {
        if (source == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String accessor : List.of("getModelIds", "getModels", "getRegisteredModels")) {
            Object result = tryInvoke(source, accessor);
            values = coerceModelIds(result);
            if (!values.isEmpty()) {
                return values;
            }
        }
        values = coerceModelIds(source);
        return values;
    }

    private static ActiveModel createActiveModelByReflection(String modelId, Plugin plugin) {
        try {
            Object api = ModelEngineAPI.getAPI();
            ActiveModel activeModel = coerceActiveModel(tryInvoke(api, "createActiveModel", modelId));
            if (activeModel != null) {
                return activeModel;
            }
            for (String accessor : List.of("getModelManager", "getModelRegistry", "getModelService")) {
                Object manager = tryInvoke(api, accessor);
                activeModel = coerceActiveModel(tryInvoke(manager, "createActiveModel", modelId));
                if (activeModel != null) {
                    return activeModel;
                }
                Object model = tryInvoke(manager, "getModel", modelId);
                activeModel = coerceActiveModel(tryInvoke(model, "createActiveModel"));
                if (activeModel != null) {
                    return activeModel;
                }
            }
        } catch (ReflectiveOperationException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to create ModelEngine model '" + modelId + "': " + e.getMessage());
            }
        }
        return null;
    }

    private static Object tryInvoke(Object target, String method) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object tryInvoke(Object target, String method, Object... args) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Class<?>[] types = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
        try {
            return target.getClass().getMethod(method, types).invoke(target, args);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static ActiveModel coerceActiveModel(Object result) {
        if (result instanceof ActiveModel activeModel) {
            return activeModel;
        }
        if (result instanceof java.util.Optional<?> optional && optional.isPresent()
                && optional.get() instanceof ActiveModel activeModel) {
            return activeModel;
        }
        return null;
    }

    private static List<String> coerceModelIds(Object result) {
        if (result == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        if (result instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                if (entry != null) {
                    values.add(entry.toString());
                }
            }
            return values;
        }
        if (result instanceof java.util.Map<?, ?> map) {
            for (Object entry : map.keySet()) {
                if (entry != null) {
                    values.add(entry.toString());
                }
            }
        }
        return values;
    }

    private static String resolveModelId(String modelId, List<String> modelEngineIds) {
        if (modelId == null || modelEngineIds == null) {
            return modelId;
        }
        for (String candidate : modelEngineIds) {
            if (candidate != null && candidate.equalsIgnoreCase(modelId)) {
                return candidate;
            }
        }
        return modelId;
    }

    private static boolean containsIgnoreCase(List<String> values, String token) {
        if (values == null || token == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }
}
