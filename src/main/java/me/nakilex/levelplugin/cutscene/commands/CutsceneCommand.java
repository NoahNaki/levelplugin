package me.nakilex.levelplugin.cutscene.commands;

import me.nakilex.levelplugin.cutscene.CutsceneManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CutsceneCommand implements CommandExecutor {
    private final CutsceneManager manager;

    public CutsceneCommand(CutsceneManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "/cutscene play <id> | list | reload | record <id> | addframe [duration] | stop | cancel");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "play":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can run this.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cutscene play <id>");
                    return true;
                }
                manager.playCutscene(player, args[1]);
                sender.sendMessage(ChatColor.YELLOW + "Playing cutscene " + args[1]);
                return true;
            case "record":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can record.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cutscene record <id>");
                    return true;
                }
                manager.startRecording(player, args[1]);
                sender.sendMessage(ChatColor.GREEN + "Recording cutscene " + args[1]);
                return true;
            case "addframe":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can add frames.");
                    return true;
                }
                if (!manager.isRecording(player)) {
                    sender.sendMessage(ChatColor.RED + "You are not recording a cutscene.");
                    return true;
                }
                long dur = 2000L;
                if (args.length >= 2) {
                    try {
                        dur = Long.parseLong(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                manager.addFrame(player, dur);
                sender.sendMessage(ChatColor.YELLOW + "Added frame with duration " + dur + "ms");
                return true;
            case "stop":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can stop recording.");
                    return true;
                }
                if (!manager.isRecording(player)) {
                    sender.sendMessage(ChatColor.RED + "You are not recording a cutscene.");
                    return true;
                }
                manager.finishRecording(player);
                sender.sendMessage(ChatColor.GREEN + "Cutscene saved.");
                return true;
            case "cancel":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can cancel recording.");
                    return true;
                }
                if (!manager.isRecording(player)) {
                    sender.sendMessage(ChatColor.RED + "You are not recording a cutscene.");
                    return true;
                }
                manager.cancelRecording(player);
                sender.sendMessage(ChatColor.YELLOW + "Recording cancelled.");
                return true;
            case "reload":
                manager.loadCutscenes();
                sender.sendMessage(ChatColor.GREEN + "Cutscenes reloaded.");
                return true;
            case "list":
                sender.sendMessage(ChatColor.GOLD + "Cutscenes: " + String.join(", ", manager.listCutscenes()));
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                return true;
        }
    }
}
