package me.nakilex.levelplugin.utils;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.Collections;

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

    public record AnimationDebugResult(boolean success,
                                       List<String> attempted,
                                       List<String> available) {
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
                    scheduleLoopAnimationStart(modeledEntity, retryModel, plugin);
                } else {
                    scheduleLoopAnimationStart(modeledEntity, activeModel, plugin);
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

    public static boolean playBestShootAnimation(Entity entity) {
        return playBestAnimation(entity, List.of("shoot", "arrow", "bow", "cast", "attack"), false);
    }

    public static boolean playBestAttackAnimation(Entity entity) {
        return playBestAnimation(entity, List.of("attack", "slash", "swing", "hit"), false);
    }

    public static boolean playBestAnimation(Entity entity, List<String> keywords, boolean loop) {
        if (entity == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null || modeledEntity.getModels().isEmpty()) {
            return false;
        }
        for (ActiveModel model : modeledEntity.getModels().values()) {
            AnimationHandler handler = model.getAnimationHandler();
            if (handler == null) {
                continue;
            }
            handler.prepare();
            String match = selectAnimationByKeywords(handler.getAnimations().keySet(), keywords);
            if (match == null || match.isBlank()) {
                continue;
            }
            if (!handler.isPlayingAnimation(match)) {
                handler.playAnimation(match, 0.0, 0.0, 1.0, loop);
            }
            return true;
        }
        return false;
    }

    public static boolean playAnimationByName(Entity entity, String animationName, boolean loop) {
        if (entity == null || animationName == null || animationName.isBlank()) {
            return false;
        }
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null || modeledEntity.getModels().isEmpty()) {
            return false;
        }
        for (ActiveModel model : modeledEntity.getModels().values()) {
            AnimationHandler handler = model.getAnimationHandler();
            if (handler == null) {
                continue;
            }
            handler.prepare();
            String match = resolveAnimationName(model, handler, animationName);
            if (match == null || match.isBlank()) {
                continue;
            }
            handler.playAnimation(match, 0.0, 0.0, 1.0, loop);
            return true;
        }
        return false;
    }

    public static List<String> getAvailableAnimationNames(Entity entity) {
        if (entity == null) {
            return List.of();
        }
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null || modeledEntity.getModels().isEmpty()) {
            return List.of();
        }
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ActiveModel model : modeledEntity.getModels().values()) {
            AnimationHandler handler = model.getAnimationHandler();
            if (handler == null) {
                continue;
            }
            handler.prepare();
            names.addAll(handler.getAnimations().keySet());
            if (model.getBlueprint() != null && model.getBlueprint().getAnimations() != null) {
                names.addAll(model.getBlueprint().getAnimations().keySet());
            }
            if (model.getBlueprint() != null && model.getBlueprint().getAnimationsPlaceholders() != null) {
                names.addAll(model.getBlueprint().getAnimationsPlaceholders().keySet());
                names.addAll(model.getBlueprint().getAnimationsPlaceholders().values());
            }
        }
        if (names.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(names));
    }

    public static AnimationDebugResult debugTriggerAnimation(Entity entity, String requested, boolean loop) {
        if (entity == null || requested == null || requested.isBlank()) {
            return new AnimationDebugResult(false, List.of("invalid_input"), List.of());
        }
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null || modeledEntity.getModels().isEmpty()) {
            return new AnimationDebugResult(false, List.of("no_modeled_entity"), List.of());
        }
        List<String> attempted = new ArrayList<>();
        List<String> available = getAvailableAnimationNames(entity);
        for (Map.Entry<String, ActiveModel> entry : modeledEntity.getModels().entrySet()) {
            String modelKey = entry.getKey();
            ActiveModel model = entry.getValue();
            AnimationHandler handler = model.getAnimationHandler();
            if (handler == null) {
                attempted.add(modelKey + ":no_handler");
                continue;
            }
            handler.prepare();
            LinkedHashSet<String> candidates = new LinkedHashSet<>();
            candidates.add(requested);
            String resolved = resolveAnimationName(model, handler, requested);
            if (resolved != null) {
                candidates.add(resolved);
            }
            String keywordMatch = selectAnimationByKeywords(available, List.of(requested));
            if (keywordMatch != null) {
                candidates.add(keywordMatch);
            }
            for (String candidate : candidates) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }
                try {
                    if (handler.getAnimations().containsKey(candidate)) {
                        handler.playAnimation(candidate, 0.0, 0.0, 1.0, loop);
                        attempted.add(modelKey + ":" + candidate + ":played");
                        return new AnimationDebugResult(true, attempted, available);
                    }
                    var property = handler.getAnimation(candidate);
                    if (property != null && handler.playAnimation(property, true)) {
                        attempted.add(modelKey + ":" + candidate + ":played_property");
                        return new AnimationDebugResult(true, attempted, available);
                    }
                    attempted.add(modelKey + ":" + candidate + ":no_match");
                } catch (Exception ex) {
                    attempted.add(modelKey + ":" + candidate + ":error(" + ex.getClass().getSimpleName() + ")");
                }
            }
        }
        return new AnimationDebugResult(false, attempted, available);
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
        ActiveModel fromBlueprint = createActiveModelFromBlueprint(modelId, plugin);
        if (fromBlueprint != null) {
            return fromBlueprint;
        }
        return createActiveModelByReflection(modelId, plugin);
    }

    private static void tryStartLoopAnimation(ActiveModel model, Plugin plugin) {
        if (model == null) {
            return;
        }
        try {
            model.initializeRenderer();
            model.generateModel();
            AnimationHandler handler = model.getAnimationHandler();
            if (handler == null) {
                return;
            }
            handler.prepare();
            configureDefaultStateAnimations(model, handler);
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

    private static void scheduleLoopAnimationStart(ModeledEntity modeledEntity, ActiveModel model, Plugin plugin) {
        if (modeledEntity == null || model == null) {
            return;
        }
        Runnable starter = () -> tryStartLoopAnimation(model, plugin);
        try {
            modeledEntity.queuePostInitTask(starter);
        } catch (Exception ignored) {
        }
        starter.run();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, starter, 1L);
            Bukkit.getScheduler().runTaskLater(plugin, starter, 20L);
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

    private static void configureDefaultStateAnimations(ActiveModel model, AnimationHandler handler) {
        if (model == null || handler == null || handler.getAnimations() == null || handler.getAnimations().isEmpty()) {
            return;
        }
        setDefaultStateAnimation(model, handler, ModelState.IDLE, List.of("idle", "loop", "stand"));
        setDefaultStateAnimation(model, handler, ModelState.WALK, List.of("walk", "run", "move"));
        setDefaultStateAnimation(model, handler, ModelState.STRAFE, List.of("strafe", "walk"));
        setDefaultStateAnimation(model, handler, ModelState.JUMP, List.of("jump"));
        setDefaultStateAnimation(model, handler, ModelState.SPAWN, List.of("spawn", "summon", "appear"));
        setDefaultStateAnimation(model, handler, ModelState.DEATH, List.of("death", "die"));
    }

    private static void setDefaultStateAnimation(ActiveModel model,
                                                 AnimationHandler handler,
                                                 ModelState state,
                                                 List<String> keywords) {
        if (state == null || keywords == null || keywords.isEmpty()) {
            return;
        }
        String animation = selectAnimationByKeywords(handler.getAnimations().keySet(), keywords);
        if (animation == null || animation.isBlank()) {
            return;
        }
        AnimationHandler.DefaultProperty current = handler.getDefaultProperty(state);
        if (current != null && current.getAnimation() != null && !current.getAnimation().isBlank()) {
            return;
        }
        handler.setDefaultProperty(new AnimationHandler.DefaultProperty(state, animation, 0.0, 0.0, 1.0));
    }

    private static String selectAnimationByKeywords(Collection<String> animationNames, List<String> keywords) {
        if (animationNames == null || animationNames.isEmpty()) {
            return null;
        }
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            for (String animation : animationNames) {
                if (animation == null) {
                    continue;
                }
                if (animation.equalsIgnoreCase(keyword)) {
                    return animation;
                }
            }
        }
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
            for (String animation : animationNames) {
                if (animation == null) {
                    continue;
                }
                if (animation.toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
                    return animation;
                }
            }
        }
        return null;
    }

    private static String resolveAnimationName(ActiveModel model,
                                               AnimationHandler handler,
                                               String requested) {
        if (handler == null || requested == null || requested.isBlank()) {
            return null;
        }
        String fromHandler = selectAnimationByKeywords(handler.getAnimations().keySet(), List.of(requested));
        if (fromHandler != null) {
            return fromHandler;
        }
        if (model == null || model.getBlueprint() == null) {
            return null;
        }
        ModelBlueprint blueprint = model.getBlueprint();
        if (blueprint.getAnimationsPlaceholders() != null) {
            String mapped = blueprint.getAnimationsPlaceholders().get(requested);
            if (mapped != null && handler.getAnimations().containsKey(mapped)) {
                return mapped;
            }
            String placeholderKey = selectAnimationByKeywords(blueprint.getAnimationsPlaceholders().keySet(), List.of(requested));
            if (placeholderKey != null) {
                String placeholderValue = blueprint.getAnimationsPlaceholders().get(placeholderKey);
                if (placeholderValue != null && handler.getAnimations().containsKey(placeholderValue)) {
                    return placeholderValue;
                }
            }
        }
        if (blueprint.getAnimations() != null) {
            String fromBlueprint = selectAnimationByKeywords(blueprint.getAnimations().keySet(), List.of(requested));
            if (fromBlueprint != null && handler.getAnimations().containsKey(fromBlueprint)) {
                return fromBlueprint;
            }
        }
        return null;
    }

    private static ActiveModel createActiveModelFromBlueprint(String modelId, Plugin plugin) {
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        try {
            ModelBlueprint blueprint = ModelEngineAPI.getBlueprint(modelId);
            if (blueprint == null) {
                return null;
            }
            return ModelEngineAPI.createActiveModel(blueprint);
        } catch (RuntimeException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to create ModelEngine blueprint model '" + modelId + "': " + e.getMessage());
            }
            return null;
        }
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
