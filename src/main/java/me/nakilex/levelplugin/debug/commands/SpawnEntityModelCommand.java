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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
        for (int i = 1; i < args.length; i++) {
            String modelId = args[i];
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
            if (activeModel == null) {
                continue;
            }
            modeledEntity.addModel(activeModel, true);
            appliedModels.add(modelId);
        }

        if (appliedModels.isEmpty()) {
            entity.remove();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No valid ModelEngine models were provided.");
            return true;
        }

        modeledEntity.registerSelf();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Spawned " + type.name().toLowerCase(Locale.ROOT) + " with models: " + String.join(", ", appliedModels));
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
            return CommandUtil.filterStartingWith(entityOptions, args[0]);
        }
        if (args.length >= 2 && Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            try {
                List<String> models = getModelIds();
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

    private Object tryInvoke(Object target, String method) throws ReflectiveOperationException {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
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
}
