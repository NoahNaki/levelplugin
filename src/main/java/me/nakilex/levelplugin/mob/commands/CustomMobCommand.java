package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CustomMobCommand implements CommandExecutor, TabCompleter {

    private final CustomMobManager mobManager;

    public CustomMobCommand(CustomMobManager mobManager) {
        this.mobManager = mobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " <list|spawn|info|reload>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                List<String> ids = mobManager.getMobIds();
                if (ids.isEmpty()) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                            "No custom mobs are configured.");
                } else {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                            "Custom mobs: " + String.join(", ", ids));
                }
                return true;
            }
            case "reload" -> {
                mobManager.reload();
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                        "Reloaded custom mobs.");
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
                            "Unknown custom mob: " + id);
                    return true;
                }
                var def = opt.get();
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Mob: " + def.id() + " (" + def.entityType().name().toLowerCase(Locale.ROOT) + ")");
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Display: " + def.displayName());
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Level: " + def.level() + " | Boss: " + def.boss());
                return true;
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Only players can spawn custom mobs.");
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
                            "Failed to spawn custom mob: " + id);
                    return true;
                }
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                        "Spawned " + spawned.size() + "x " + id + ".");
                return true;
            }
            default -> {
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                        "Usage: /" + label + " <list|spawn|info|reload>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(List.of("list", "spawn", "info", "reload"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("info"))) {
            return CommandUtil.filterStartingWith(mobManager.getMobIds(), args[1]);
        }
        return List.of();
    }
}
