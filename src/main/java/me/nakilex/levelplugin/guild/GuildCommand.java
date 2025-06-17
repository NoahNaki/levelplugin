package me.nakilex.levelplugin.guild;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GuildCommand implements CommandExecutor {

    private final GuildManager manager;
    private final GuildGUI gui;

    public GuildCommand(GuildManager manager, GuildGUI gui) {
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        Player player = (Player) sender;
        UUID id = player.getUniqueId();

        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /guild create <name>");
                    return true;
                }
                String name = args[1];
                if (manager.createGuild(name, id) != null) {
                    player.sendMessage(ChatColor.GREEN + "Guild " + name + " created!");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not create guild.");
                }
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /guild invite <player>");
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Player not found");
                    return true;
                }
                if (me.nakilex.levelplugin.Main.getInstance().getIgnoreManager().isIgnoring(targetPlayer.getUniqueId(), id)
                        || me.nakilex.levelplugin.Main.getInstance().getIgnoreManager().isIgnoring(id, targetPlayer.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Cannot invite that player.");
                    return true;
                }
                if (manager.invite(id, targetPlayer.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "Invite sent to " + targetPlayer.getName());
                    targetPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " invited you to join their guild. Type /guild accept to join.");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not invite player");
                }
                break;
            case "accept":
                if (manager.accept(id)) {
                    player.sendMessage(ChatColor.GREEN + "You joined the guild!");
                } else {
                    player.sendMessage(ChatColor.RED + "No pending invite.");
                }
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /guild kick <player>");
                    return true;
                }
                Player kick = Bukkit.getPlayer(args[1]);
                if (kick == null) {
                    player.sendMessage(ChatColor.RED + "Player not found");
                    return true;
                }
                if (kick.getUniqueId().equals(id)) {
                    player.sendMessage(ChatColor.RED + "You cannot kick yourself.");
                    return true;
                }
                if (manager.removeMember(id, kick.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "Kicked " + kick.getName());
                    kick.sendMessage(ChatColor.RED + "You were kicked from the guild");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not kick player");
                }
                break;
            case "promote":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /guild promote <player>");
                    return true;
                }
                Player promote = Bukkit.getPlayer(args[1]);
                if (promote == null) {
                    player.sendMessage(ChatColor.RED + "Player not found");
                    return true;
                }
                if (manager.promote(id, promote.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + promote.getName() + " is now the leader.");
                    promote.sendMessage(ChatColor.GREEN + "You are now the guild leader!");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not promote player");
                }
                break;
            case "leave":
                Guild g = manager.getGuild(id);
                if (g == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a guild");
                    return true;
                }
                if (g.getLeader().equals(id)) {
                    manager.disbandGuild(g.getName());
                    player.sendMessage(ChatColor.YELLOW + "Guild disbanded.");
                } else {
                    manager.removeMember(g.getLeader(), id);
                    player.sendMessage(ChatColor.YELLOW + "You left the guild.");
                }
                break;
            case "alliance":
                if (args.length < 2) {
                    player.sendMessage("/guild alliance <guild>");
                    return true;
                }
                Guild my = manager.getGuild(id);
                if (my == null || !my.getLeader().equals(id)) {
                    player.sendMessage(ChatColor.RED + "Only guild leaders can set alliances.");
                    return true;
                }
                if (manager.requestAlliance(my.getName(), args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Alliance request sent.");
                    Guild targetGuild = manager.getGuild(args[1]);
                    if (targetGuild != null) {
                        Player leader = Bukkit.getPlayer(targetGuild.getLeader());
                        if (leader != null) {
                            leader.sendMessage(ChatColor.YELLOW + my.getName() + " has requested an alliance with your guild. Use /guild allyaccept " + my.getName() + " to accept.");
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Cannot ally with that guild.");
                }
                break;
            case "hostile":
                if (args.length < 2) {
                    player.sendMessage("/guild hostile <guild>");
                    return true;
                }
                Guild myh = manager.getGuild(id);
                if (myh == null || !myh.getLeader().equals(id)) {
                    player.sendMessage(ChatColor.RED + "Only guild leaders can declare hostility.");
                    return true;
                }
                if (manager.setHostile(myh.getName(), args[1])) {
                    player.sendMessage(ChatColor.RED + "Declared hostility.");
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to declare hostility.");
                }
                break;
            case "neutral":
                if (args.length < 2) {
                    player.sendMessage("/guild neutral <guild>");
                    return true;
                }
                Guild myn = manager.getGuild(id);
                if (myn == null || !myn.getLeader().equals(id)) {
                    player.sendMessage(ChatColor.RED + "Only guild leaders can change relations.");
                    return true;
                }
                if (myn.getAllies().contains(args[1])) {
                    if (manager.revokeAlliance(myn.getName(), args[1])) {
                        player.sendMessage(ChatColor.YELLOW + "Alliance revoked; now neutral with " + args[1] + ".");
                    } else {
                        player.sendMessage(ChatColor.RED + "Could not change relation.");
                    }
                } else if (manager.requestNeutral(myn.getName(), args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Neutrality request sent.");
                    Guild neutralTarget = manager.getGuild(args[1]);
                    if (neutralTarget != null) {
                        Player leaderN = Bukkit.getPlayer(neutralTarget.getLeader());
                        if (leaderN != null) {
                            leaderN.sendMessage(ChatColor.YELLOW + myn.getName() + " has requested neutrality. Use /guild neutralaccept " + myn.getName() + " to accept.");
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to set neutral.");
                }
                break;
            case "allyaccept":
                if (args.length < 2) {
                    player.sendMessage("/guild allyaccept <guild>");
                    return true;
                }
                Guild recA = manager.getGuild(id);
                if (recA == null || !recA.getLeader().equals(id)) {
                    player.sendMessage(ChatColor.RED + "Only guild leaders can accept alliances.");
                    return true;
                }
                if (manager.acceptAlliance(recA.getName(), args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Alliance formed with " + args[1] + ".");
                    Guild requestGuild = manager.getGuild(args[1]);
                    if (requestGuild != null) {
                        Player reqLeader = Bukkit.getPlayer(requestGuild.getLeader());
                        if (reqLeader != null) {
                            reqLeader.sendMessage(ChatColor.GREEN + recA.getName() + " accepted your alliance request.");
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "No alliance request from that guild.");
                }
                break;
            case "allydeny":
                if (args.length < 2) {
                    player.sendMessage("/guild allydeny <guild>");
                    return true;
                }
                Guild recAD = manager.getGuild(id);
                if (recAD == null || !recAD.getLeader().equals(id)) {
                    player.sendMessage(ChatColor.RED + "Only guild leaders can deny alliances.");
                    return true;
                }
                if (manager.denyAlliance(recAD.getName(), args[1])) {
                    player.sendMessage(ChatColor.YELLOW + "Alliance request denied.");
                } else {
                    player.sendMessage(ChatColor.RED + "No alliance request from that guild.");
                }
                break;
            case "allyrevoke":
                if (args.length < 2) {
                    player.sendMessage("/guild allyrevoke <guild>");
                    return true;
                }
                Guild myRev = manager.getGuild(id);
                if (myRev == null || !myRev.getLeader().equals(id)) {
                    player.sendMessage(ChatColor.RED + "Only guild leaders can revoke alliances.");
                    return true;
                }
                if (manager.revokeAlliance(myRev.getName(), args[1])) {
                    player.sendMessage(ChatColor.YELLOW + "Alliance with " + args[1] + " revoked.");
                } else {
                    player.sendMessage(ChatColor.RED + "You are not allied with that guild.");
                }
                break;
            case "neutralaccept":
                if (args.length < 2) {
                    player.sendMessage("/guild neutralaccept <guild>");
                    return true;
                }
                Guild recN = manager.getGuild(id);
                if (recN == null || !recN.getLeader().equals(id)) {
                    player.sendMessage(ChatColor.RED + "Only guild leaders can accept neutrality.");
                    return true;
                }
                if (manager.acceptNeutral(recN.getName(), args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Relations set to neutral with " + args[1] + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "No neutrality request from that guild.");
                }
                break;
            case "neutraldeny":
                if (args.length < 2) {
                    player.sendMessage("/guild neutraldeny <guild>");
                    return true;
                }
                Guild recND = manager.getGuild(id);
                if (recND == null || !recND.getLeader().equals(id)) {
                    player.sendMessage(ChatColor.RED + "Only guild leaders can deny neutrality.");
                    return true;
                }
                if (manager.denyNeutral(recND.getName(), args[1])) {
                    player.sendMessage(ChatColor.YELLOW + "Neutrality request denied.");
                } else {
                    player.sendMessage(ChatColor.RED + "No neutrality request from that guild.");
                }
                break;
            case "list":
                gui.open(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand");
                break;
        }
        return true;
    }
}
