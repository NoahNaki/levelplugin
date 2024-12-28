package me.nakilex.levelplugin.party;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyCommands implements CommandExecutor {

    private final PartyManager partyManager;
    private final Map<UUID, Long> inviteCooldowns = new HashMap<>();
    private static final Map<UUID, UUID> pendingInvites = new HashMap<>(); // Maps invitee -> inviter



    public PartyCommands(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /party <subcommand>");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "create":
                if (partyManager.createParty(playerId)) {
                    player.sendMessage(ChatColor.GREEN + "Party created successfully.");
                } else {
                    player.sendMessage(ChatColor.RED + "You are already in a party.");
                }
                break;

            case "invite":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /party invite <player>");
                    return true;
                }
                Player invitee = Bukkit.getPlayer(args[1]);
                if (invitee == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                UUID inviteeId = invitee.getUniqueId();
                Party inviterParty = partyManager.getParty(playerId); // Renamed to inviterParty

                if (inviterParty == null || !inviterParty.isLeader(playerId)) {
                    player.sendMessage(ChatColor.RED + "You must be a party leader to invite players.");
                    return true;
                }
                if (partyManager.isInParty(inviteeId)) {
                    player.sendMessage(ChatColor.RED + "Player is already in a party.");
                    return true;
                }

                // Cooldown check
                long currentTime = System.currentTimeMillis();
                if (inviteCooldowns.containsKey(inviteeId)) {
                    long lastInvite = inviteCooldowns.get(inviteeId);
                    if (currentTime - lastInvite < 10000) { // 10 seconds cooldown
                        player.sendMessage(ChatColor.RED + "You must wait before inviting this player again.");
                        return true;
                    }
                }
                // Update cooldown
                inviteCooldowns.put(inviteeId, currentTime);

                PartyUtils.sendInvite(playerId, inviteeId);
                player.sendMessage(ChatColor.GREEN + "Invitation sent to " + invitee.getName() + ".");
                break;


            case "kick":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /party kick <player>");
                    return true;
                }
                Player kickTarget = Bukkit.getPlayer(args[1]);
                if (kickTarget == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                UUID kickId = kickTarget.getUniqueId();
                if (partyManager.removeMember(playerId, kickId)) {
                    kickTarget.sendMessage(ChatColor.RED + "You have been kicked from the party.");
                    player.sendMessage(ChatColor.GREEN + "Player kicked successfully.");
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to kick player.");
                }
                break;

            case "leave":
                if (partyManager.removeMember(playerId, playerId)) {
                    player.sendMessage(ChatColor.GREEN + "You left the party.");
                } else {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                }
                break;

            case "list":
                Party playerParty = partyManager.getParty(playerId); // Renamed to playerParty
                if (playerParty != null) {
                    player.sendMessage(ChatColor.GREEN + "Party Members:");
                    for (UUID memberId : playerParty.getMembers()) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null && member.isOnline()) {
                            player.sendMessage(ChatColor.YELLOW + "- " + member.getName());
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                }
                break;

            case "promote":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /party promote <player>");
                    return true;
                }
                Player promoteTarget = Bukkit.getPlayer(args[1]);
                if (promoteTarget == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                UUID promoteId = promoteTarget.getUniqueId();
                if (partyManager.promoteLeader(playerId, promoteId)) {
                    player.sendMessage(ChatColor.GREEN + "Player promoted to party leader.");
                    promoteTarget.sendMessage(ChatColor.GREEN + "You are now the party leader.");
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to promote player.");
                }
                break;

            case "accept":
                if (PartyUtils.acceptInvite(playerId, partyManager)) {
                    player.sendMessage(ChatColor.GREEN + "You have joined the party.");
                } else {
                    player.sendMessage(ChatColor.RED + "No pending invites.");
                }
                break;

            case "deny":
                // Retrieve inviter first before any removal occurs
                UUID inviterId = PartyUtils.getInviter(playerId);

                // Proceed to deny the invite
                if (PartyUtils.denyInvite(playerId)) { // This may remove the invite internally
                    player.sendMessage(ChatColor.YELLOW + "You have declined the invite.");

                    // Send feedback to inviter if available
                    if (inviterId != null) {
                        Player inviter = Bukkit.getPlayer(inviterId);
                        if (inviter != null) {
                            inviter.sendMessage(ChatColor.RED + player.getName() + " has declined your invitation.");
                        }
                    }

                    // Explicitly remove invite after feedback
                    pendingInvites.remove(playerId); // Ensure it's cleaned up afterward
                } else {
                    player.sendMessage(ChatColor.RED + "No pending invites.");
                }
                break;


            case "chat":
                Party chatParty = partyManager.getParty(playerId);
                if (chatParty != null) {
                    chatParty.toggleChat();
                    player.sendMessage(ChatColor.GREEN + "Party chat is now " + (chatParty.isChatEnabled() ? "enabled" : "disabled") + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand.");
                break;
        }
        return true;
    }
}
