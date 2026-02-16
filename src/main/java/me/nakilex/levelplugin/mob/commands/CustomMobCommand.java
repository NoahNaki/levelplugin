package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.mob.custom.CustomMobInstance;
import me.nakilex.levelplugin.mob.custom.gui.CustomMobAdminGUI;
import me.nakilex.levelplugin.mob.custom.spawner.CustomMobSpawner;
import me.nakilex.levelplugin.mob.custom.spawner.CustomMobSpawnerManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CustomMobCommand implements CommandExecutor, TabCompleter {

    private final CustomMobManager mobManager;
    private final CustomMobSpawnerManager spawnerManager;
    private final CustomMobAdminGUI adminGui;

    public CustomMobCommand(CustomMobManager mobManager) {
        this.mobManager = mobManager;
        this.spawnerManager = mobManager.getSpawnerManager();
        this.adminGui = mobManager.getAdminGui();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " <list|spawn|info|reload|gui|kill|killall|spawner>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                List<String> ids = mobManager.getMobIds();
                if (ids.isEmpty()) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                            "No mobs are configured.");
                } else {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Mobs: " + String.join(", ", ids));
                }
                return true;
            }
            case "reload" -> {
                mobManager.reload();
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                        "Reloaded mobs.");
                return true;
            }
            case "info" -> {
                if (args.length < 2) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Usage: /" + label + " info <mobId>");
                    return true;
                }
                String id = args[1];
                var opt = mobManager.getDefinition(id);
                if (opt.isEmpty()) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Unknown mob: " + id);
                    return true;
                }
                var def = opt.get();
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Mob: " + def.id() + " (" + def.entityType().name().toLowerCase(Locale.ROOT) + ")");
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Display: " + def.displayName());
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Level: " + def.levelRange().format() + " | Boss: " + def.boss());
                return true;
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Only players can spawn mobs.");
                    return true;
                }
                if (args.length < 2) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Usage: /" + label + " spawn <mobId> [amount]");
                    return true;
                }
                String id = args[1];
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ex) {
                        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                                "Amount must be a number.");
                        return true;
                    }
                }
                var spawned = mobManager.spawn(id, player.getLocation(), amount);
                if (spawned.isEmpty()) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Failed to spawn mob: " + id);
                    return true;
                }
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                        "Spawned " + spawned.size() + "x " + id + ".");
                return true;
            }
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Only players can open the mob GUI.");
                    return true;
                }
                adminGui.openMain(player);
                return true;
            }
            case "kill" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Only players can kill mobs.");
                    return true;
                }
                org.bukkit.entity.Entity target = player.getTargetEntity(8);
                if (!(target instanceof org.bukkit.entity.LivingEntity living)) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "Look at a mob to kill it.");
                    return true;
                }
                CustomMobInstance instance = mobManager.getInstance(living).orElse(null);
                if (instance == null) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "That entity is not a mob.");
                    return true;
                }
                living.setHealth(0);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Removed mob: " + instance.id());
                return true;
            }
            case "killall" -> {
                int removed = 0;
                for (CustomMobInstance instance : List.copyOf(mobManager.getActiveMobs().values())) {
                    if (instance != null && instance.entity() != null && !instance.entity().isDead()) {
                        instance.entity().setHealth(0);
                        removed++;
                    }
                }
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                        ChatUtil.applyEmojis("Removed " + removed + " mobs."));
                return true;
            }
            case "spawner" -> {
                return handleSpawnerCommand(sender, label, args);
            }
            default -> {
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Usage: /" + label + " <list|spawn|info|reload|gui|kill|killall|spawner>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(List.of("list", "spawn", "info", "reload", "gui", "kill", "killall", "spawner"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("info"))) {
            return CommandUtil.filterStartingWith(mobManager.getMobIds(), args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("spawner")) {
            return tabCompleteSpawner(args);
        }
        return List.of();
    }

    private boolean handleSpawnerCommand(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " spawner <create|remove|list|set|info>");
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                List<String> names = spawnerManager.getSpawnerNames();
                if (names.isEmpty()) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                            "No mob spawners are configured.");
                    return true;
                }
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Spawners: " + String.join(", ", names));
                return true;
            }
            case "move" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Only players can move spawners.");
                    return true;
                }
                if (args.length < 3) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Usage: /" + label + " spawner move <name>");
                    return true;
                }
                String name = args[2];
                if (spawnerManager.moveSpawner(name, player.getLocation())) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                            "Moved spawner '" + name + "'.");
                } else {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Failed to move spawner.");
                }
                return true;
            }
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Only players can create spawners.");
                    return true;
                }
                if (args.length < 4) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Usage: /" + label + " spawner create <name> <mobId>");
                    return true;
                }
                String name = args[2];
                String mobId = args[3];
                if (mobManager.getDefinition(mobId).isEmpty()) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Unknown mob: " + mobId);
                    return true;
                }
                boolean created = spawnerManager.createSpawner(name, mobId, player.getLocation());
                if (!created) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Spawner name already exists or is invalid.");
                    return true;
                }
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                        "Created spawner '" + name + "' for mob '" + mobId + "'.");
                return true;
            }
            case "remove" -> {
                if (args.length < 3) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Usage: /" + label + " spawner remove <name>");
                    return true;
                }
                String name = args[2];
                if (spawnerManager.removeSpawner(name)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                            "Removed spawner '" + name + "'.");
                } else {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Unknown spawner: " + name);
                }
                return true;
            }
            case "set" -> {
                if (args.length < 5) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Usage: /" + label + " spawner set <name> <flag> <value>");
                    return true;
                }
                String name = args[2];
                String flag = args[3];
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                ChatMessageUtil.MessageType result = spawnerManager.setFlag(name, flag, value);
                if (result == ChatMessageUtil.MessageType.SUCCESS) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                            "Updated spawner '" + name + "'.");
                } else {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Failed to update spawner. Check name/flag/value.");
                }
                return true;
            }
            case "info" -> {
                if (args.length < 3) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Usage: /" + label + " spawner info <name>");
                    return true;
                }
                String name = args[2];
                Optional<CustomMobSpawner> spawnerOpt = spawnerManager.getSpawner(name);
                if (spawnerOpt.isEmpty()) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Unknown spawner: " + name);
                    return true;
                }
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Spawner '" + spawnerOpt.get().getName() + "':");
                for (String line : spawnerManager.describeSpawner(spawnerOpt.get())) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, line);
                }
                return true;
            }
            default -> {
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Usage: /" + label + " spawner <create|remove|list|set|info|move>");
                return true;
            }
        }
    }

    private List<String> tabCompleteSpawner(String[] args) {
        if (args.length == 2) {
            return CommandUtil.filterStartingWith(List.of("create", "remove", "list", "set", "info", "move"), args[1]);
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("remove") || sub.equals("set") || sub.equals("info") || sub.equals("move")) {
                return CommandUtil.filterStartingWith(spawnerManager.getSpawnerNames(), args[2]);
            }
            if (sub.equals("create")) {
                return List.of();
            }
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("create")) {
            return CommandUtil.filterStartingWith(mobManager.getMobIds(), args[3]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
            return CommandUtil.filterStartingWith(spawnerManager.getFlagNames(), args[3]);
        }
        return List.of();
    }
}
