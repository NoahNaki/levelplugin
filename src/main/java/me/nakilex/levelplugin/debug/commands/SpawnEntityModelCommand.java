package me.nakilex.levelplugin.debug.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
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
        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "ModelEngine is not enabled on this server.");
                return true;
            }
            List<String> modelEngineIds = ModelEngineUtil.getModelIdsSafely(plugin);
            List<String> blueprintIds = ModelEngineUtil.getBlueprintModelIds(plugin);
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
        List<String> models = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            models.add(args[i]);
        }
        ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyModels(entity, models, plugin);
        List<String> appliedModels = result.applied();
        List<String> failedModels = result.failed();
        List<String> blueprintOnlyModels = result.blueprintOnly();

        if (appliedModels.isEmpty()) {
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
                List<String> models = ModelEngineUtil.getModelIdsSafely(plugin);
                models.addAll(ModelEngineUtil.getBlueprintModelIds(plugin));
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

}
