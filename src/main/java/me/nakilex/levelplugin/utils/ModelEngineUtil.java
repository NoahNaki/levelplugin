package me.nakilex.levelplugin.utils;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
            String resolvedId = resolveModelId(modelId, modelEngineIds);
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(resolvedId);
            if (activeModel == null) {
                activeModel = createActiveModelByReflection(resolvedId, plugin);
            }
            if (activeModel == null) {
                failedModels.add(modelId);
                if (!modelEngineIds.isEmpty()
                        && !containsIgnoreCase(modelEngineIds, resolvedId)
                        && containsIgnoreCase(blueprintIds, resolvedId)) {
                    blueprintOnlyModels.add(modelId);
                }
                continue;
            }
            var added = modeledEntity.addModel(activeModel, true);
            if (added.isEmpty()) {
                ActiveModel retryModel = ModelEngineAPI.createActiveModel(resolvedId);
                if (retryModel == null) {
                    retryModel = createActiveModelByReflection(resolvedId, plugin);
                }
                if (retryModel == null || modeledEntity.addModel(retryModel, true).isEmpty()) {
                    failedModels.add(modelId);
                    continue;
                }
            }
            appliedModels.add(modelId);
        }
        return new ModelApplyResult(appliedModels, failedModels, blueprintOnlyModels);
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
