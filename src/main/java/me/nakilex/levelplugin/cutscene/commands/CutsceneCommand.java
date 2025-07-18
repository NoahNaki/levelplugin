package me.nakilex.levelplugin.cutscene.commands;

import me.nakilex.levelplugin.cutscene.CutsceneManager;
import org.bukkit.Bukkit;
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
            sender.sendMessage(ChatColor.GREEN + "/cutscene play <id> [player] | list | reload | record <id> | addframe [duration] | stop | cancel | skip [player]");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "play":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cutscene play <id> [player]");
                    return true;
                }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(ChatColor.RED + "Specify a player.");
                    return true;
                }
                manager.playCutscene(target, args[1]);
                sender.sendMessage(ChatColor.YELLOW + "Playing cutscene " + args[1] + " for " + target.getName());
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
            case "skip":
                if (args.length >= 2) {
                    Player p = Bukkit.getPlayer(args[1]);
                    if (p == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                    manager.skipCutscene(p);
                    sender.sendMessage(ChatColor.GREEN + "Skipped cutscene for " + p.getName());
                    return true;
                } else if (sender instanceof Player self) {
                    manager.skipCutscene(self);
                    sender.sendMessage(ChatColor.GREEN + "Cutscene skipped.");
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Specify a player.");
                    return true;
                }
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
