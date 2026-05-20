package me.nakilex.levelplugin.mail;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MailAdminCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length < 6) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Usage: /mailadmin send <player|all> <coins> <gems> <xp> <subject>");
            return true;
        }
        if (!args[0].equalsIgnoreCase("send")) return true;
        String target = args[1];
        int coins = parse(args[2]), gems = parse(args[3]), xp = parse(args[4]);
        String subject = args[5];
        List<org.bukkit.inventory.ItemStack> items = player.getInventory().getItemInMainHand().getType().isAir()
                ? List.of() : List.of(player.getInventory().getItemInMainHand().clone());
        if ("all".equalsIgnoreCase(target)) {
            MailManager.getInstance().sendToAllOnline(player.getUniqueId(), subject, "Administrative mail.", coins, gems, xp, items);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Sent mail to all online players.");
            return true;
        }
        Player t = Bukkit.getPlayerExact(target);
        if (t == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Target must be online for now.");
            return true;
        }
        MailManager.getInstance().sendToPlayer(t.getUniqueId(), player.getUniqueId(), subject, "Administrative mail.", coins, gems, xp, items);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Sent mail to " + t.getName() + ".");
        return true;
    }

    private int parse(String n) { try { return Math.max(0, Integer.parseInt(n)); } catch (Exception ignored) { return 0; } }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("send");
        if (args.length == 2) {
            List<String> out = new ArrayList<>();
            out.add("all");
            String start = args[1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(start)) out.add(p.getName());
            return out;
        }
        return List.of();
    }
}
