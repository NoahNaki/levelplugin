package me.nakilex.levelplugin.debug.commands;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class SpawnEntityModelCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public SpawnEntityModelCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "ModelEngine is not enabled on this server.");
                return true;
            }
            List<String> modelEngineIds = getModelIdsSafely();
            List<String> blueprintIds = getBlueprintModelIds();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "ModelEngine models (" + modelEngineIds.size() + "):");
            if (modelEngineIds.isEmpty()) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "No models are loaded by ModelEngine.");
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        String.join(", ", modelEngineIds));
            }
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Blueprint files (" + blueprintIds.size() + "):");
            if (blueprintIds.isEmpty()) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "No .bbmodel files found in the blueprints folder.");
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        String.join(", ", blueprintIds));
            }
            return true;
        }
        if (args.length < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /se <entity> <model...>");
            return true;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "ModelEngine is not enabled on this server.");
            return true;
        }
        EntityType type = parseEntityType(args[0]);
        if (type == null || !type.isSpawnable() || !type.isAlive()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Unknown or unsupported entity type.");
            return true;
        }
        Location spawnLocation = player.getLocation();
        Entity entity = player.getWorld().spawnEntity(spawnLocation, type);
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(entity);
        if (modeledEntity == null) {
            entity.remove();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Failed to create a modeled entity.");
            return true;
        }

        List<String> appliedModels = new ArrayList<>();
        List<String> failedModels = new ArrayList<>();
        List<String> blueprintOnlyModels = new ArrayList<>();
        List<String> modelEngineIds = getModelIdsSafely();
        List<String> blueprintIds = getBlueprintModelIds();
        modeledEntity.registerSelf();
        for (int i = 1; i < args.length; i++) {
            String modelId = args[i];
            String resolvedId = resolveModelId(modelId, modelEngineIds);
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(resolvedId);
            if (activeModel == null) {
                activeModel = createActiveModelByReflection(resolvedId);
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
                failedModels.add(modelId);
                continue;
            }
            appliedModels.add(modelId);
        }

        if (appliedModels.isEmpty()) {
            modeledEntity.destroy();
            entity.remove();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "No valid ModelEngine models were applied.");
            if (!blueprintOnlyModels.isEmpty()) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Blueprints are not loaded as models: " + String.join(", ", blueprintOnlyModels)
                                + ". Import them in ModelEngine to create actual model IDs.");
            }
            if (!failedModels.isEmpty()) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Failed to resolve: " + String.join(", ", failedModels)
                                + ". Make sure models are loaded by ModelEngine (not just blueprints).");
            }
            return true;
        }

        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Spawned " + type.name().toLowerCase(Locale.ROOT) + " with models: " + String.join(", ", appliedModels));
        if (!blueprintOnlyModels.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Blueprints not loaded as models: " + String.join(", ", blueprintOnlyModels)
                            + ". Import them in ModelEngine to create actual model IDs.");
        }
        if (!failedModels.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Some models failed to apply: " + String.join(", ", failedModels)
                            + ". Check ModelEngine logs for load errors.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> entityOptions = Arrays.stream(EntityType.values())
                    .filter(EntityType::isSpawnable)
                    .filter(EntityType::isAlive)
                    .map(type -> type.name().toLowerCase(Locale.ROOT))
                    .toList();
            List<String> options = new ArrayList<>(entityOptions);
            options.add("list");
            return CommandUtil.filterStartingWith(options, args[0]);
        }
        if (args.length >= 2 && Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            try {
                List<String> models = getModelIds();
                models.addAll(getBlueprintModelIds());
                if (!models.isEmpty()) {
                    return CommandUtil.filterStartingWith(models, args[args.length - 1]);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to fetch ModelEngine models: " + e.getMessage());
            }
        }
        return List.of();
    }

    private EntityType parseEntityType(String token) {
        try {
            return EntityType.valueOf(token.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<String> getModelIds() throws ReflectiveOperationException {
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

    private List<String> getModelIdsSafely() {
        try {
            return getModelIds();
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Failed to fetch ModelEngine model ids: " + e.getMessage());
            return List.of();
        }
    }

    private List<String> getBlueprintModelIds() {
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
            plugin.getLogger().warning("Failed to read ModelEngine blueprints: " + e.getMessage());
        }
        return new ArrayList<>(ids);
    }

    private List<String> extractModelIds(Object source) throws ReflectiveOperationException {
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

    private ActiveModel createActiveModelByReflection(String modelId) {
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
            plugin.getLogger().warning("Failed to create ModelEngine model '" + modelId + "': " + e.getMessage());
        }
        return null;
    }

    private Object tryInvoke(Object target, String method) throws ReflectiveOperationException {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private Object tryInvoke(Object target, String method, Object... args) throws ReflectiveOperationException {
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

    private ActiveModel coerceActiveModel(Object result) {
        if (result instanceof ActiveModel activeModel) {
            return activeModel;
        }
        if (result instanceof java.util.Optional<?> optional && optional.isPresent()
                && optional.get() instanceof ActiveModel activeModel) {
            return activeModel;
        }
        return null;
    }

    private List<String> coerceModelIds(Object result) {
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

    private String resolveModelId(String modelId, List<String> modelEngineIds) {
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

    private boolean containsIgnoreCase(List<String> values, String token) {
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
