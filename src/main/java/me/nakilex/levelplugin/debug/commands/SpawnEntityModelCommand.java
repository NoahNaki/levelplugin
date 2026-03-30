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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SpawnEntityModelCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Map<UUID, UUID> lastSpawnedModelEntity = new HashMap<>();

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
        if (args.length >= 1 && args[0].equalsIgnoreCase("anim")) {
            return handlePlayAnimation(player, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("animdebug")) {
            return handleDebugAnimation(player, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("inspect")) {
            return handleInspectAnimations(player);
        }
        if (args.length < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /se <entity> <model...> | /se anim <name|shoot|attack> [loop] | /se animdebug <name> [loop] | /se inspect [verbose] | /se list");
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
        lastSpawnedModelEntity.put(player.getUniqueId(), entity.getUniqueId());
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

    private boolean handlePlayAnimation(Player player, String[] args) {
        if (args.length < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /se anim <name|shoot|attack> [loop]");
            return true;
        }
        Entity target = resolveAnimationTarget(player);
        if (target == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No modeled entity found. Look at one or spawn with /se first.");
            return true;
        }
        String animation = args[1];
        boolean loop = args.length >= 3 && Boolean.parseBoolean(args[2]);
        boolean played;
        if (animation.equalsIgnoreCase("shoot")) {
            played = ModelEngineUtil.playBestShootAnimation(target);
        } else if (animation.equalsIgnoreCase("attack")) {
            played = ModelEngineUtil.playBestAttackAnimation(target);
        } else {
            played = ModelEngineUtil.playAnimationByName(target, animation, loop);
        }
        if (!played) {
            List<String> available = ModelEngineUtil.getRuntimeAnimationNames(target);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Animation not found/played in runtime handler. Runtime available: " + (available.isEmpty() ? "(none)" : String.join(", ", available)));
            return true;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Triggered animation '" + animation + "' on entity " + target.getType().name().toLowerCase(Locale.ROOT) + ".");
        return true;
    }

    private boolean handleInspectAnimations(Player player) {
        Entity target = resolveAnimationTarget(player);
        if (target == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No modeled entity found. Look at one or spawn with /se first.");
            return true;
        }
        List<String> runtime = ModelEngineUtil.getRuntimeAnimationNames(target);
        List<String> allKnown = ModelEngineUtil.getAvailableAnimationNames(target);
        List<String> blueprintOnly = new java.util.ArrayList<>(allKnown);
        blueprintOnly.removeIf(name -> runtime.stream().anyMatch(runtimeName -> runtimeName.equalsIgnoreCase(name)));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Runtime animations on " + target.getType().name().toLowerCase(Locale.ROOT) + ": "
                        + (runtime.isEmpty() ? "(none)" : String.join(", ", runtime)));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Blueprint-only names (not directly triggerable): "
                        + (blueprintOnly.isEmpty() ? "(none)" : String.join(", ", blueprintOnly)));
        return true;
    }

    private boolean handleDebugAnimation(Player player, String[] args) {
        if (args.length < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /se animdebug <name> [loop]");
            return true;
        }
        Entity target = resolveAnimationTarget(player);
        if (target == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No modeled entity found. Look at one or spawn with /se first.");
            return true;
        }
        boolean loop = args.length >= 3 && Boolean.parseBoolean(args[2]);
        ModelEngineUtil.AnimationDebugResult result = ModelEngineUtil.debugTriggerAnimation(target, args[1], loop);
        ChatMessageUtil.send(player, result.success()
                ? ChatMessageUtil.MessageType.SUCCESS
                : ChatMessageUtil.MessageType.WARNING,
                "Anim debug " + (result.success() ? "success" : "failed") + " for '" + args[1] + "'.");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Attempted: " + (result.attempted().isEmpty() ? "(none)" : String.join(", ", result.attempted())));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Available: " + (result.available().isEmpty() ? "(none)" : String.join(", ", result.available())));
        return true;
    }

    private Entity resolveAnimationTarget(Player player) {
        Entity lookedAt = player.getTargetEntity(16);
        if (lookedAt != null && !ModelEngineUtil.getAvailableAnimationNames(lookedAt).isEmpty()) {
            return lookedAt;
        }
        UUID fallbackId = lastSpawnedModelEntity.get(player.getUniqueId());
        if (fallbackId == null || player.getWorld() == null) {
            return null;
        }
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.getUniqueId().equals(fallbackId)) {
                return entity;
            }
        }
        return null;
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
            options.add("anim");
            options.add("animdebug");
            options.add("inspect");
            return CommandUtil.filterStartingWith(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("anim")) {
            return CommandUtil.filterStartingWith(List.of("shoot", "attack", "idle", "walk"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("anim")) {
            return CommandUtil.filterStartingWith(List.of("true", "false"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("animdebug")) {
            return CommandUtil.filterStartingWith(List.of("shoot", "attack", "idle", "walk"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("animdebug")) {
            return CommandUtil.filterStartingWith(List.of("true", "false"), args[2]);
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
