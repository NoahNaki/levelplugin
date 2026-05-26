package me.nakilex.levelplugin.advancement.commands;

import me.nakilex.levelplugin.advancement.AdvancementService;
import me.nakilex.levelplugin.advancement.AdvancementToastUtil;
import me.nakilex.levelplugin.advancement.model.Advancement;
import me.nakilex.levelplugin.advancement.model.AdvancementKey;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class AdvancementAdminCommand implements CommandExecutor, TabCompleter {
    private final AdvancementService service;
    public AdvancementAdminCommand(AdvancementService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.GOLD + "Advancement tabs:");
            service.tabs().forEach(tab -> sender.sendMessage(ChatColor.GRAY + "- " + tab.namespace() + ChatColor.DARK_GRAY + " (" + tab.advancements().size() + ")"));
            return true;
        }
        if (args.length < 3) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
        AdvancementKey key;
        try { key = AdvancementKey.parse(args[2]); } catch (Exception e) { sender.sendMessage(ChatColor.RED + "Invalid key format. Use namespace:key"); return true; }
        Optional<Advancement> advancement = service.find(key);
        if (advancement.isEmpty()) { sender.sendMessage(ChatColor.RED + "Unknown advancement key."); return true; }
        UUID teamId = target.getUniqueId();
        if (args[0].equalsIgnoreCase("grant")) {
            service.setProgression(teamId, key, advancement.get().maxProgress());
            sender.sendMessage(ChatColor.GREEN + "Granted " + key + " to " + target.getName());
            target.sendMessage(ChatUtil.applyEmojis("&aAdvancement granted: &f" + key));
            AdvancementToastUtil.showToast(target, advancement.get());
            return true;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            service.setProgression(teamId, key, 0);
            sender.sendMessage(ChatColor.YELLOW + "Reset " + key + " for " + target.getName());
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("list", "grant", "reset");
        if (args.length == 2) return null;
        if (args.length == 3) {
            List<String> keys = new ArrayList<>();
            service.tabs().forEach(t -> t.advancements().forEach(a -> keys.add(a.key().toString())));
            return keys;
        }
        return List.of();
    }
}
