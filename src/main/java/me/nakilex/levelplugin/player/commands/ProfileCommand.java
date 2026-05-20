package me.nakilex.levelplugin.player.commands;

import me.nakilex.levelplugin.player.profile.PlayerProfile;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.player.profile.ProfileSelectionGUI;
import me.nakilex.levelplugin.environment.EnvironmentAreaInstanceManager;
import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Command exposing CRUD operations for player profiles.
 */
public class ProfileCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        ProfileManager pm = ProfileManager.getInstance();

        if (args.length == 0) {
            ProfileSelectionGUI.startSelection(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /profile create <name>");
                    return true;
                }
                int unlocked = pm.getUnlockedSlots(player.getUniqueId());
                List<PlayerProfile> list = pm.getProfiles(player.getUniqueId());
                int slot = -1;
                for (int i = 0; i < unlocked; i++) {
                    if (list.get(i) == null) { slot = i; break; }
                }
                if (slot == -1) {
                    player.sendMessage(ChatColor.RED + "No empty profile slot available.");
                    return true;
                }
                pm.createProfile(player.getUniqueId(), slot, args[1]);
                ProfileSelectionGUI.markNewProfile(player, slot);
                player.sendMessage(ChatColor.GREEN + "Profile created in slot " + (slot + 1) + ".");
                return true;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /profile delete <slot>");
                    return true;
                }
                Integer del = parseSlot(args[1]);
                if (del == null) {
                    player.sendMessage(ChatColor.RED + "Slot must be 1-4.");
                    return true;
                }
                Integer active = pm.getActiveSlot(player.getUniqueId());
                PlayerProfile beforeDelete = pm.getProfile(player.getUniqueId(), del);
                pm.deleteProfile(player, del);
                if (beforeDelete != null && pm.getProfile(player.getUniqueId(), del) != null) {
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "Profile deleted.");
                if (active != null && active.equals(del)) {
                    pm.clearActiveSlot(player.getUniqueId());
                    org.bukkit.World lobbyWorld = org.bukkit.Bukkit.getWorld("world");
                    if (lobbyWorld != null) {
                        player.teleport(new org.bukkit.Location(lobbyWorld, 217, 6, 80));
                    }
                    ProfileSelectionGUI.startSelection(player);
                }
                return true;

            case "select":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /profile select <slot>");
                    return true;
                }
                Integer sel = parseSlot(args[1]);
                if (sel == null) {
                    player.sendMessage(ChatColor.RED + "Slot must be 1-4.");
                    return true;
                }
                ProfileSelectionGUI.selectProfile(player, sel);
                return true;

            case "update":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /profile update <slot> <name>");
                    return true;
                }
                Integer upd = parseSlot(args[1]);
                if (upd == null) {
                    player.sendMessage(ChatColor.RED + "Slot must be 1-4.");
                    return true;
                }
                if (pm.renameProfile(player.getUniqueId(), upd, args[2])) {
                    player.sendMessage(ChatColor.GREEN + "Profile renamed.");
                } else {
                    player.sendMessage(ChatColor.RED + "No profile in that slot.");
                }
                return true;

            case "unlock":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /profile unlock <slots>");
                    return true;
                }
                try {
                    int count = Integer.parseInt(args[1]);
                    pm.setUnlockedSlots(player.getUniqueId(), count);
                    player.sendMessage(ChatColor.GREEN + "Unlocked " + count + " profile slots.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Slots must be a number.");
                }
                return true;

            case "lock":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /profile lock <slots>");
                    return true;
                }
                try {
                    int count = Integer.parseInt(args[1]);
                    pm.setUnlockedSlots(player.getUniqueId(), count);
                    player.sendMessage(ChatColor.GREEN + "Unlocked slots set to " + count + ".");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Slots must be a number.");
                }
                return true;
            default:
                player.sendMessage(ChatColor.RED + "Usage: /profile <create|delete|select|update|unlock|lock>");
                return true;
        }
    }

    private Integer parseSlot(String arg) {
        try {
            int slot = Integer.parseInt(arg) - 1;
            if (slot < 0 || slot > 3) return null;
            return slot;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of("create", "delete", "select", "update", "unlock", "lock");
            String start = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(start)) result.add(s);
            }
            return result;
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "delete":
                case "select":
                case "update":
                    return slotCompletions(args[1]);
                case "unlock":
                case "lock":
                    return List.of("1", "2", "3", "4");
                default:
                    break;
            }
        }
        if (args.length == 3 && "update".equalsIgnoreCase(args[0])) {
            return Collections.singletonList("name");
        }
        return Collections.emptyList();
    }

    private List<String> slotCompletions(String startArg) {
        List<String> slots = List.of("1", "2", "3", "4");
        String start = startArg.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String s : slots) {
            if (s.startsWith(start)) result.add(s);
        }
        return result;
    }
}
