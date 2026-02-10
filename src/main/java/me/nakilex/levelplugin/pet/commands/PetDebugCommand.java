package me.nakilex.levelplugin.pet.commands;

import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.PetProgression;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import me.nakilex.levelplugin.pet.utils.PetDisplayUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class PetDebugCommand implements CommandExecutor, TabCompleter {
    private final PetManager petManager;

    public PetDebugCommand(PetManager petManager) {
        this.petManager = petManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";

        switch (sub) {
            case "list" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) {
                    return true;
                }
                PetChatUtil.send(target, "Available pets: " + String.join(", ", petManager.getPetIds()));
                return true;
            }
            case "summon" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " summon <petId> [player]");
                    return true;
                }
                Player target = resolveTarget(sender, args, 2);
                if (target == null) {
                    return true;
                }
                String petId = args[1];
                petManager.summonPet(target, petId);
                return true;
            }
            case "dismiss" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) {
                    return true;
                }
                petManager.dismissPet(target);
                PetChatUtil.send(target, "Dismissed active pet.");
                return true;
            }
            case "setlevel" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " setlevel <petId> <level> [player]");
                    return true;
                }
                Player target = resolveTarget(sender, args, 3);
                if (target == null) {
                    return true;
                }
                String petId = args[1];
                int level = parseInt(args[2], 1);
                petManager.setPetLevel(target, petId, level);
                PetChatUtil.send(target, "Set " + petId + " to level " + level + ".");
                return true;
            }
            case "addxp" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " addxp <petId> <amount> [player]");
                    return true;
                }
                Player target = resolveTarget(sender, args, 3);
                if (target == null) {
                    return true;
                }
                String petId = args[1];
                int amount = parseInt(args[2], 0);
                petManager.addPetXp(target, petId, amount);
                PetChatUtil.send(target, "Added " + amount + " XP to " + petId + ".");
                return true;
            }
            case "addcopy" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " addcopy <petId> <amount> [player]");
                    return true;
                }
                Player target = resolveTarget(sender, args, 3);
                if (target == null) {
                    return true;
                }
                String petId = args[1];
                int amount = parseInt(args[2], 0);
                petManager.addPetCopies(target, petId, amount);
                PetChatUtil.send(target, "Added " + amount + " copies to " + petId + ".");
                return true;
            }
            case "info" -> {
                Player target = resolveTarget(sender, args, 2);
                if (target == null) {
                    return true;
                }
                Optional<PetDefinition> defOpt = args.length > 1
                        ? petManager.getDefinition(args[1])
                        : Optional.empty();
                PetDefinition def = defOpt.orElseGet(() -> petManager.getActivePet(target.getUniqueId())
                        .map(me.nakilex.levelplugin.pet.PetInstance::definition)
                        .orElse(null));
                if (def == null) {
                    PetChatUtil.send(target, "No pet to inspect.");
                    return true;
                }
                String displayName = PetDisplayUtil.formatDisplayName(def);
                int xp = petManager.getProfile(target.getUniqueId()).getPetXp(def.id());
                int tier = petManager.getProfile(target.getUniqueId()).getPetTier(def.id());
                int level = PetProgression.levelFromXp(xp, def.xpPerLevel(), def.maxLevel());
                List<PetEffectDefinition> effects = def.effectsForLevel(level, tier);
                PetChatUtil.send(target, ChatColor.WHITE + displayName + ChatColor.GRAY
                        + " (" + def.rarity().getSymbol() + "<glyph:pet>" + ChatColor.GRAY + ")");
                PetChatUtil.send(target, "Level " + level + " (" + xp + " XP)");
                PetChatUtil.send(target, "Tier " + tier
                        + " | Copies " + petManager.getProfile(target.getUniqueId()).getPetCopies(def.id()));
                if (!effects.isEmpty()) {
                    for (PetEffectDefinition effect : effects) {
                        PetChatUtil.send(target, "Effect: " + effect.type().displayName()
                                + " (" + effect.type().formatDescription(effect.baseValue()) + ")");
                    }
                }
                return true;
            }
            case "reload" -> {
                petManager.reload();
                if (sender instanceof Player player) {
                    PetChatUtil.send(player, "Reloaded pet definitions.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Reloaded pet definitions.");
                }
                return true;
            }
            default -> {
                if (sender instanceof Player player) {
                    PetChatUtil.send(player, "Usage: /" + label + " <list|summon|dismiss|setlevel|addxp|addcopy|info|reload>");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <list|summon|dismiss|setlevel|addxp|addcopy|info|reload>");
                }
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(List.of("list", "summon", "dismiss", "setlevel", "addxp", "addcopy", "info", "reload"), args[0]);
        }
        if (args.length == 2 && List.of("summon", "setlevel", "addxp", "addcopy", "info").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterByPrefix(petManager.getPetIds(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("summon")) {
            return filterByPrefix(getOnlineNames(), args[2]);
        }
        if (args.length == 4 && List.of("setlevel", "addxp", "addcopy").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterByPrefix(getOnlineNames(), args[3]);
        }
        return Collections.emptyList();
    }

    private Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (!(sender instanceof Player) && args.length <= index) {
            sender.sendMessage(ChatColor.RED + "Usage: /petdebug <action> <player>");
            return null;
        }
        if (args.length > index) {
            Player target = Bukkit.getPlayerExact(args[index]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[index]);
            }
            return target;
        }
        return sender instanceof Player player ? player : null;
    }

    private List<String> getOnlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> filterByPrefix(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return options;
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                filtered.add(option);
            }
        }
        return filtered;
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
